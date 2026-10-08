package com.messenger.prime

import android.bluetooth.BluetoothAdapter
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ItemHorizontalContactBinding
import java.io.File

class HorizontalContactAdapter(
    private var contacts: List<ChatModel>,
    private val onContactClick: (ChatModel) -> Unit
) : RecyclerView.Adapter<HorizontalContactAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHorizontalContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHorizontalContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        val context = holder.itemView.context
        val binding = holder.binding

        val displayName = AvatarManager.getContactDisplayName(context, contact.id, contact.id, contact.name)
        binding.tvName.text = displayName

        val avatarSource = contact.avatarUri ?: AvatarManager.getContactAvatarUriOrFile(context, contact.id, displayName, contact.id)

        var loaded = false
        if (!avatarSource.isNullOrEmpty()) {
            try {
                loadAvatarIntoView(context, avatarSource, binding.ivAvatar, displayName)
                loaded = true
            } catch (_: Exception) {}
        }

        if (loaded) {
            binding.ivAvatar.visibility = View.VISIBLE
            binding.tvInitials.visibility = View.GONE
        } else {
            binding.ivAvatar.visibility = View.INVISIBLE
            binding.tvInitials.visibility = View.VISIBLE
            val initial = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "P"
            binding.tvInitials.text = initial
            val color = getAvatarColor(displayName)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 15 * context.resources.displayMetrics.density
                setColor(color)
            }
            binding.tvInitials.background = bg
        }

        if (contact.onlineStatus == OnlineStatus.ONLINE) {
            binding.viewOnlineDot.visibility = View.VISIBLE
        } else {
            binding.viewOnlineDot.visibility = View.GONE
        }

        binding.layoutHorizontalItem.setOnClickListener {
            onContactClick(contact)
        }
    }

    override fun getItemCount(): Int = contacts.size

    @Suppress("unused")
    fun updateContacts(newContacts: List<ChatModel>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = contacts.size
            override fun getNewListSize(): Int = newContacts.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return contacts[oldItemPosition].id == newContacts[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return contacts[oldItemPosition] == newContacts[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        contacts = newContacts
        diffResult.dispatchUpdatesTo(this)
    }
}
