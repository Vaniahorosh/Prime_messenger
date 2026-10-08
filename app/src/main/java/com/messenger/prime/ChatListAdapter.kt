package com.messenger.prime

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.messenger.prime.databinding.ItemChatBinding
import com.messenger.prime.databinding.ItemChatFooterBinding
import java.io.File

class ChatDiffCallback(
    private val oldList: List<ChatModel>,
    private val newList: List<ChatModel>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        if (oldItem.id == "__footer__" || newItem.id == "__footer__") return null

        val diffBundle = Bundle()
        if (oldItem.lastMessage != newItem.lastMessage || oldItem.activityState != newItem.activityState || oldItem.isTyping != newItem.isTyping) {
            diffBundle.putString("lastMessage", newItem.lastMessage)
            diffBundle.putString("activityState", newItem.activityState)
            diffBundle.putBoolean("isTyping", newItem.isTyping)
        }
        if (oldItem.time != newItem.time) {
            diffBundle.putString("time", newItem.time)
        }
        if (oldItem.unreadCount != newItem.unreadCount) {
            diffBundle.putInt("unreadCount", newItem.unreadCount)
        }
        if (oldItem.messageStatus != newItem.messageStatus) {
            diffBundle.putString("messageStatus", newItem.messageStatus.name)
        }
        if (oldItem.onlineStatus != newItem.onlineStatus) {
            diffBundle.putString("onlineStatus", newItem.onlineStatus.name)
        }
        if (oldItem.avatarUri != newItem.avatarUri) {
            diffBundle.putString("avatarUri", newItem.avatarUri ?: "")
        }
        if (oldItem.name != newItem.name) {
            diffBundle.putString("name", newItem.name)
        }
        if (oldItem.isMuted != newItem.isMuted) {
            diffBundle.putBoolean("isMuted", newItem.isMuted)
        }

        return if (!diffBundle.isEmpty) diffBundle else null
    }
}

