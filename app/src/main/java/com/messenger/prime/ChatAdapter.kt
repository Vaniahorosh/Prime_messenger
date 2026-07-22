package com.messenger.prime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ItemChatBinding
import com.messenger.prime.databinding.ItemChatFooterBinding

class ChatAdapter(private var chatList: List<ChatModel>, private val onStartChatClick: () -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CHAT = 0
        private const val TYPE_FOOTER = 1
    }

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)
    class FooterViewHolder(val binding: ItemChatFooterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (position == chatList.size) TYPE_FOOTER else TYPE_CHAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_FOOTER) {
            val binding = ItemChatFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            FooterViewHolder(binding)
        } else {
            val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FooterViewHolder) {
            holder.binding.btnStartChatFooter.setOnClickListener { onStartChatClick() }
        } else if (holder is ChatViewHolder) {
            val chat = chatList[position]
            val context = holder.itemView.context
            val binding = holder.binding

            binding.tvContactName.text = chat.name
            binding.tvLastMessage.text = chat.lastMessage
            binding.tvMessageTime.text = chat.time

            if (chat.avatarUri != null) {
                binding.ivUserAvatar.setImageURI(Uri.parse(chat.avatarUri))
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_person)
            }

            val onlineBadge = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            when (chat.onlineStatus) {
                OnlineStatus.ONLINE -> {
                    binding.viewOnlineStatus.visibility = View.VISIBLE
                    onlineBadge.setColor(Color.parseColor("#4CAF50"))
                    binding.viewOnlineStatus.background = onlineBadge
                }
                OnlineStatus.BLOCKED -> {
                    binding.viewOnlineStatus.visibility = View.VISIBLE
                    onlineBadge.setColor(Color.parseColor("#F44336"))
                    binding.viewOnlineStatus.background = onlineBadge
                }
                OnlineStatus.OFFLINE -> {
                    binding.viewOnlineStatus.visibility = View.GONE
                }
            }

            when (chat.messageStatus) {
                MessageStatus.SENT -> {
                    binding.ivMessageStatus.visibility = View.VISIBLE
                    binding.ivMessageStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done))
                }
                MessageStatus.READ -> {
                    binding.ivMessageStatus.visibility = View.VISIBLE
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_done_all)
                }
                MessageStatus.ERROR -> {
                    binding.ivMessageStatus.visibility = View.VISIBLE
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_error)
                }
                MessageStatus.NONE -> {
                    binding.ivMessageStatus.visibility = View.GONE
                }
            }

            binding.ivMuteStatus.visibility = if (chat.isMuted) View.VISIBLE else View.GONE

            if (chat.unreadCount > 0) {
                binding.tvUnreadCounter.visibility = View.VISIBLE
                binding.tvUnreadCounter.text = chat.unreadCount.toString()

                val counterBg = GradientDrawable().apply { cornerRadius = 100f }
                if (chat.isMuted) {
                    counterBg.setColor(Color.parseColor("#8E8E93"))
                } else {
                    counterBg.setColor(Color.parseColor("#2196F3"))
                }
                binding.tvUnreadCounter.background = counterBg
            } else {
                binding.tvUnreadCounter.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = chatList.size + 1

    fun updateList(newList: List<ChatModel>) {
        chatList = newList
        notifyDataSetChanged()
    }
}
