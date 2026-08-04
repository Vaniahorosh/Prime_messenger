# План по унификации длительности анимаций (600мс)

Этот план направлен на установку единого времени выполнения всех анимаций в проекте равным 600мс для обеспечения плавности и единообразия интерфейса.

## Предлагаемые изменения

### [app]

#### XML Ресурсы анимаций (res/anim)
Во всех XML файлах анимаций значение `android:duration` будет изменено на "600".
- `slide_in_left.xml`
- `slide_in_right.xml`
- `slide_in_top.xml`
- `slide_in_top_alpha.xml`
- `slide_out_bottom.xml`
- `slide_out_left.xml`
- `slide_out_right.xml`
- `slide_up_in.xml`
- `slide_up_out.xml`
- `stay.xml`

#### Kotlin Классы
Во всех вызовах `.setDuration()` и присвоениях `duration = ...` значение будет изменено на 600 (или 600L).
- [ChatListActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ChatListActivity.kt)
- [LoginActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/LoginActivity.kt)
- [PhotoEditorActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/PhotoEditorActivity.kt)
- [PhotoViewActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/PhotoViewActivity.kt)
- [PrimeNotification.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/PrimeNotification.kt)
- [RegisterActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/RegisterActivity.kt)
- [SettingsActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/SettingsActivity.kt)
- [HiActivity.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/HiActivity.kt)
- [ViewExtensions.kt](file:///C:/Users/going/AndroidStudioProjects/Prime_messenger/app/src/main/java/com/messenger/prime/ViewExtensions.kt)

## План проверки
1. Сборка проекта.
2. Визуальная проверка различных переходов и анимаций на предмет плавности и корректности времени выполнения.
3. Проверка того, что анимации не накладываются друг на друга из-за увеличенного времени.
