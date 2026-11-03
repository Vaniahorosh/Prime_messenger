package com.messenger.prime

import java.util.Locale

/**
 * Класс для автоматического определения провайдера нейросети, модели и эндпоинта по API ключу.
 */
data class AiModelConfig(
    val provider: String,
    val model: String,
    val endpointUrl: String,
    val maxContextTokens: Int = 128000
)

object AiModelDetector {

    fun detect(apiKey: String, savedProvider: String = "", savedModel: String = "", customUrl: String = ""): AiModelConfig {
        val trimmedKey = apiKey.trim()

        // 1. Если ключ начинается на AIzaSy -> Google Gemini
        if (trimmedKey.startsWith("AIzaSy")) {
            val model = if (savedModel.contains("gemini", ignoreCase = true)) savedModel else "gemini-1.5-flash"
            return AiModelConfig(
                provider = "Gemini",
                model = model,
                endpointUrl = "https://generativelanguage.googleapis.com",
                maxContextTokens = 1048576
            )
        }

        // 2. Если ключ начинается на sk-ant- -> Anthropic (Claude)
        if (trimmedKey.startsWith("sk-ant-")) {
            val model = if (savedModel.contains("claude", ignoreCase = true)) savedModel else "claude-3-5-sonnet-20241022"
            return AiModelConfig(
                provider = "Anthropic",
                model = model,
                endpointUrl = "https://api.anthropic.com/v1/messages",
                maxContextTokens = 200000
            )
        }

        // 3. Если ключ начинается на sk-or-v1- -> OpenRouter
        if (trimmedKey.startsWith("sk-or-v1-")) {
            val model = if (savedModel.isNotBlank()) savedModel else "google/gemini-2.0-flash-exp:free"
            return AiModelConfig(
                provider = "OpenRouter",
                model = model,
                endpointUrl = "https://openrouter.ai/api/v1/chat/completions",
                maxContextTokens = 128000
            )
        }

        // 4. Если ключ начинается на sk-proj-, sk-admin-, sk-svcacct- -> OpenAI
        if (trimmedKey.startsWith("sk-proj-") || trimmedKey.startsWith("sk-admin-") || trimmedKey.startsWith("sk-svcacct-")) {
            val model = if (savedModel.contains("gpt", ignoreCase = true)) savedModel else "gpt-4o-mini"
            return AiModelConfig(
                provider = "OpenAI",
                model = model,
                endpointUrl = "https://api.openai.com/v1/chat/completions",
                maxContextTokens = 128000
            )
        }

        // 5. Если сохранен провайдер DeepSeek или в ключе есть признаки / настраиваемый URL
        if (savedProvider == "DeepSeek / Custom" || customUrl.contains("deepseek", ignoreCase = true)) {
            val endpoint = if (customUrl.isNotBlank()) {
                if (customUrl.endsWith("/chat/completions")) customUrl else "$customUrl/chat/completions"
            } else "https://api.deepseek.com/v1/chat/completions"

            val model = if (savedModel.isNotBlank()) savedModel else "deepseek-chat"
            return AiModelConfig(
                provider = "DeepSeek",
                model = model,
                endpointUrl = endpoint,
                maxContextTokens = 64000
            )
        }

        // 6. По умолчанию для стандартных ключей sk- (OpenAI / DeepSeek / Gemini fallback)
        if (trimmedKey.startsWith("sk-")) {
            return if (savedProvider == "OpenAI" || savedModel.contains("gpt", ignoreCase = true)) {
                AiModelConfig(
                    provider = "OpenAI",
                    model = savedModel.ifBlank { "gpt-4o-mini" },
                    endpointUrl = "https://api.openai.com/v1/chat/completions",
                    maxContextTokens = 128000
                )
            } else {
                AiModelConfig(
                    provider = "DeepSeek",
                    model = savedModel.ifBlank { "deepseek-chat" },
                    endpointUrl = if (customUrl.isNotBlank()) customUrl else "https://api.deepseek.com/v1/chat/completions",
                    maxContextTokens = 64000
                )
            }
        }

        // Fallback: Gemini по умолчанию
        return AiModelConfig(
            provider = savedProvider.ifBlank { "Gemini" },
            model = savedModel.ifBlank { "gemini-1.5-flash" },
            endpointUrl = if (customUrl.isNotBlank()) customUrl else "https://generativelanguage.googleapis.com",
            maxContextTokens = 1048576
        )
    }

    /**
     * Оценка количества токенов в списке сообщений.
     */
    fun estimateTokens(messages: List<AiMessage>): Int {
        var totalChars = 0
        for (msg in messages) {
            totalChars += msg.text.length
        }
        return (totalChars / 3.0).toInt().coerceAtLeast(0)
    }

    /**
     * Расчет процента израсходованного контекста/квоты.
     */
    fun calculateUsagePercent(estimatedTokens: Int, maxTokens: Int): String {
        if (maxTokens <= 0) return "0%"
        val pct = (estimatedTokens.toDouble() / maxTokens.toDouble()) * 100.0
        return if (pct == 0.0) {
            "0%"
        } else if (pct < 0.1) {
            "<0.1%"
        } else if (pct < 10.0) {
            String.format(Locale.US, "%.1f%%", pct)
        } else {
            "${pct.toInt()}%"
        }
    }

    /**
     * Форматированный вывод израсходованной квоты и количества токенов.
     */
    fun formatTokenUsage(estimatedTokens: Int, maxTokens: Int): String {
        val tokenFormatted = if (estimatedTokens >= 1000) "${estimatedTokens / 1000}k" else "$estimatedTokens"
        val maxFormatted = if (maxTokens >= 1000000) "${maxTokens / 1000000}M" else "${maxTokens / 1000}k"
        return "Квота ${calculateUsagePercent(estimatedTokens, maxTokens)} ($tokenFormatted/$maxFormatted)"
    }
}
