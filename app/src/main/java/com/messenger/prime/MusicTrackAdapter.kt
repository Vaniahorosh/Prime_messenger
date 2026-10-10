package com.messenger.prime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.messenger.prime.databinding.ItemMusicTrackBinding

class MusicTrackAdapter(
    private var tracks: List<TrackItem>,
    private val onTrackClick: (TrackItem) -> Unit,
    private val onActionClick: ((TrackItem) -> Unit)? = null
) : RecyclerView.Adapter<MusicTrackAdapter.TrackViewHolder>() {

    private var activeTrackId: String? = null
    private var isPlaying: Boolean = false

    fun setActiveTrack(trackId: String?, playing: Boolean) {
        activeTrackId = trackId
        isPlaying = playing
        notifyDataSetChanged()
    }

    fun updateTracks(newTracks: List<TrackItem>) {
        tracks = newTracks
        notifyDataSetChanged()
    }

    class TrackViewHolder(val binding: ItemMusicTrackBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemMusicTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = tracks[position]
        val binding = holder.binding

        binding.tvTrackTitle.text = track.title
        binding.tvTrackArtist.text = track.artist
        binding.tvTrackDuration.text = track.durationStr

        val isActive = track.id == activeTrackId || track.path == activeTrackId
        val context = holder.itemView.context

        if (isActive) {
            binding.ivTrackPlayBadge.visibility = View.VISIBLE
            binding.ivTrackPlayBadge.setImageResource(if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play)
            binding.tvTrackTitle.setTextColor(ColorAccentManager.getCurrentAccentColor(context))
        } else {
            binding.ivTrackPlayBadge.visibility = View.GONE
            binding.tvTrackTitle.setTextColor(ContextCompat.getColor(context, R.color.prime_text_primary))
        }

        if (!track.coverPath.isNullOrEmpty()) {
            Glide.with(context)
                .load(track.coverPath)
                .placeholder(R.drawable.ic_music)
                .error(R.drawable.ic_music)
                .into(binding.ivTrackCover)
        } else {
            binding.ivTrackCover.setImageResource(R.drawable.ic_music)
        }

        if (onActionClick != null) {
            binding.ivTrackAction.visibility = View.VISIBLE
            binding.ivTrackAction.setImageResource(if (track.isSaved) R.drawable.ic_done else R.drawable.ic_download)
            binding.ivTrackAction.setOnClickListener { onActionClick.invoke(track) }
        } else {
            binding.ivTrackAction.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onTrackClick(track)
        }
    }

    override fun getItemCount(): Int = tracks.size
}