class ChatListAdapter(
    private var chatList: List<ChatModel>,
    private val onChatClick: (ChatModel) -> Unit,
    private val onDeleteClick: (ChatModel, Int) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CHAT = 0
        private const val TYPE_FOOTER = 1
    }

    private val footerDummy = ChatModel(id = "__footer__", name = "", lastMessage = "", time = "", avatarUri = null)

    var isSearchActive = false
        private set

    var currentSearchQuery = ""
        private set

    fun setSearchQuery(query: String) {
        currentSearchQuery = query.trim()
    }

    private fun highlightSearchText(text: String, query: String, accentColor: Int): CharSequence {
        if (query.isEmpty() || text.isEmpty()) return text
        val normalizedText = text.replace('ё', 'е').replace('Ё', 'Е')
        val normalizedQuery = query.replace('ё', 'е').replace('Ё', 'Е')
        val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return text

        val spannable = SpannableString(text)
        var hasMatch = false

        for (token in tokens) {
            var startIdx = normalizedText.indexOf(token, 0, ignoreCase = true)
            while (startIdx >= 0) {
                val endIdx = (startIdx + token.length).coerceAtMost(text.length)
                spannable.setSpan(
                    ForegroundColorSpan(accentColor),
                    startIdx,
                    endIdx,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIdx,
                    endIdx,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                hasMatch = true
                startIdx = normalizedText.indexOf(token, endIdx, ignoreCase = true)
            }
        }
        return if (hasMatch) spannable else text
    }

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        var isRevealed = false
        var typingRunnable: Runnable? = null
        var typingDotsCount = 0
        var isTypingAnimationRunning = false
        var currentBaseText = "Печатает"
        val handler = Handler(Looper.getMainLooper())

        @SuppressLint("SetTextI18n")
        fun startTypingAnimation(baseText: String = "Печатает") {
            if ((isTypingAnimationRunning && currentBaseText == baseText)) return
            stopTypingAnimation()

            isTypingAnimationRunning = true
            currentBaseText = baseText
            typingDotsCount = 0
            typingRunnable = object : Runnable {
                override fun run() {
                    typingDotsCount = (typingDotsCount + 1) % 4
                    val dots = ".".repeat(typingDotsCount)
                    binding.tvLastMessage.text = "$currentBaseText$dots"
                    handler.postDelayed(this, 300)
                }
            }
            handler.post(typingRunnable!!)
        }

        fun stopTypingAnimation() {
            isTypingAnimationRunning = false
            typingRunnable?.let { handler.removeCallbacks(it) }
            typingRunnable = null
        }

        fun resetReveal() {
            binding.layoutContent.animate().cancel()
            binding.layoutContent.translationX = 0f
            binding.layoutDelete.visibility = View.INVISIBLE
            isRevealed = false
        }
    }

    class FooterViewHolder(val binding: ItemChatFooterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        if (position == chatList.size) return TYPE_FOOTER
        return TYPE_CHAT
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty() || holder !is ChatViewHolder || position >= chatList.size) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        val diffBundle = payloads.first() as? Bundle ?: run {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        val context = holder.itemView.context
        val chat = chatList[position]
        val binding = holder.binding

        if (diffBundle.containsKey("name")) {
            val rawName = if (BluetoothAdapter.checkBluetoothAddress(chat.name)) "Собеседник" else chat.name
            val accentColor = ColorAccentManager.getCurrentAccentColor(context)
            if (isSearchActive && currentSearchQuery.isNotEmpty()) {
                binding.tvContactName.text = highlightSearchText(rawName, currentSearchQuery, accentColor)
            } else {
                binding.tvContactName.text = rawName
            }
        }

        if (diffBundle.containsKey("lastMessage") || diffBundle.containsKey("activityState") || diffBundle.containsKey("isTyping")) {
            val now = System.currentTimeMillis()
            val isCurrentlyTyping = chat.isTyping || "TYPING".equals(chat.activityState, ignoreCase = true) || (chat.typingUntil > 0 && chat.typingUntil > now)
            val accentColor = ColorAccentManager.getCurrentAccentColor(context)

            if (!isCurrentlyTyping && isSearchActive && currentSearchQuery.isNotEmpty()) {
                holder.stopTypingAnimation()
                binding.tvLastMessage.text = highlightSearchText(chat.lastMessage, currentSearchQuery, accentColor)
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_text_secondary))
                binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
            } else if ("SENDING_MEDIA".equals(chat.activityState, ignoreCase = true) || "SENDING_PHOTO".equals(chat.activityState, ignoreCase = true) || "SENDING_VIDEO".equals(chat.activityState, ignoreCase = true) || "SENDING_FILE".equals(chat.activityState, ignoreCase = true)) {
                holder.startTypingAnimation("Отправка медиа")
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
            } else if ("VIEWING_PHOTO".equals(chat.activityState, ignoreCase = true)) {
                holder.stopTypingAnimation()
                binding.tvLastMessage.text = "Смотрит фото"
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
            } else if ("VIEWING_VIDEO".equals(chat.activityState, ignoreCase = true)) {
                holder.stopTypingAnimation()
                binding.tvLastMessage.text = "Смотрит видео"
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
            } else if ("VIEWING_FILE".equals(chat.activityState, ignoreCase = true)) {
                holder.stopTypingAnimation()
                binding.tvLastMessage.text = "Смотрит файл"
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
            } else if (isCurrentlyTyping) {
                holder.startTypingAnimation("Печатает")
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
            } else {
                holder.stopTypingAnimation()
                binding.tvLastMessage.text = chat.lastMessage
                binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_text_secondary))
                binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
            }
        }

        if (diffBundle.containsKey("time")) {
            binding.tvMessageTime.text = chat.time
        }

        if (diffBundle.containsKey("unreadCount")) {
            val count = diffBundle.getInt("unreadCount")
            if (count > 0) {
                binding.tvUnreadCounter.visibility = View.VISIBLE
                binding.tvUnreadCounter.text = if (count > 99) "99+" else count.toString()
                val counterBg = GradientDrawable().apply { cornerRadius = 100f }
                counterBg.setColor(if (chat.isMuted) ContextCompat.getColor(context, R.color.prime_text_secondary) else ContextCompat.getColor(context, R.color.prime_brand))
                binding.tvUnreadCounter.background = counterBg
            } else {
                binding.tvUnreadCounter.visibility = View.GONE
            }
        }

        if (diffBundle.containsKey("messageStatus")) {
            when (chat.messageStatus) {
                MessageStatus.SENDING -> {
                    binding.ivMessageStatus.visibility = View.VISIBLE
                    binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                }
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
        }

        if (diffBundle.containsKey("onlineStatus")) {
            val onlineBadge = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            when (chat.onlineStatus) {
                OnlineStatus.ONLINE -> {
                    binding.viewOnlineStatus.visibility = View.VISIBLE
                    onlineBadge.setColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.viewOnlineStatus.background = onlineBadge
                }
                OnlineStatus.BLOCKED -> {
                    binding.viewOnlineStatus.visibility = View.VISIBLE
                    onlineBadge.setColor(ContextCompat.getColor(context, R.color.prime_danger))
                    binding.viewOnlineStatus.background = onlineBadge
                }
                OnlineStatus.OFFLINE -> {
                    binding.viewOnlineStatus.visibility = View.GONE
                }
            }
        }

        if (diffBundle.containsKey("isMuted")) {
            binding.ivMuteStatus.visibility = if (chat.isMuted) View.VISIBLE else View.GONE
        }

        if (diffBundle.containsKey("avatarUri") || diffBundle.containsKey("name")) {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FooterViewHolder -> {
                if (isSearchActive || chatList.isEmpty()) {
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
                    holder.itemView.visibility = View.GONE
                } else {
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    holder.itemView.visibility = View.VISIBLE
                    holder.binding.tvFooterEndHint.visibility = View.VISIBLE
                }
            }
            is ChatViewHolder -> {
                if (position < 0 || position >= chatList.size) return
                val chat = chatList[position]
                val context = holder.itemView.context
                val binding = holder.binding

                holder.resetReveal()

                try {
                    Glide.with(context).clear(binding.ivUserAvatar)
                } catch (_: Exception) {}
                binding.ivUserAvatar.setImageDrawable(null)
                binding.tvUserInitials.visibility = View.GONE
                binding.ivUserAvatar.visibility = View.INVISIBLE

                val now = System.currentTimeMillis()
                val isCurrentlyTyping = chat.isTyping || "TYPING".equals(chat.activityState, ignoreCase = true) || (chat.typingUntil > 0 && chat.typingUntil > now)

                val rawName = if (BluetoothAdapter.checkBluetoothAddress(chat.name)) "Собеседник" else chat.name
                val accentColor = ColorAccentManager.getCurrentAccentColor(context)

                if (isSearchActive && currentSearchQuery.isNotEmpty()) {
                    binding.tvContactName.text = highlightSearchText(rawName, currentSearchQuery, accentColor)
                } else {
                    binding.tvContactName.text = rawName
                }

                if (!isCurrentlyTyping && isSearchActive && currentSearchQuery.isNotEmpty()) {
                    holder.stopTypingAnimation()
                    binding.tvLastMessage.text = highlightSearchText(chat.lastMessage, currentSearchQuery, accentColor)
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_text_secondary))
                    binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
                } else if ("SENDING_MEDIA".equals(chat.activityState, ignoreCase = true) || "SENDING_PHOTO".equals(chat.activityState, ignoreCase = true) || "SENDING_VIDEO".equals(chat.activityState, ignoreCase = true) || "SENDING_FILE".equals(chat.activityState, ignoreCase = true)) {
                    holder.startTypingAnimation("Отправка медиа")
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
                } else if ("VIEWING_PHOTO".equals(chat.activityState, ignoreCase = true)) {
                    holder.stopTypingAnimation()
                    binding.tvLastMessage.text = "Смотрит фото"
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
                } else if ("VIEWING_VIDEO".equals(chat.activityState, ignoreCase = true)) {
                    holder.stopTypingAnimation()
                    binding.tvLastMessage.text = "Смотрит видео"
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
                } else if ("VIEWING_FILE".equals(chat.activityState, ignoreCase = true)) {
                    holder.stopTypingAnimation()
                    binding.tvLastMessage.text = "Смотрит файл"
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
                } else if (isCurrentlyTyping) {
                    holder.startTypingAnimation("Печатает")
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_success))
                    binding.tvLastMessage.setTypeface(null, Typeface.ITALIC)
                } else {
                    holder.stopTypingAnimation()
                    binding.tvLastMessage.text = chat.lastMessage
                    binding.tvLastMessage.setTextColor(ContextCompat.getColor(context, R.color.prime_text_secondary))
                    binding.tvLastMessage.setTypeface(null, Typeface.NORMAL)
                }
                binding.tvMessageTime.text = chat.time

                if (chat.id == "block_test_contact") {
                    val radiusPx = (14 * context.resources.displayMetrics.density).toInt()
                    Glide.with(context)
                        .load(R.drawable.prime_logo)
                        .transform(CenterCrop(), RoundedCorners(radiusPx))
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivUserAvatar)
                    binding.ivUserAvatar.visibility = View.VISIBLE
                    binding.tvUserInitials.visibility = View.GONE
                } else {
                    var avatarLoaded = false
                    val avatarToLoad = chat.avatarUri ?: AvatarManager.getContactAvatarUriOrFile(context, chat.id, chat.name, chat.id)

                    if (!avatarToLoad.isNullOrEmpty()) {
                        try {
                            loadAvatarIntoView(context, avatarToLoad, binding.ivUserAvatar, chat.name)
                            binding.ivUserAvatar.visibility = View.VISIBLE
                            binding.tvUserInitials.visibility = View.GONE
                            avatarLoaded = true
                        } catch (_: Exception) {}
                    }

                    if (!avatarLoaded) {
                        val initial = if (chat.name.isNotEmpty()) chat.name.take(1).uppercase() else "P"
                        binding.tvUserInitials.text = initial
                        binding.tvUserInitials.visibility = View.VISIBLE
                        binding.ivUserAvatar.visibility = View.INVISIBLE
                        val color = getAvatarColor(chat.name)
                        val radiusPx = 0.24f * 54 * context.resources.displayMetrics.density
                        val bg = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = radiusPx
                            setColor(color)
                        }
                        binding.tvUserInitials.background = bg
                    }
                }

                binding.layoutContent.setOnClickListener {
                    if (holder.isRevealed) {
                        animateHideDelete(holder)
                    } else {
                        onChatClick(chat)
                    }
                }

                binding.layoutContent.setOnLongClickListener {
                    if (!holder.isRevealed) {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        animateShowDelete(holder)
                        true
                    } else false
                }

                binding.btnDeleteContact.setOnClickListener {
                    onDeleteClick(chat, position)
                    holder.resetReveal()
                }

                val onlineBadge = GradientDrawable().apply { shape = GradientDrawable.OVAL }
                when (chat.onlineStatus) {
                    OnlineStatus.ONLINE -> {
                        binding.viewOnlineStatus.visibility = View.VISIBLE
                        onlineBadge.setColor(ContextCompat.getColor(context, R.color.prime_success))
                        binding.viewOnlineStatus.background = onlineBadge
                    }
                    OnlineStatus.BLOCKED -> {
                        binding.viewOnlineStatus.visibility = View.VISIBLE
                        onlineBadge.setColor(ContextCompat.getColor(context, R.color.prime_danger))
                        binding.viewOnlineStatus.background = onlineBadge
                    }
                    OnlineStatus.OFFLINE -> {
                        binding.viewOnlineStatus.visibility = View.GONE
                    }
                }

                when (chat.messageStatus) {
                    MessageStatus.SENDING -> {
                        binding.ivMessageStatus.visibility = View.VISIBLE
                        binding.ivMessageStatus.setImageResource(R.drawable.ic_clock)
                    }
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
                    binding.tvUnreadCounter.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
                    val counterBg = GradientDrawable().apply { cornerRadius = 100f }
                    counterBg.setColor(if (chat.isMuted) ContextCompat.getColor(context, R.color.prime_text_secondary) else ContextCompat.getColor(context, R.color.prime_brand))
                    binding.tvUnreadCounter.background = counterBg
                } else {
                    binding.tvUnreadCounter.visibility = View.GONE
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ChatViewHolder) {
            holder.stopTypingAnimation()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ChatViewHolder) {
            holder.stopTypingAnimation()
        }
    }

    override fun getItemCount(): Int = if (chatList.isEmpty()) 0 else chatList.size + 1

    fun updateList(newList: List<ChatModel>) {
        val oldAdapterItems = if (chatList.isEmpty()) emptyList() else chatList + footerDummy
        val newAdapterItems = if (newList.isEmpty()) emptyList() else newList + footerDummy

        val diffCallback = ChatDiffCallback(oldAdapterItems, newAdapterItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        chatList = ArrayList(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getChatList(): List<ChatModel> = chatList

    fun setSearchActive(active: Boolean) {
        if (isSearchActive == active) return
        isSearchActive = active
        notifyDataSetChanged()
    }

    private fun animateShowDelete(holder: ChatViewHolder) {
        val density = holder.itemView.resources.displayMetrics.density
        val targetTranslationX = -90f * density

        holder.binding.layoutDelete.visibility = View.VISIBLE
        holder.binding.layoutContent.animate()
            .translationX(targetTranslationX)
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction { holder.isRevealed = true }
            .start()
    }

    private fun animateHideDelete(holder: ChatViewHolder) {
        holder.binding.layoutContent.animate()
            .translationX(0f)
            .setDuration(220)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                holder.resetReveal()
            }
            .start()
    }
}
