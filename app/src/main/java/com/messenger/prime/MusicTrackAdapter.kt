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
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MusicTrackAdapter.TrackViewHolder>() {

    private var activeTrackId: String? = null
    private var isPlaying: Boolean = false
    private val selectedTracks = mutableSetOf<String>()

    fun getSelectedItems(): List<TrackItem> = tracks.filter { selectedTracks.contains(it.path) }

    fun clearSelection() {
        selectedTracks.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

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

        val isSelected = selectedTracks.contains(track.path)
        
        binding.ivTrackAction.visibility = View.VISIBLE
        if (isSelected) {
            binding.ivTrackAction.setImageResource(R.drawable.ic_done)
            binding.ivTrackAction.setColorFilter(ContextCompat.getColor(context, R.color.prime_brand))
            holder.itemView.setBackgroundColor(0x20154B87)
        } else {
            binding.ivTrackAction.setImageResource(R.drawable.ic_add)
            binding.ivTrackAction.setColorFilter(ContextCompat.getColor(context, R.color.prime_text_secondary))
            holder.itemView.setBackgroundResource(R.drawable.bg_chat_item_light)
        }

        val toggleSelection = {
            if (isSelected) {
                selectedTracks.remove(track.path)
            } else {
                selectedTracks.add(track.path)
            }
            notifyItemChanged(position)
            onSelectionChanged(selectedTracks.size)
        }

        binding.flCoverContainer.setOnClickListener {
            if (isActive && isPlaying) {
                PrimeMusicManager.togglePlayPause(context)
            } else {
                onTrackClick(track)
            }
        }

        binding.ivTrackAction.setOnClickListener {
            toggleSelection()
        }

        holder.itemView.setOnClickListener {
            toggleSelection()
        }
    }

    override fun getItemCount(): Int = tracks.size
}
