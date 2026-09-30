package com.messenger.prime

import android.bluetooth.BluetoothAdapter
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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

        val displayName = if (BluetoothAdapter.checkBluetoothAddress(contact.name)) "Собеседник" else contact.name
        binding.tvName.text = displayName

        var loaded = false
        if (!contact.avatarUri.isNullOrEmpty()) {
            try {
                val uri = contact.avatarUri.toUri()
                val file = if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else null
                if (file != null && file.exists()) {
                    loadAvatarFileIntoView(context, file, binding.ivAvatar)
                    loaded = true
                } else {
                    loadAvatarUriIntoView(context, uri, binding.ivAvatar)
                    loaded = true
                }
            } catch (_: Exception) {}
        }

        if (!loaded) {
            val possible = listOfNotNull(
                File(context.filesDir, "rec_avatar_${contact.name}.gif"),
                File(context.filesDir, "rec_avatar_${contact.id}.gif"),
                File(context.filesDir, "rec_avatar_${contact.name}.jpg"),
                File(context.filesDir, "rec_avatar_${contact.id}.jpg"),
                File(context.filesDir, "avatar_${contact.name}.jpg"),
                File(context.filesDir, "avatar_${contact.id}.jpg")
            )
            val found = possible.firstOrNull { it.exists() && it.length() > 0 }
            if (found != null) {
                loadAvatarFileIntoView(context, found, binding.ivAvatar)
                loaded = true
            }
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

    fun updateContacts(newContacts: List<ChatModel>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
}
