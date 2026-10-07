package com.messenger.prime

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Модель презентационного слайда возможности Prime Messenger,
 * полностью соответствующая надписям на экране HiActivity.
 */
data class MessengerFeature(
    val id: Int,
    val sloganPrefix: String,
    val sloganButton: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null,
    val accentColor: Color
)

/**
 * Список 5 ключевых возможностей Prime Messenger, где в первом разделе "Что такое Prime"
 * отображается официальный векторный XML логотип Prime Messenger, окрашенный в фирменный цвет.
 */
val primeMessengerFeatures = listOf(
    MessengerFeature(
        id = 1,
        sloganPrefix = "Всегда будь в",
        sloganButton = "Прайме!",
        title = "Что такое Prime Messenger?",
        subtitle = "Мессенджер нового поколения",
        description = "Приватный и свободный мессенджер для мгновенного общения онлайн и без интернета по Bluetooth P2P.",
        drawableRes = R.drawable.ic_prime_logo_vector,
        accentColor = Color(0xFF38BDF8)
    ),
    MessengerFeature(
        id = 2,
        sloganPrefix = "Всегда сообщение",
        sloganButton = "Быстрее!",
        title = "Всегда сообщение Быстрее!",
        subtitle = "Мгновенная доставка & Скорость",
        description = "Мгновенная отправка сообщений, реакций и медиафайлов с плавным откликом интерфейса.",
        icon = Icons.Default.Lock,
        accentColor = Color(0xFF22C55E)
    ),
    MessengerFeature(
        id = 3,
        sloganPrefix = "Всегда будь на",
        sloganButton = "Связи!",
        title = "Всегда будь на Связи!",
        subtitle = "Связь без интернета, Контакты & Поиск",
        description = "Общайся без интернета по Bluetooth P2P или онлайн, используй глобальный поиск и историю аватаров.",
        icon = Icons.Default.Search,
        accentColor = Color(0xFFEAB308)
    ),
    MessengerFeature(
        id = 4,
        sloganPrefix = "Всегда будь в",
        sloganButton = "Приватности!",
        title = "Всегда будь в Приватности!",
        subtitle = "Анонимность & Шифрование",
        description = "Защищенная локальная база данных, анонимизация устройств и защита от посторонних.",
        icon = Icons.Default.Lock,
        accentColor = Color(0xFFEC4899)
    ),
    MessengerFeature(
        id = 5,
        sloganPrefix = "Всегда мы",
        sloganButton = "Кастомнее!",
        title = "Всегда мы Кастомнее!",
        subtitle = "Лавовый фон & Персонализация",
        description = "Живой лавовый фон (Lava BG), Material 3 акцентные цвета, медиаплеер со спектром и фоторедактор.",
        icon = Icons.Default.Edit,
        accentColor = Color(0xFFA855F7)
    )
)

/**
 * Бесшовный промо-блок с покрашенным векторным логотипом Prime в 1-м разделе.
 */
@Composable
fun PrimeFeaturePromoCard(
    modifier: Modifier = Modifier,
    onSlideChanged: (prefix: String, button: String) -> Unit = { _, _ -> }
) {
    var currentSlideIndex by remember { mutableIntStateOf(0) }
    var isPressed by remember { mutableStateOf(false) }

    // Переключение слайдов с синхронизацией родительского экрана
    LaunchedEffect(currentSlideIndex) {
        val current = primeMessengerFeatures[currentSlideIndex]
        onSlideChanged(current.sloganPrefix, current.sloganButton)
    }

    // Бесконечный цикличный проигрыватель с остановкой при зажатии
    LaunchedEffect(isPressed, currentSlideIndex) {
        if (isPressed) return@LaunchedEffect
        delay(3800)
        currentSlideIndex = (currentSlideIndex + 1) % primeMessengerFeatures.size
    }

    val currentFeature = primeMessengerFeatures[currentSlideIndex]

    // Стабильный контейнер
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Верхний отцентрованный бэдж текущей подкатегории
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentFeature.accentColor.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(currentFeature.accentColor)
                        )
                        Text(
                            text = currentFeature.subtitle,
                            color = currentFeature.accentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Основной анимированный слайд
            AnimatedContent(
                targetState = currentFeature,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) + slideInHorizontally { width -> width / 3 } togetherWith
                            fadeOut(animationSpec = tween(400)) + slideOutHorizontally { width -> -width / 3 }
                },
                label = "SeamlessPromoLoop"
            ) { feature ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Значок функции или векторный логотип Prime с окрашиванием в акцентный цвет
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = feature.accentColor.copy(alpha = 0.25f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (feature.drawableRes != null) {
                                Icon(
                                    painter = painterResource(id = feature.drawableRes),
                                    contentDescription = feature.title,
                                    tint = feature.accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (feature.icon != null) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = feature.title,
                                    tint = feature.accentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = feature.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = feature.description,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Клики по сегментам прогресс-бара для быстрого выбора слайда
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                primeMessengerFeatures.forEachIndexed { index, _ ->
                    val isSelected = index == currentSlideIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clickable { currentSlideIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isSelected) 4.dp else 3.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) currentFeature.accentColor else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Полноэкранная презентация
 */
@Composable
fun HiPromoScreen(
    onFinished: () -> Unit = {}
) {
    val darkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9))
    ) {
        AnimatedBackground(darkTheme = darkTheme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.statusBarsPadding())

            PrimeFeaturePromoCard()

            Button(
                onClick = onFinished,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp)
                    .navigationBarsPadding()
            ) {
                Text(text = "Продолжить", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
