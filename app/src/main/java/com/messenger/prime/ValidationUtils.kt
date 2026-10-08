package com.messenger.prime

import android.content.SharedPreferences

/**
 * Утилиты для жесткой валидации ввода в приложении.
 */
object ValidationUtils {

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
     * Проверяет формат логина: только латиница, цифры и подчеркивание.
     */
    fun isValidLoginFormat(text: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9_]+$")
        return regex.matches(text)
    }

    /**
     * Полная валидация логина с учетом формата, длины, спецслов и уникальности.
     */
    fun validateLogin(login: String, sharedPrefs: SharedPreferences? = null, currentLogin: String? = null): String? {
        val trimmed = login.trim()
        if (trimmed.isEmpty()) return "Введите логин"
        if (trimmed.length < 3) return "Логин минимум 3 символа"
        if (trimmed.length > 25) return "Логин максимум 25 символов"
        if (!isValidLoginFormat(trimmed)) return "Только латиница, цифры и _"
        if (isRestricted(trimmed)) return "Этот логин защищен системой"
        if (sharedPrefs != null && trimmed != currentLogin && sharedPrefs.contains(trimmed)) {
            return "Этот логин уже занят на устройстве"
        }
        return null
    }

    /**
     * Валидация отображаемого имени пользователя.
     */
    fun validateName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Как вас зовут?"
        if (trimmed.length < 2) return "Имя слишком короткое"
        if (trimmed.length > 30) return "Имя слишком длинное"
        if (isRestricted(trimmed)) return "Это имя защищено системой"
        return null
    }

    /**
     * Валидация пароля.
     */
    fun validatePassword(password: String): String? {
        if (password.isEmpty()) return "Введите пароль"
        if (password.length < 8) return "Пароль минимум 8 символов"
        return null
    }

    /**
     * Возвращает конкретную причину ошибки валидации или null, если текст корректен (совместимость).
     */
    fun getValidationError(text: String, isLogin: Boolean): String? {
        return if (isLogin) validateLogin(text) else validateName(text)
    }

    /**
     * Глубокая проверка на наличие запрещенных фраз с учетом подмены букв и шума.
     */
    fun isRestricted(text: String?): Boolean {
        if (text.isNullOrBlank()) return false

        val baseClean = text.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        val skeleton = normalizeToSkeleton(baseClean)

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
            'а' to 'a', 'б' to 'b', 'в' to 'v', 'г' to 'g', 'д' to 'd',
            'е' to 'e', 'ё' to 'e', 'ж' to 'z', 'з' to 'z', 'и' to 'i',
            'й' to 'i', 'к' to 'k', 'л' to 'l', 'м' to 'm', 'н' to 'n',
            'о' to 'o', 'п' to 'n', 'р' to 'p', 'с' to 'c', 'т' to 't',
            'у' to 'y', 'ф' to 'f', 'х' to 'x', 'ц' to 'c', 'ч' to 'c',
            'ш' to 's', 'щ' to 's', 'ы' to 'i', 'э' to 'e', 'ю' to 'u',
            'я' to 'a', 'і' to 'i', 'ј' to 'j', 'ь' to 'b', 'ъ' to 'b',
            '@' to 'a', '4' to 'a', '0' to 'o', '3' to 'e', '1' to 'i',
            '!' to 'i', '$' to 's', '5' to 's', '7' to 't', '8' to 'b',
            '|' to 'l', 'v' to 'v', 'w' to 'v'
        )

        val sb = StringBuilder()
        for (char in input) {
            sb.append(mapping[char] ?: char)
        }
        return sb.toString()
    }
}
