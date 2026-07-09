package com.messenger.prime

// Класс, описывающий один чат в списке
data class ChatModel(
    val id: String, // Уникальный ID чата или пользователя
    val name: String, // Имя контакта
    val lastMessage: String, // Текст последнего сообщения
    val time: String, // Время (например, "14:23")
    val avatarUri: String?, // Ссылка на аватарку

    // Статус онлайна
    val onlineStatus: OnlineStatus = OnlineStatus.OFFLINE,

    // Статус прочтения/отправки сообщения (галочки)
    val messageStatus: MessageStatus = MessageStatus.NONE,

    // Счетчики и настройки
    val unreadCount: Int = 0, // Количество новых сообщений
    val isMuted: Boolean = false // Заглушен ли чат (иконка без звука)
)

// Перечисление для статуса в сети (кружочек на аватарке)
enum class OnlineStatus {
    OFFLINE, // Нет точки
    ONLINE,  // Зеленая точка
    BLOCKED  // Красная точка с вопросом
}

// Перечисление для галочек/ошибок отправки
enum class MessageStatus {
    NONE,       // Ничего не показываем (например, последнее сообщение не от нас)
    SENT,       // Одна галочка (Отправлено, но не прочитано)
    READ,       // Две галочки (Прочитано)
    ERROR       // Красный знак восклицания (Ошибка отправки)
}