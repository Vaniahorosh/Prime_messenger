# План синхронизации статуса сети в островках

Этот план направлен на синхронизацию текста подсказки ("ПОИСК" / "Ожидание сети...") и анимации между нижним плавающим островком и верхним статичным островком в списке чатов.

## Предлагаемые изменения

### [app]

#### [MODIFY] [ChatAdapter.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatAdapter.kt)
- Добавить приватную переменную `currentNetworkHint`, которая будет хранить актуальный текст подсказки.
- Обновить `onBindViewHolder` для `HeaderViewHolder`, чтобы устанавливать хинт в `inputLayoutHeaderSearch`.
- Добавить метод `updateNetworkHint(newHint: String)`, который обновляет переменную и вызывает `notifyItemChanged(0)`, если поиск не активен.

#### [MODIFY] [ChatListActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatListActivity.kt)
- Создать общий метод `animateViewHint(view: TextInputLayout, newHint: String)`, содержащий логику анимации "ухода вверх" и "появления снизу".
- Обновить `animateSearchHint(newHint: String)`:
    - Теперь он будет вызывать `animateViewHint` для нижнего островка.
    - Также он будет находить `HeaderViewHolder` в `RecyclerView` (если он виден) и запускать ту же анимацию для его поля поиска.
    - Вызывать `adapter.updateNetworkHint(newHint)` для сохранения состояния.

## План проверки

### Ручная проверка
1.  Открыть список чатов.
2.  Отключить интернет на устройстве.
3.  Убедиться, что **оба** островка (верхний в списке и нижний плавающий) одновременно сменили текст на "Ожидание сети..." с одинаковой анимацией.
4.  Включить интернет.
5.  Убедиться, что текст вернулся на "ПОИСК" также в обоих местах синхронно.
