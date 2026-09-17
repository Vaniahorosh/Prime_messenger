package com.messenger.prime

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.messenger.prime.databinding.ItemChatBinding
import com.messenger.prime.databinding.ItemChatFooterBinding
import com.messenger.prime.databinding.ItemChatIslandHeaderBinding

class ChatAdapter(
    private var chatList: List<ChatModel>,
    private var userAvatarUri: String? = null,
    private var userName: String = "Пользователь",
    private var currentNetworkHint: String = "Прайм",
    private val onStartChatClick: () -> Unit,
    private val onAvatarClick: () -> Unit,
    private val onAvatarLongClick: () -> Unit,
    private val onHeaderSearchClick: () -> Unit,
    private val onNameClick: () -> Unit,
    private val onChatClick: (ChatModel) -> Unit,
    private val onDeleteClick: (ChatModel, Int) -> Unit,
    private val onEditClick: (ChatModel, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CHAT = 0
        private const val TYPE_FOOTER = 1
        private const val TYPE_HEADER = 2
    }

    var isSearchActive = false
        private set

    class ChatViewHolder(val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        var isRevealed = false
        
        fun resetReveal() {
            binding.layoutContent.animate().cancel()
            binding.layoutContent.translationX = 0f
            binding.layoutDelete.visibility = View.INVISIBLE
            isRevealed = false
        }
    }

    class FooterViewHolder(val binding: ItemChatFooterBinding) : RecyclerView.ViewHolder(binding.root)
    
    class HeaderViewHolder(val binding: ItemChatIslandHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        private val handler = Handler(Looper.getMainLooper())
        private var isShowingName = false
        private val switchRunnable = object : Runnable {
            override fun run() {
                if (networkHint == "Прайм" || networkHint == "ПОИСК") {
                    isShowingName = !isShowingName
                    updateTitle()
                }
                handler.postDelayed(this, 5000)
            }
        }

        private var currentUserName = ""
        private var networkHint = "Прайм"

        fun bind(avatarUri: String?, userName: String, netHint: String, onAvatarClick: () -> Unit, onAvatarLongClick: () -> Unit, onSearchClick: () -> Unit, onNameClick: () -> Unit) {
            currentUserName = userName
            networkHint = netHint
            
            if (avatarUri != null) {
                binding.ivHeaderAvatar.setImageURI(Uri.parse(avatarUri))
                binding.tvHeaderInitials.visibility = View.GONE
                binding.ivHeaderAvatar.visibility = View.VISIBLE
            } else {
                val initial = userName.take(1).uppercase()
                binding.tvHeaderInitials.text = initial
                binding.tvHeaderInitials.visibility = View.VISIBLE
                binding.ivHeaderAvatar.visibility = View.INVISIBLE
                
                val color = getAvatarColor(userName)
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 15 * binding.root.context.resources.displayMetrics.density
                    setColor(color)
                }
                binding.tvHeaderInitials.background = bg
            }
            binding.ivHeaderAvatar.setOnClickListener { onAvatarClick() }
            binding.tvHeaderInitials.setOnClickListener { onAvatarClick() }
            
            binding.ivHeaderAvatar.setOnLongClickListener {
                onAvatarLongClick()
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                true
            }
            binding.tvHeaderInitials.setOnLongClickListener {
                onAvatarLongClick()
                it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                true
            }

            binding.btnHeaderSearch.setOnClickListener { onSearchClick() }
            
            if (binding.tsHeaderTitle.childCount == 0) {
                binding.tsHeaderTitle.setFactory {
                    TextView(binding.root.context).apply {
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                }
            }
            
            updateTitle()
            binding.tsHeaderTitle.setOnClickListener { 
                if (isShowingName) onNameClick() 
            }

            handler.removeCallbacks(switchRunnable)
            handler.postDelayed(switchRunnable, 5000)
        }

        private fun updateTitle() {
            if (networkHint != "Прайм" && networkHint != "ПОИСК") {
                binding.tsHeaderTitle.setText(networkHint)
            } else {
                binding.tsHeaderTitle.setText(if (isShowingName) currentUserName else "Прайм")
            }
        }

        fun stopAnimation() {
            handler.removeCallbacks(switchRunnable)
        }

        private fun getAvatarColor(name: String): Int {
            val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
            val index = Math.abs(name.hashCode()) % colors.size
            return Color.parseColor(colors[index])
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (!isSearchActive && position == 0) return TYPE_HEADER
        val actualPos = if (!isSearchActive) position - 1 else position
        return if (actualPos == chatList.size) TYPE_FOOTER else TYPE_CHAT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemChatIslandHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                HeaderViewHolder(binding)
            }
            TYPE_FOOTER -> {
                val binding = ItemChatFooterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FooterViewHolder(binding)
            }
            else -> {
                val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(userAvatarUri, userName, currentNetworkHint, onAvatarClick, onAvatarLongClick, onHeaderSearchClick, onNameClick)
            }
            is FooterViewHolder -> {
                holder.binding.btnStartChatFooter.setOnClickListener { onStartChatClick() }
                if (chatList.isEmpty()) {
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
                    holder.itemView.visibility = View.GONE
                } else {
                    holder.itemView.layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    holder.itemView.visibility = View.VISIBLE
                    holder.binding.tvFooterEndHint.visibility = View.VISIBLE
                    val params = holder.binding.btnStartChatFooter.layoutParams
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    holder.binding.btnStartChatFooter.layoutParams = params
                }
            }
            is ChatViewHolder -> {
                val actualPos = if (!isSearchActive) position - 1 else position
                val chat = chatList[actualPos]
                val context = holder.itemView.context
                val binding = holder.binding

                holder.resetReveal()

                binding.tvContactName.text = chat.name
                binding.tvLastMessage.text = chat.lastMessage
                binding.tvMessageTime.text = chat.time

                if (chat.id == "block_test_contact") {
                    binding.ivUserAvatar.setImageResource(R.drawable.prime_logo)
                    binding.ivUserAvatar.visibility = View.VISIBLE
                    binding.tvUserInitials.visibility = View.GONE
                } else if (chat.avatarUri != null && chat.avatarUri.isNotEmpty()) {
                    try {
                        binding.ivUserAvatar.setImageURI(Uri.parse(chat.avatarUri))
                        binding.ivUserAvatar.visibility = View.VISIBLE
                        binding.tvUserInitials.visibility = View.GONE
                    } catch (e: Exception) {
                        binding.ivUserAvatar.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    val initial = chat.name.take(1).uppercase()
                    binding.tvUserInitials.text = initial
                    binding.tvUserInitials.visibility = View.VISIBLE
                    binding.ivUserAvatar.visibility = View.INVISIBLE
                    
                    val color = getAvatarColor(chat.name)
                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8 * context.resources.displayMetrics.density
                        setColor(color)
                    }
                    binding.tvUserInitials.background = bg
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

                binding.btnEditContact.setOnClickListener {
                    onEditClick(chat, position)
                    animateHideDelete(holder)
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
                    counterBg.setColor(if (chat.isMuted) ContextCompat.getColor(context, R.color.prime_text_secondary) else ContextCompat.getColor(context, R.color.prime_info))
                    binding.tvUnreadCounter.background = counterBg
                } else {
                    binding.tvUnreadCounter.visibility = View.GONE
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is HeaderViewHolder) {
            holder.stopAnimation()
        }
    }

    override fun getItemCount(): Int = if (isSearchActive) chatList.size + 1 else chatList.size + 2

    fun updateList(newList: List<ChatModel>, notify: Boolean = true) {
        chatList = ArrayList(newList)
        if (notify) notifyDataSetChanged()
    }

    fun getChatList(): List<ChatModel> = chatList

    fun setSearchActive(active: Boolean) {
        if (isSearchActive == active) return
        isSearchActive = active
        notifyDataSetChanged()
    }

    fun updateAvatar(newUri: String?) {
        userAvatarUri = newUri
        if (!isSearchActive) notifyItemChanged(0)
    }

    fun updateUserName(newName: String) {
        userName = newName
        if (!isSearchActive) notifyItemChanged(0)
    }

    fun updateNetworkHint(newHint: String) {
        currentNetworkHint = newHint
        if (!isSearchActive) notifyItemChanged(0)
    }

    private fun animateShowDelete(holder: ChatViewHolder) {
        holder.binding.layoutDelete.visibility = View.VISIBLE
        holder.binding.layoutContent.animate()
            .translationX(-440f) // Сдвигаем больше для двух кнопок по 72dp
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction { holder.isRevealed = true }
            .start()
    }

    private fun animateHideDelete(holder: ChatViewHolder) {
        holder.binding.layoutContent.animate()
            .translationX(0f)
            .setDuration(250)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction { 
                holder.resetReveal()
            }
            .start()
    }

    private fun getAvatarColor(name: String): Int {
        val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722")
        val index = Math.abs(name.hashCode()) % colors.size
        return Color.parseColor(colors[index])
    }
}
