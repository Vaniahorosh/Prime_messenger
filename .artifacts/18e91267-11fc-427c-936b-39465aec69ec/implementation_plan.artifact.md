# План реализации динамического заголовка и поиска в статичном островке

Этот план описывает обновление статичного островка в списке чатов: добавление иконки поиска и реализацию динамической смены заголовка ("Прайм" / "Имя пользователя") с вертикальной анимацией.

## Предлагаемые изменения

### [app]

#### [NEW] [ic_search.xml](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/res/drawable/ic_search.xml)
- Создание векторной иконки лупы в белом цвете.

#### [MODIFY] [item_chat_island_header.xml](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/res/layout/item_chat_island_header.xml)
- Удаление `TextInputLayout`.
- Добавление `TextSwitcher` (id: `tsHeaderTitle`) по центру между аватаркой и будущей кнопкой поиска.
- Добавление `ImageButton` (id: `btnHeaderSearch`) с правой стороны для вызова строки поиска.

#### [MODIFY] [ChatAdapter.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatAdapter.kt)
- Обновление конструктора: добавление `userName` и коллбэка `onNameClick`.
- В `HeaderViewHolder`:
    - Инициализация `TextSwitcher` (установка фабрики `TextView` и анимаций `slide_in_top_alpha`/`slide_out_bottom`).
    - Реализация циклического переключения текста каждые 5 секунд ("Прайм" <-> имя пользователя).
    - Обработка жизненного цикла (запуск таймера при привязке и остановка при отвязке View от окна).
- Обработка клика по `tsHeaderTitle` для вызова диалога смены имени.

#### [MODIFY] [ChatListActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatListActivity.kt)
- Получение актуального имени пользователя из `SharedPreferences`.
- Передача имени и нового коллбэка `onNameClick` в `ChatAdapter`.
- Реализация метода `showNameEditDialog()` (копия логики из `SettingsActivity`) для редактирования имени прямо из списка чатов.

## План проверки

### Ручная проверка
1.  **Интерфейс**: В статичном островке (вверху списка) справа должна появиться лупа.
2.  **Анимация**: Заголовок должен плавно меняться с "Прайм" на ваше имя каждые 5 секунд (анимация проваливания/вылета по вертикали).
3.  **Поиск**: Нажатие на лупу должно активировать нижнюю панель поиска.
4.  **Смена имени**: Нажатие на заголовок ("Прайм" или Имя) должно открывать диалог редактирования. После сохранения имя в заголовке должно обновиться.
