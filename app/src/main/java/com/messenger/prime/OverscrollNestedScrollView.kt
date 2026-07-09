package com.messenger.prime

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * NestedScrollView, который умеет ловить "второй свайп вниз" —
 * когда контент уже находится в самом верху (scrollY == 0) и
 * дальше скроллить нечего, но пользователь продолжает тянуть вниз.
 *
 * Стандартный AppBarLayout.Behavior это не обрабатывает: он консьюмит
 * скролл только пока есть куда раскрывать шапку. Как только шапка
 * полностью развёрнута — весь дальнейший драг просто "проваливается"
 * и не производит эффекта, поэтому ловим его тут сами.
 *
 * Логика включается только когда [isPullEnabled] вернёт true —
 * снаружи это должно быть true ТОЛЬКО когда AppBarLayout уже полностью
 * expanded (offset == 0). Иначе первый свайп (раскрытие шапки) будет
 * конфликтовать с этим и работать криво.
 */
class OverscrollNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    /** Вызывается непрерывно во время протяжки, dragPx — величина оттяжки с демпфированием. */
    var onPullProgress: ((dragPx: Float) -> Unit)? = null

    /** Вызывается на отпускании пальца. reachedThreshold — дотянул ли пользователь до порога. */
    var onPullReleased: ((reachedThreshold: Boolean) -> Unit)? = null

    /** Порог, после которого палец "долистал" достаточно — открываем fullscreen. */
    var onPullThresholdReached: (() -> Unit)? = null

    /** Внешний колбэк: разрешена ли сейчас логика overscroll (шапка полностью раскрыта). */
    var isPullEnabled: () -> Boolean = { false }

    private val pullThresholdPx = resources.displayMetrics.density * 110f
    private val dampingFactor = 0.5f

    private var startY = 0f
    private var dragging = false
    private var currentDrag = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dragging) return true
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isPullEnabled()) {
            if (dragging) {
                dragging = false
                currentDrag = 0f
                onPullReleased?.invoke(false)
            }
            return super.onTouchEvent(ev)
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y
                dragging = false
                currentDrag = 0f
                super.onTouchEvent(ev)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = ev.y - startY
                val atTop = scrollY == 0

                if (atTop && delta > 0) {
                    dragging = true
                    currentDrag = delta * dampingFactor
                    onPullProgress?.invoke(currentDrag)
                    return true
                } else if (dragging) {
                    // потянул назад / ушли из зоны overscroll — сбрасываем
                    dragging = false
                    currentDrag = 0f
                    onPullReleased?.invoke(false)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val reached = currentDrag >= pullThresholdPx
                    dragging = false
                    val dragSnapshot = currentDrag
                    currentDrag = 0f
                    startY = 0f
                    onPullReleased?.invoke(reached)
                    if (reached) onPullThresholdReached?.invoke()
                    return dragSnapshot > 0
                }
            }
        }
        return super.onTouchEvent(ev)
    }
}