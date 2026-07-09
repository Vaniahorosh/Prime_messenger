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

class ChatAdapter(private var chatList: List<ChatModel>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        val context = holder.itemView.context
        val binding = holder.binding

        // 1. Установка базовых текстов
        binding.tvContactName.text = chat.name
        binding.tvLastMessage.text = chat.lastMessage
        binding.tvMessageTime.text = chat.time

        // 2. Установка аватарки
        if (chat.avatarUri != null) {
            binding.ivUserAvatar.setImageURI(Uri.parse(chat.avatarUri))
        } else {
            binding.ivUserAvatar.setImageResource(R.drawable.ic_person)
        }

        // 3. Обработка статуса онлайна (Кружочки на аватарке по схеме)
        val onlineBadge = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        when (chat.onlineStatus) {
            OnlineStatus.ONLINE -> {
                binding.viewOnlineStatus.visibility = View.VISIBLE
                onlineBadge.setColor(Color.parseColor("#4CAF50")) // Зеленый
                binding.viewOnlineStatus.background = onlineBadge
                // Если заблокирован, можно подставить кастомный drawable с вопросительным знаком, пока сделаем просто цвет
            }
            OnlineStatus.BLOCKED -> {
                binding.viewOnlineStatus.visibility = View.VISIBLE
                onlineBadge.setColor(Color.parseColor("#F44336")) // Красный
                binding.viewOnlineStatus.background = onlineBadge
            }
            OnlineStatus.OFFLINE -> {
                binding.viewOnlineStatus.visibility = View.GONE
            }
        }

        // 4. Обработка иконок статуса сообщения (Галочки)
        when (chat.messageStatus) {
            MessageStatus.SENT -> {
                binding.ivMessageStatus.visibility = View.VISIBLE
                binding.ivMessageStatus.setImageResource(R.drawable.ic_done) // Одна галочка
                binding.ivMessageStatus.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_done))
            }
            MessageStatus.READ -> {
                binding.ivMessageStatus.visibility = View.VISIBLE
                binding.ivMessageStatus.setImageResource(R.drawable.ic_done_all) // Две галочки
            }
            MessageStatus.ERROR -> {
                binding.ivMessageStatus.visibility = View.VISIBLE
                binding.ivMessageStatus.setImageResource(R.drawable.ic_error) // Восклицательный знак
            }
            MessageStatus.NONE -> {
                binding.ivMessageStatus.visibility = View.GONE
            }
        }

        // 5. Обработка статуса "Без звука"
        binding.ivMuteStatus.visibility = if (chat.isMuted) View.VISIBLE else View.GONE

        // 6. Обработка счетчика сообщений (Цвет зависит от Mute по твоей схеме)
        if (chat.unreadCount > 0) {
            binding.tvUnreadCounter.visibility = View.VISIBLE
            binding.tvUnreadCounter.text = chat.unreadCount.toString()

            val counterBg = GradientDrawable().apply { cornerRadius = 100f }
            if (chat.isMuted) {
                counterBg.setColor(Color.parseColor("#8E8E93")) // Серый счетчик, если без звука
            } else {
                counterBg.setColor(Color.parseColor("#2196F3")) // Синий счетчик по умолчанию
            }
            binding.tvUnreadCounter.background = counterBg
        } else {
            binding.tvUnreadCounter.visibility = View.GONE
        }

        // 7. Клик по элементу (Переход в ChatActivity пока закомментирован)
        holder.itemView.setOnClickListener {
            /*
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("CHAT_ID", chat.id)
            context.startActivity(intent)
            */
        }
    }


    override fun getItemCount(): Int = chatList.size
    // ... тут твой код onBindViewHolder и getItemCount ...

    // ДОБАВЬ ВОТ ЭТО:
    fun updateList(newList: List<ChatModel>) {
        chatList = newList
        notifyDataSetChanged()
    }
} // Это самая последняя скобка всего файла ChatAdapter.kt
