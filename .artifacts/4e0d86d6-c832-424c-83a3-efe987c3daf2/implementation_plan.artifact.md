# План реализации - Переход на светлую тему в ChatList

Этот план описывает изменения для перевода списка чатов на светлую тему с сохранением стиля glassmorphism и хорошей читаемости.

## Предлагаемые изменения

### 1. Новые графические ресурсы

#### [NEW] [bg_chat_item_light.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/drawable/bg_chat_item_light.xml)
- Создать фон для элементов списка: белый цвет, закругление 16dp, без обводки (или очень тонкая серая).

#### [NEW] [bg_island_light.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/drawable/bg_island_light.xml)
- Создать фон для островка управления: белый полупрозрачный цвет (`#CCFFFFFF`), закругление 24dp.

### 2. Разметка элементов списка

#### [MODIFY] [item_chat.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/item_chat.xml)
- Заменить фон на `@drawable/bg_chat_item_light`.
- Изменить цвета текста:
    - `tvContactName`: черный (`#000000`).
    - `tvLastMessage`: темно-серый (`#666666`).
    - `tvMessageTime`: серый (`#8E8E93`).

#### [MODIFY] [item_chat_footer.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/item_chat_footer.xml)
- Изменить стиль кнопки "Начать общение": сделать ее синей с белым текстом для контраста на белом фоне.
- Изменить цвет нижнего текста на серый.

### 3. Разметка экрана списка чатов

#### [MODIFY] [activity_chat_list.xml](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/res/layout/activity_chat_list.xml)
- Заменить `android:background` на светло-серый (`#F7F8FA`) — это поможет белым карточкам чатов не сливаться с фоном.
- Изменить цвет текста `tvPullIndicator` на темно-синий.
- Настроить цвета в `inputLayoutSearch` и `etSearch` для светлой темы (черный текст, серый хинт).
- Заменить фон островка `islandBlurBackground` на `@drawable/bg_island_light`.

### 4. Код активности

#### [MODIFY] [ChatListActivity.kt](file:///C:/Users/going/StudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatListActivity.kt)
- Обновить настройки статус-бара: установить `isAppearanceLightStatusBars = true`, чтобы иконки стали темными.

## План проверки

### Автоматические тесты
- Запуск `./gradlew :app:compileDebugKotlin`.

### Ручная проверка
- Проверить, что на светло-сером фоне белые карточки чатов выглядят отчетливо.
- Убедиться, что текст сообщений и имен легко читается.
- Проверить видимость островка управления и работоспособность поиска в светлой теме.

---
> [!IMPORTANT]
> Использование фона `#F7F8FA` вместо чисто белого критически важно для того, чтобы элементы с белым фоном (карточки) имели "объем" и не сливались в единое полотно.
