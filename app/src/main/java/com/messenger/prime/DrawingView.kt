package com.messenger.prime

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var drawPath: Path = Path()
    private var drawPaint: Paint = Paint()
    private var canvasBitmap: Bitmap? = null

    private var paintColor = Color.RED
    private var brushSize = 20f
    private var hardness = 1.0f // 0.1 to 1.0
    private var isEraser = false

    // История для Undo
    private val paths = mutableListOf<Path>()
    private val paints = mutableListOf<Paint>()

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.color = paintColor
        drawPaint.isAntiAlias = true
        drawPaint.strokeWidth = brushSize
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        updateHardness()
    }

    private fun updateHardness() {
        if (hardness < 1.0f) {
            val blurRadius = (brushSize / 2f) * (1.0f - hardness)
            if (blurRadius > 0) {
                drawPaint.maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            } else {
                drawPaint.maskFilter = null
            }
        } else {
            drawPaint.maskFilter = null
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (i in paths.indices) {
            canvas.drawPath(paths[i], paints[i])
        }
        canvas.drawPath(drawPath, drawPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                paths.add(Path(drawPath))
                paints.add(Paint(drawPaint))
                drawPath.reset()
                onUndoAvailableListener?.invoke(true)
                performClick()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1)
            paints.removeAt(paints.size - 1)
            invalidate()
            onUndoAvailableListener?.invoke(paths.isNotEmpty())
        }
    }

    fun setBrushColor(newColor: Int) {
        paintColor = newColor
        drawPaint.color = paintColor
        isEraser = false
        drawPaint.xfermode = null
        updateHardness()
    }

    fun setEraser(eraser: Boolean) {
        isEraser = eraser
        if (isEraser) {
            drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            drawPaint.xfermode = null
        }
        updateHardness()
    }

    fun setBrushSize(newSize: Float) {
        brushSize = newSize
        drawPaint.strokeWidth = brushSize
        updateHardness()
    }

    fun setHardness(newHardness: Float) {
        hardness = newHardness.coerceIn(0.1f, 1.0f)
        updateHardness()
    }

    private var onUndoAvailableListener: ((Boolean) -> Unit)? = null
    fun setOnUndoAvailableListener(listener: (Boolean) -> Unit) {
        onUndoAvailableListener = listener
    }

    fun getResultBitmap(): Bitmap? {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return null
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        for (i in paths.indices) {
            canvas.drawPath(paths[i], paints[i])
        }
        return result
    }
    
    fun clear() {
        paths.clear()
        paints.clear()
        drawPath.reset()
        invalidate()
        onUndoAvailableListener?.invoke(false)
    }

    fun hasHistory(): Boolean = paths.isNotEmpty()
}
