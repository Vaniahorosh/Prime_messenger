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
        if (trimmed.isEmpty()) return "Логин не может быть пустым. Пожалуйста, придумайте уникальное имя пользователя."
        if (trimmed.length < 3) return "Слишком короткий логин (минимум 3 символа). Добавьте еще несколько букв или цифр."
        if (trimmed.length > 25) return "Слишком длинный логин (максимум 25 символов). Постарайтесь сделать его короче."
        if (!isValidLoginFormat(trimmed)) return "В логине допускаются только латинские буквы (a-z, A-Z), цифры (0-9) и знак подчеркивания (_). Пробелы и кириллица запрещены."
        if (isRestricted(trimmed)) return "Этот логин зарезервирован системой. Пожалуйста, выберите другой."
        if (sharedPrefs != null && trimmed != currentLogin && sharedPrefs.contains(trimmed)) {
            return "Этот логин уже занят другим профилем на данном устройстве. Попробуйте добавить цифры или изменить имя."
        }
        return null
    }

    /**
     * Валидация отображаемого имени пользователя (до 32 символов).
     */
    fun validateName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Имя не может быть пустым. Как к вам обращаться?"
        if (trimmed.length < 2) return "Имя должно содержать минимум 2 символа."
        if (trimmed.length > 32) return "Имя слишком длинное (максимум 32 символа)."
        if (isRestricted(trimmed)) return "Это имя защищено системой. Выберите другое."
        return null
    }

    /**
     * Валидация пароля (от 8 до 32 символов).
     */
    fun validatePassword(password: String): String? {
        if (password.isEmpty()) return "Пароль не может быть пустым."
        if (password.length < 8) return "Пароль слишком короткий (минимум 8 символов). Для безопасности используйте более длинный пароль."
        if (password.length > 32) return "Пароль слишком длинный (максимум 32 символа)."
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
