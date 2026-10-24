package com.messenger.prime

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmer {

    private fun getVideoRotation(file: File): Int {
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            rotationStr?.toInt() ?: 0
        } catch (e: Exception) {
            0
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }
    }

    fun concatenateVideos(inputFiles: List<File>, outputFile: File) {
        val validFiles = inputFiles.filter { it.exists() && it.length() > 500 }
        if (validFiles.isEmpty()) return
        if (validFiles.size == 1) {
            validFiles[0].copyTo(outputFile, overwrite = true)
            return
        }

        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val rotation = getVideoRotation(validFiles[0])
            if (rotation != 0) {
                muxer.setOrientationHint(rotation)
            }

            // 1. Add tracks to Muxer from the first valid file
            val firstExtractor = MediaExtractor()
            firstExtractor.setDataSource(validFiles[0].absolutePath)

            var muxerVideoTrack = -1
            var muxerAudioTrack = -1
            var bufferSize = 1024 * 1024

            for (i in 0 until firstExtractor.trackCount) {
                val format = firstExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    muxerVideoTrack = muxer.addTrack(format)
                } else if (mime.startsWith("audio/")) {
                    muxerAudioTrack = muxer.addTrack(format)
                }
                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val sz = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    if (sz > bufferSize) bufferSize = sz
                }
            }
            firstExtractor.release()

            val buffer = ByteBuffer.allocateDirect(bufferSize)
            muxer.start()

            var globalVideoOffsetUs = 0L
            var globalAudioOffsetUs = 0L

            for (fileIndex in validFiles.indices) {
                val file = validFiles[fileIndex]
                val extractor = MediaExtractor()
                extractor.setDataSource(file.absolutePath)

                val fileTrackToMuxerTrack = HashMap<Int, Int>()
                for (i in 0 until extractor.trackCount) {
                    val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("video/") && muxerVideoTrack != -1) {
                        extractor.selectTrack(i)
                        fileTrackToMuxerTrack[i] = muxerVideoTrack
                    } else if (mime.startsWith("audio/") && muxerAudioTrack != -1) {
                        extractor.selectTrack(i)
                        fileTrackToMuxerTrack[i] = muxerAudioTrack
                    }
                }

                var firstVideoPts = -1L
                var firstAudioPts = -1L
                var foundFirstVideoKeyFrame = (fileIndex == 0)

                var maxVideoPtsUs = 0L
                var maxAudioPtsUs = 0L
                val info = MediaCodec.BufferInfo()

                while (true) {
                    info.offset = 0
                    info.size = extractor.readSampleData(buffer, 0)
                    if (info.size < 0) break

                    val trackIndex = extractor.sampleTrackIndex
                    val targetMuxerTrack = fileTrackToMuxerTrack[trackIndex]
                    if (targetMuxerTrack != null) {
                        val samplePtsUs = extractor.sampleTime
                        val isVideoTrack = (targetMuxerTrack == muxerVideoTrack)
                        val isSync = (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0

                        if (isVideoTrack) {
                            if (!foundFirstVideoKeyFrame) {
                                if (!isSync) {
                                    extractor.advance()
                                    continue
                                }
                                foundFirstVideoKeyFrame = true
                            }
                            if (firstVideoPts < 0) firstVideoPts = samplePtsUs
                            val relativePts = (samplePtsUs - firstVideoPts).coerceAtLeast(0L)
                            info.presentationTimeUs = globalVideoOffsetUs + relativePts
                            if (relativePts > maxVideoPtsUs) maxVideoPtsUs = relativePts
                        } else {
                            if (firstAudioPts < 0) firstAudioPts = samplePtsUs
                            val relativePts = (samplePtsUs - firstAudioPts).coerceAtLeast(0L)
                            info.presentationTimeUs = globalAudioOffsetUs + relativePts
                            if (relativePts > maxAudioPtsUs) maxAudioPtsUs = relativePts
                        }

                        info.flags = if (isSync) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                        muxer.writeSampleData(targetMuxerTrack, buffer, info)
                    }
                    extractor.advance()
                }

                globalVideoOffsetUs += maxVideoPtsUs + 33_000L
                globalAudioOffsetUs += maxAudioPtsUs + 33_000L
                extractor.release()
            }
        } catch (e: Exception) {
            Log.e("VideoTrimmer", "Error concatenating videos", e)
            validFiles[0].copyTo(outputFile, overwrite = true)
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e("VideoTrimmer", "Error releasing muxer", e)
            }
        }
    }

    fun trimVideo(inputFile: File, outputFile: File, startMs: Long, endMs: Long) {
        if (startMs <= 0 && endMs <= 0) {
            inputFile.copyTo(outputFile, overwrite = true)
            return
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(inputFile.absolutePath)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val rotation = getVideoRotation(inputFile)
            if (rotation != 0) {
                muxer.setOrientationHint(rotation)
            }

            val indexMap = HashMap<Int, Int>()
            var bufferSize = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true || mime?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)
                    val newIndex = muxer.addTrack(format)
                    indexMap[i] = newIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (newSize > bufferSize) bufferSize = newSize
                    }
                }
            }

            if (bufferSize < 0) bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocateDirect(bufferSize)

            muxer.start()

            val startUs = startMs * 1000
            val endUs = if (endMs > 0) endMs * 1000 else Long.MAX_VALUE

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var firstSampleTimeUs = Long.MAX_VALUE
            for (i in 0 until trackCount) {
                extractor.selectTrack(i)
                val t = extractor.sampleTime
                if (t in 0..<firstSampleTimeUs) {
                    firstSampleTimeUs = t
                }
            }
            if (firstSampleTimeUs == Long.MAX_VALUE) firstSampleTimeUs = 0L

            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val info = MediaCodec.BufferInfo()
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (endUs > 0 && sampleTimeUs > endUs) {
                    break
                }

                if (sampleTimeUs >= startUs) {
                    info.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                    info.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    val trackIndex = extractor.sampleTrackIndex
                    val muxerTrackIndex = indexMap[trackIndex]
                    if (muxerTrackIndex != null) {
                        muxer.writeSampleData(muxerTrackIndex, buffer, info)
                    }
                }
                extractor.advance()
            }
        } catch (e: Exception) {
            Log.e("VideoTrimmer", "Error trimming video", e)
            inputFile.copyTo(outputFile, overwrite = true)
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
                extractor.release()
            } catch (e: Exception) {
                Log.e("VideoTrimmer", "Error releasing muxer", e)
            }
        }
    }
}
