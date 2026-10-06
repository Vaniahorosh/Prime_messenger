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

        val isBtAddress = try {
            BluetoothAdapter.checkBluetoothAddress(contact.name)
        } catch (_: Exception) {
            false
        }
        val displayName = if (isBtAddress) "Собеседник" else contact.name
        binding.tvName.text = displayName

        try {
            com.bumptech.glide.Glide.with(context).clear(binding.ivAvatar)
        } catch (_: Exception) {}
        binding.ivAvatar.setImageDrawable(null)

        var loaded = false
        if (!contact.avatarUri.isNullOrEmpty()) {
            try {
                val uri = contact.avatarUri.toUri()
                val file = if (uri.scheme == "file" && uri.path != null) File(uri.path!!) else null
                if (file != null) {
                    if (file.exists() && file.length() > 0) {
                        loadAvatarFileIntoView(context, file, binding.ivAvatar)
                        loaded = true
                    }
                } else {
                    loadAvatarUriIntoView(context, uri, binding.ivAvatar)
                    loaded = true
                }
            } catch (_: Exception) {}
        }

        if (!loaded) {
            val sharedPrefs = context.getSharedPreferences("PrimeLocalDB", android.content.Context.MODE_PRIVATE)
            val currentUser = sharedPrefs.getString("current_user", "") ?: ""
            val myName = sharedPrefs.getString("${currentUser}_name", currentUser) ?: ""
            val isGeneric = displayName.equals("Собеседник", ignoreCase = true) ||
                            displayName.equals("Prime Собеседник", ignoreCase = true) ||
                            displayName.equals("Пользователь", ignoreCase = true) ||
                            displayName.equals("Prime User", ignoreCase = true) ||
                            displayName.equals("Контакт", ignoreCase = true)
            val isLocalName = contact.name.equals(currentUser, ignoreCase = true) || contact.name.equals(myName, ignoreCase = true)
            val isLocalId = contact.id.equals(currentUser, ignoreCase = true) || contact.id.equals(myName, ignoreCase = true)

            val possible = mutableListOf<File>()
            if (!contact.id.isNullOrEmpty() && !isLocalId && !isGeneric) {
                possible.add(File(context.filesDir, "rec_avatar_${contact.id}.gif"))
                possible.add(File(context.filesDir, "rec_avatar_${contact.id}.jpg"))
            }
            if (!contact.name.isNullOrEmpty() && !isLocalName && !isGeneric) {
                possible.add(File(context.filesDir, "rec_avatar_${contact.name}.gif"))
                possible.add(File(context.filesDir, "rec_avatar_${contact.name}.jpg"))
            }

            val found = possible.firstOrNull { it.exists() && it.length() > 0 }
            if (found != null) {
                try {
                    loadAvatarFileIntoView(context, found, binding.ivAvatar)
                    loaded = true
                } catch (_: Exception) {}
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
