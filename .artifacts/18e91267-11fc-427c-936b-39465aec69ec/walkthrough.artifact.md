# Валидация запрещенных имен и логинов

Я реализовал систему защиты, которая запрещает использование служебных и брендовых слов в качестве логинов и имен пользователей.

## Что было сделано

### 1. Умный инструмент валидации ([ValidationUtils.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ValidationUtils.kt))
- **Нормализация**: Перед проверкой текст очищается от пробелов и знаков пунктуации.
- **Защита от подмены символов (Homoglyphs)**: Реализован алгоритм, который распознает попытки заменить латинские буквы похожими кириллическими (и наоборот). Например, «Primе» (с русской 'е') будет считаться эквивалентным «prime».
- **Список запретов**:
    - `prime`, `прайм`, `праим`
    - `служба поддержки`, `служба поддржки`
    - `заметки`

### 2. Глобальная интеграция
- **[LoginActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/LoginActivity.kt)**: Блокировка ввода запрещенных логинов при входе/регистрации.
- **[RegisterActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/RegisterActivity.kt)**: Запрет использования этих слов в поле «Ваше Имя».
- **[SettingsActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/SettingsActivity.kt)**: Защита при смене логина или имени в настройках профиля.

## Визуальные изменения
- При попытке использовать запрещенное слово поле ввода подсвечивается красным с ошибкой **«Недопустимый формат»**.

## Проверка
- Сборка: **Build finished successfully.**
- Теперь пользователи не смогут выдавать себя за администрацию или использовать защищенные брендовые имена.

> [!IMPORTANT]
> Проверка работает регистронезависимо и учитывает любые вариации написания с пробелами или похожими по начертанию символами разных алфавитов.
