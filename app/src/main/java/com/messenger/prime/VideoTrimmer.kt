package com.messenger.prime

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmer {
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
            val firstSampleTimeUs = extractor.sampleTime.coerceAtLeast(0L)

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
