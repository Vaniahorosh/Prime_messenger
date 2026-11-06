package com.messenger.prime

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.messenger.prime.databinding.DialogAiChatHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiChatHistoryBottomSheet(
    private val activity: Activity,
    private val onSessionSelected: (AiChatSession) -> Unit,
    private val onCreateNewSession: () -> Unit,
    private val onSessionDeleted: (String) -> Unit
) {
    private var dialog: BottomSheetDialog? = null
    private var binding: DialogAiChatHistoryBinding? = null

    fun show() {
        val b = DialogAiChatHistoryBinding.inflate(LayoutInflater.from(activity))
        binding = b

        val d = BottomSheetDialog(activity, R.style.Theme_Prime_BottomSheetDialog)
        d.setContentView(b.root)

        d.setOnShowListener {
            val bottomSheetView = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheetView != null) {
                bottomSheetView.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(bottomSheetView)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isFitToContents = true
            }
        }

        if (d.window != null) {
            d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            d.window?.setDimAmount(0.45f)
        }

        // Setup BlurView
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
            ?: (activity.window.decorView as ViewGroup)
        val windowBg = activity.window.decorView.background
        val isDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val overlayColor = if (isDark) Color.parseColor("#450F172A") else Color.parseColor("#45154B87")

        b.blurViewHistorySheet.setupBlur(rootView, 20f, overlayColor, windowBg)

        // Tint icons and buttons dynamically
        ColorAccentManager.tintViews(activity, b.btnCreateNewChat, b.ivHeaderIcon)

        refreshSessionsList()

        b.btnCreateNewChat.setOnClickListener {
            d.dismiss()
            onCreateNewSession()
        }

        dialog = d
        d.show()
    }

    private fun refreshSessionsList() {
        val b = binding ?: return
        val sessions = AiChatStorage.getAllSessions(activity)
        val activeId = AiChatStorage.getActiveSessionId(activity)

        if (sessions.isEmpty()) {
            b.tvEmptyHistoryNotice.visibility = View.VISIBLE
            b.rvAiChatHistory.visibility = View.GONE
        } else {
            b.tvEmptyHistoryNotice.visibility = View.GONE
            b.rvAiChatHistory.visibility = View.VISIBLE
            b.rvAiChatHistory.layoutManager = LinearLayoutManager(activity)
            b.rvAiChatHistory.adapter = AiChatHistoryAdapter(
                sessions = sessions,
                activeSessionId = activeId,
                onItemClick = { session ->
                    dialog?.dismiss()
                    onSessionSelected(session)
                },
                onDeleteClick = { session ->
                    AiChatStorage.deleteSession(activity, session.id)
                    onSessionDeleted(session.id)
                    refreshSessionsList()
                }
            )
        }
    }
}

class AiChatHistoryAdapter(
    private val sessions: List<AiChatSession>,
    private val activeSessionId: String,
    private val onItemClick: (AiChatSession) -> Unit,
    private val onDeleteClick: (AiChatSession) -> Unit
) : RecyclerView.Adapter<AiChatHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.containerSessionItem)
        val ivIcon: ImageView = view.findViewById(R.id.ivSessionIcon)
        val ivActiveBadge: ImageView = view.findViewById(R.id.ivActiveBadge)
        val tvTitle: TextView = view.findViewById(R.id.tvSessionTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvSessionSubtitle)
        val btnDelete: ImageView = view.findViewById(R.id.btnDeleteChatSession)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ai_chat_history, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val isActive = session.id == activeSessionId

        holder.tvTitle.text = session.title

        val dateStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.updatedAt))
        val countMsg = session.messages.size
        val countText = when {
            countMsg == 1 -> "1 сообщение"
            countMsg in 2..4 -> "$countMsg сообщения"
            else -> "$countMsg сообщений"
        }
        holder.tvSubtitle.text = "$countText • $dateStr"

        if (isActive) {
            holder.ivActiveBadge.visibility = View.VISIBLE
            holder.container.alpha = 1.0f
            holder.tvTitle.setTextColor(ColorAccentManager.getCurrentAccentColor(holder.itemView.context))
            holder.ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(holder.itemView.context))
            holder.ivActiveBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(ColorAccentManager.getCurrentAccentColor(holder.itemView.context))
        } else {
            holder.ivActiveBadge.visibility = View.GONE
            holder.container.alpha = 0.85f
            holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.prime_text_primary))
            holder.ivIcon.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, R.color.prime_text_secondary))
        }

        holder.container.setOnClickListener { onItemClick(session) }
        holder.btnDelete.setOnClickListener { onDeleteClick(session) }
    }

    override fun getItemCount() = sessions.size
}
