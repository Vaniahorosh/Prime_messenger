package com.messenger.prime

/**
 * Утилиты для жесткой валидации ввода в приложении.
 */
object ValidationUtils {

    // Список запрещенных слов в максимально нормализованном виде
    private val restrictedKeywords = listOf(
        "prime",
        "прайм",
        "праим",
        "службаподдержки",
        "службаподдржки",
        "заметки",
        "support",
        "admin",
        "админ"
    )

    /**
     * Проверяет формат логина: только строчная латиница, цифры и подчеркивание.
     */
    fun isValidLoginFormat(text: String): Boolean {
        val regex = Regex("^[a-z0-9_]+$")
        return regex.matches(text)
    }

    /**
     * Возвращает конкретную причину ошибки валидации или null, если текст корректен.
     */
    fun getValidationError(text: String, isLogin: Boolean): String? {
        if (text.isEmpty()) return null

        if (isLogin && !isValidLoginFormat(text)) {
            return "Используйте латиницу, цифры и _"
        }

        if (isRestricted(text)) {
            return "Это имя защищено системой"
        }

        return null
    }

    /**
     * Глубокая проверка на наличие запрещенных фраз с учетом подмены букв и шума.
     */
    fun isRestricted(text: String?): Boolean {
        if (text.isNullOrBlank()) return false

        // 1. Предварительная очистка: нижний регистр и удаление ВСЕГО, что не буквы и не цифры
        // Это убьет попытки обхода через "p.r.i.m.e", "s u p p o r t" и т.д.
        val baseClean = text.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")

        // 2. Полная нормализация гомоглифов (сведение похожих знаков к одному "скелету")
        val skeleton = normalizeToSkeleton(baseClean)

        // 3. Проверка на вхождение
        return restrictedKeywords.any { keyword ->
            val normalizedKeyword = normalizeToSkeleton(keyword.replace(Regex("[^\\p{L}\\p{N}]"), ""))
            skeleton.contains(normalizedKeyword) || baseClean.contains(normalizedKeyword)
        }
    }

    /**
     * Переводит текст в "скелетный" вид, заменяя все похожие символы на один базовый.
     */
    private fun normalizeToSkeleton(input: String): String {
        val mapping = mapOf(
            // Кириллица -> Латиница (наиболее частые подмены)
            'а' to 'a', 'б' to 'b', 'в' to 'v', 'е' to 'e', 'з' to 'z',
            'и' to 'i', 'й' to 'i', 'к' to 'k', 'л' to 'l', 'м' to 'm',
            'н' to 'n', 'о' to 'o', 'р' to 'p', 'с' to 'c', 'т' to 't',
            'у' to 'y', 'х' to 'x', 'і' to 'i', 'ј' to 'j', 'ь' to 'b',
            // Спецсимволы и цифры, похожие на буквы
            '@' to 'a', '4' to 'a', '0' to 'o', '3' to 'e', '1' to 'i',
            '!' to 'i', '$' to 's', '5' to 's', '7' to 't', '8' to 'b'
        )

        val sb = StringBuilder()
        for (char in input) {
            sb.append(mapping[char] ?: char)
        }
        return sb.toString()
    }
}
