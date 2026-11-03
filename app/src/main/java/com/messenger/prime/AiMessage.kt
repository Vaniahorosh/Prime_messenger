package com.messenger.prime

data class AiMessage(
    val text: String,
    val isUser: Boolean,
    val time: String,
    var reasoningText: String? = null
)
