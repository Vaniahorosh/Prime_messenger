package com.messenger.prime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class AiChatSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Новый чат",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    val messages: MutableList<AiMessage> = mutableListOf(),
    var modelName: String = "gemini-1.5-flash",
    var tokensUsed: Int = 0
)

object AiChatStorage {

    private const val FILE_NAME = "ai_chat_sessions.json"
    private const val PREF_KEY_ACTIVE_SESSION = "ai_active_session_id"

    @Synchronized
    fun getAllSessions(context: Context): MutableList<AiChatSession> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()

        val list = mutableListOf<AiChatSession>()
        try {
            val jsonStr = file.readText(Charsets.UTF_8)
            if (jsonStr.isBlank()) return list

            val jsonArr = JSONArray(jsonStr)
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Новый чат")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val modelName = obj.optString("modelName", "gemini-1.5-flash")
                val tokensUsed = obj.optInt("tokensUsed", 0)

                val msgsList = mutableListOf<AiMessage>()
                val msgsArr = obj.optJSONArray("messages")
                if (msgsArr != null) {
                    for (j in 0 until msgsArr.length()) {
                        val mObj = msgsArr.getJSONObject(j)
                        msgsList.add(
                            AiMessage(
                                text = mObj.optString("text", ""),
                                isUser = mObj.optBoolean("isUser", false),
                                time = mObj.optString("time", "")
                            )
                        )
                    }
                }

                list.add(
                    AiChatSession(
                        id = id,
                        title = title,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        messages = msgsList,
                        modelName = modelName,
                        tokensUsed = tokensUsed
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.sortByDescending { it.updatedAt }
        return list
    }

    @Synchronized
    fun getSession(context: Context, id: String): AiChatSession? {
        return getAllSessions(context).find { it.id == id }
    }

    @Synchronized
    fun saveSession(context: Context, session: AiChatSession) {
        val sessions = getAllSessions(context)
        val index = sessions.indexOfFirst { it.id == session.id }
        session.updatedAt = System.currentTimeMillis()

        if (index >= 0) {
            sessions[index] = session
        } else {
            sessions.add(0, session)
        }

        saveAllSessions(context, sessions)
    }

    @Synchronized
    fun deleteSession(context: Context, id: String) {
        val sessions = getAllSessions(context)
        sessions.removeAll { it.id == id }
        saveAllSessions(context, sessions)

        if (getActiveSessionId(context) == id) {
            val nextActiveId = sessions.firstOrNull()?.id ?: ""
            setActiveSessionId(context, nextActiveId)
        }
    }

    @Synchronized
    fun createNewSession(context: Context, initialModel: String = "gemini-1.5-flash"): AiChatSession {
        val newSession = AiChatSession(
            id = UUID.randomUUID().toString(),
            title = "Новый чат",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            messages = mutableListOf(),
            modelName = initialModel
        )
        saveSession(context, newSession)
        setActiveSessionId(context, newSession.id)
        return newSession
    }

    @Synchronized
    private fun saveAllSessions(context: Context, sessions: List<AiChatSession>) {
        val jsonArr = JSONArray()
        for (s in sessions) {
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
                put("modelName", s.modelName)
                put("tokensUsed", s.tokensUsed)

                val mArr = JSONArray()
                for (m in s.messages) {
                    mArr.put(JSONObject().apply {
                        put("text", m.text)
                        put("isUser", m.isUser)
                        put("time", m.time)
                    })
                }
                put("messages", mArr)
            }
            jsonArr.put(sObj)
        }

        val file = File(context.filesDir, FILE_NAME)
        file.writeText(jsonArr.toString(), Charsets.UTF_8)
    }

    fun getActiveSessionId(context: Context): String {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        return sp.getString(PREF_KEY_ACTIVE_SESSION, "") ?: ""
    }

    fun setActiveSessionId(context: Context, id: String) {
        val sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE)
        sp.edit().putString(PREF_KEY_ACTIVE_SESSION, id).apply()
    }

    /**
     * Автоматическая генерация короткой темы чата из первого сообщения пользователя.
     */
    fun autoGenerateTitle(prompt: String): String {
        val clean = prompt.trim()
            .replace("\n", " ")
            .replace(Regex("[#*`_\\[\\]]"), "")
            .trim()

        if (clean.length <= 32) {
            return clean.ifBlank { "Новый чат" }
        }
        val words = clean.split(" ")
        val sb = StringBuilder()
        for (w in words) {
            if (sb.length + w.length + 1 > 30) break
            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(w)
        }
        val title = sb.toString().trim()
        return if (title.isNotBlank()) "$title..." else "Новый чат"
    }
}
