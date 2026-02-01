package com.example.cocktailbar

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

class TemplatePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class EditMode {
        NONE, BACKGROUND, LOGO, DRINKS
    }

    var editMode: EditMode = EditMode.NONE
        set(value) {
            field = value
            invalidate()
        }

    // Images
    private var backgroundBitmap: Bitmap? = null
    private var logoBitmap: Bitmap? = null

    // Background transform (relative values 0-1, where 0.5 = centered)
    var backgroundScale: Float = 1f
    var backgroundOffsetX: Float = 0.5f  // 0.5 = centered
    var backgroundOffsetY: Float = 0.5f  // 0.5 = centered

    // Logo transform (0-1 relative positions)
    var logoX: Float = 0.5f
    var logoY: Float = 0.1f
    var logoScale: Float = 1f

    // Drinks area (0-1 relative positions)
    var drinksX: Float = 0.1f
    var drinksY: Float = 0.25f
    var drinksWidth: Float = 0.8f
    var drinksHeight: Float = 0.65f
    var drinksFontSize: Float = 16f
    var drinksColumns: Int = 1

    // Drinks data
    var drinks: List<Drink> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    // Paints
    private val drinksAreaPaint = Paint().apply {
        color = Color.parseColor("#33FF6B35")
        style = Paint.Style.FILL
    }

    private val drinksAreaStrokePaint = Paint().apply {
        color = Color.parseColor("#FF6B35")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val logoStrokePaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val drinkNamePaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val drinkPricePaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val handlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val handleStrokePaint = Paint().apply {
        color = Color.parseColor("#FF6B35")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Touch handling
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = -1
    private var isDraggingHandle = false
    private var dragHandleCorner = -1

    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())

    var onLayoutChanged: (() -> Unit)? = null
    var onTapInViewMode: (() -> Unit)? = null

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        backgroundBitmap = bitmap
        invalidate()
    }

    fun setLogoBitmap(bitmap: Bitmap?) {
        logoBitmap = bitmap
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        drawBackground(canvas)

        // Draw drinks area
        drawDrinksArea(canvas)

        // Draw logo
        drawLogo(canvas)

        // Draw edit handles based on mode
        when (editMode) {
            EditMode.LOGO -> drawLogoHandles(canvas)
            EditMode.DRINKS -> drawDrinksHandles(canvas)
            else -> {}
        }
    }

    private fun drawBackground(canvas: Canvas) {
        backgroundBitmap?.let { bitmap ->
            val canvasWidth = width.toFloat()
            val canvasHeight = height.toFloat()

            // Calculate scaled dimensions to cover the canvas
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height
            val canvasAspect = canvasWidth / canvasHeight

            val baseWidth: Float
            val baseHeight: Float

            if (bitmapAspect > canvasAspect) {
                // Bitmap is wider - fit to height
                baseHeight = canvasHeight
                baseWidth = canvasHeight * bitmapAspect
            } else {
                // Bitmap is taller - fit to width
                baseWidth = canvasWidth
                baseHeight = canvasWidth / bitmapAspect
            }

            val scaledWidth = baseWidth * backgroundScale
            val scaledHeight = baseHeight * backgroundScale

            // Calculate offset range (how much we can pan)
            val maxOffsetX = (scaledWidth - canvasWidth) / 2
            val maxOffsetY = (scaledHeight - canvasHeight) / 2

            // Convert relative offset (0-1) to absolute pixels
            // 0.5 = centered, 0 = shifted left/up max, 1 = shifted right/down max
            val absoluteOffsetX = if (maxOffsetX > 0) {
                (backgroundOffsetX - 0.5f) * 2 * maxOffsetX
            } else {
                0f
            }
            val absoluteOffsetY = if (maxOffsetY > 0) {
                (backgroundOffsetY - 0.5f) * 2 * maxOffsetY
            } else {
                0f
            }

            val left = (canvasWidth - scaledWidth) / 2 + absoluteOffsetX
            val top = (canvasHeight - scaledHeight) / 2 + absoluteOffsetY

            val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(bitmap, null, destRect, null)
        }
    }

    private fun drawLogo(canvas: Canvas) {
        logoBitmap?.let { bitmap ->
            val baseLogoWidth = width * 0.3f
            val logoWidth = baseLogoWidth * logoScale
            val logoHeight = (bitmap.height.toFloat() / bitmap.width) * logoWidth

            val left = width * logoX - logoWidth / 2
            val top = height * logoY - logoHeight / 2

            val destRect = RectF(left, top, left + logoWidth, top + logoHeight)
            canvas.drawBitmap(bitmap, null, destRect, null)

            if (editMode == EditMode.LOGO) {
                canvas.drawRect(destRect, logoStrokePaint)
            }
        }
    }

    private fun drawDrinksArea(canvas: Canvas) {
        val left = width * drinksX
        val top = height * drinksY
        val right = left + width * drinksWidth
        val bottom = top + height * drinksHeight

        if (editMode == EditMode.DRINKS) {
            canvas.drawRect(left, top, right, bottom, drinksAreaPaint)
            canvas.drawRect(left, top, right, bottom, drinksAreaStrokePaint)
        }

        // Draw drinks list
        drawDrinksList(canvas, left, top, right, bottom)
    }

    private fun drawDrinksList(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        if (drinks.isEmpty()) return

        val areaWidth = right - left
        val areaHeight = bottom - top
        val columnWidth = areaWidth / drinksColumns

        // Scale font size relative to canvas height for consistency
        val scaledFontSize = drinksFontSize * (height / 800f) * resources.displayMetrics.density
        val padding = 16f * (height / 800f)

        drinkNamePaint.textSize = scaledFontSize
        drinkPricePaint.textSize = scaledFontSize

        val lineHeight = scaledFontSize * 1.5f
        val maxLinesPerColumn = ((areaHeight - padding * 2) / lineHeight).toInt()

        var drinkIndex = 0
        for (col in 0 until drinksColumns) {
            val colLeft = left + col * columnWidth + padding
            val colRight = left + (col + 1) * columnWidth - padding
            var y = top + padding + scaledFontSize

            for (line in 0 until maxLinesPerColumn) {
                if (drinkIndex >= drinks.size) break

                val drink = drinks[drinkIndex]
                canvas.drawText(drink.name, colLeft, y, drinkNamePaint)
                canvas.drawText(drink.getDisplayPrice(), colRight, y, drinkPricePaint)

                y += lineHeight
                drinkIndex++
            }
        }
    }

    private fun drawLogoHandles(canvas: Canvas) {
        logoBitmap?.let { bitmap ->
            val baseLogoWidth = width * 0.3f
            val logoWidth = baseLogoWidth * logoScale
            val logoHeight = (bitmap.height.toFloat() / bitmap.width) * logoWidth

            val left = width * logoX - logoWidth / 2
            val top = height * logoY - logoHeight / 2
            val right = left + logoWidth
            val bottom = top + logoHeight

            drawHandle(canvas, right, bottom) // Bottom-right scale handle
        }
    }

    private fun drawDrinksHandles(canvas: Canvas) {
        val left = width * drinksX
        val top = height * drinksY
        val right = left + width * drinksWidth
        val bottom = top + height * drinksHeight

        // Draw corner handles
        drawHandle(canvas, left, top)      // Top-left
        drawHandle(canvas, right, top)     // Top-right
        drawHandle(canvas, left, bottom)   // Bottom-left
        drawHandle(canvas, right, bottom)  // Bottom-right
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        val radius = 20f
        canvas.drawCircle(x, y, radius, handlePaint)
        canvas.drawCircle(x, y, radius, handleStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // In NONE mode, just detect taps and pass through
        if (editMode == EditMode.NONE) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                onTapInViewMode?.invoke()
            }
            return true
        }

        scaleGestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activePointerId = event.getPointerId(0)

                // Check if touching a handle
                if (editMode == EditMode.DRINKS) {
                    dragHandleCorner = getDrinksHandleAtPosition(event.x, event.y)
                    isDraggingHandle = dragHandleCorner >= 0
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (scaleGestureDetector.isInProgress) return true

                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return true

                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val dx = x - lastTouchX
                val dy = y - lastTouchY

                when (editMode) {
                    EditMode.BACKGROUND -> {
                        // Convert pixel movement to relative offset change
                        backgroundBitmap?.let { bitmap ->
                            val canvasWidth = width.toFloat()
                            val canvasHeight = height.toFloat()

                            val bitmapAspect = bitmap.width.toFloat() / bitmap.height
                            val canvasAspect = canvasWidth / canvasHeight

                            val baseWidth: Float
                            val baseHeight: Float

                            if (bitmapAspect > canvasAspect) {
                                baseHeight = canvasHeight
                                baseWidth = canvasHeight * bitmapAspect
                            } else {
                                baseWidth = canvasWidth
                                baseHeight = canvasWidth / bitmapAspect
                            }

                            val scaledWidth = baseWidth * backgroundScale
                            val scaledHeight = baseHeight * backgroundScale

                            val maxOffsetX = (scaledWidth - canvasWidth) / 2
                            val maxOffsetY = (scaledHeight - canvasHeight) / 2

                            // Convert pixel drag to relative change
                            if (maxOffsetX > 0) {
                                val relativeChangeX = dx / (2 * maxOffsetX)
                                backgroundOffsetX = (backgroundOffsetX + relativeChangeX).coerceIn(0f, 1f)
                            }
                            if (maxOffsetY > 0) {
                                val relativeChangeY = dy / (2 * maxOffsetY)
                                backgroundOffsetY = (backgroundOffsetY + relativeChangeY).coerceIn(0f, 1f)
                            }
                        }
                        onLayoutChanged?.invoke()
                    }
                    EditMode.LOGO -> {
                        logoX = (logoX + dx / width).coerceIn(0.1f, 0.9f)
                        logoY = (logoY + dy / height).coerceIn(0.05f, 0.95f)
                        onLayoutChanged?.invoke()
                    }
                    EditMode.DRINKS -> {
                        if (isDraggingHandle) {
                            handleDrinksResize(dragHandleCorner, x, y)
                        } else {
                            drinksX = (drinksX + dx / width).coerceIn(0f, 1f - drinksWidth)
                            drinksY = (drinksY + dy / height).coerceIn(0f, 1f - drinksHeight)
                        }
                        onLayoutChanged?.invoke()
                    }
                    else -> {}
                }

                lastTouchX = x
                lastTouchY = y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = -1
                isDraggingHandle = false
                dragHandleCorner = -1
            }
        }
        return true
    }

    private fun getDrinksHandleAtPosition(x: Float, y: Float): Int {
        val left = width * drinksX
        val top = height * drinksY
        val right = left + width * drinksWidth
        val bottom = top + height * drinksHeight
        val threshold = 50f

        return when {
            isNearPoint(x, y, left, top, threshold) -> 0     // Top-left
            isNearPoint(x, y, right, top, threshold) -> 1    // Top-right
            isNearPoint(x, y, left, bottom, threshold) -> 2  // Bottom-left
            isNearPoint(x, y, right, bottom, threshold) -> 3 // Bottom-right
            else -> -1
        }
    }

    private fun isNearPoint(x: Float, y: Float, px: Float, py: Float, threshold: Float): Boolean {
        return Math.abs(x - px) < threshold && Math.abs(y - py) < threshold
    }

    private fun handleDrinksResize(corner: Int, x: Float, y: Float) {
        val relX = x / width
        val relY = y / height

        when (corner) {
            0 -> { // Top-left
                val newWidth = (drinksX + drinksWidth) - relX
                val newHeight = (drinksY + drinksHeight) - relY
                if (newWidth > 0.1f && newHeight > 0.1f) {
                    drinksX = relX.coerceIn(0f, 0.9f)
                    drinksY = relY.coerceIn(0f, 0.9f)
                    drinksWidth = newWidth.coerceIn(0.1f, 1f)
                    drinksHeight = newHeight.coerceIn(0.1f, 1f)
                }
            }
            1 -> { // Top-right
                val newWidth = relX - drinksX
                val newHeight = (drinksY + drinksHeight) - relY
                if (newWidth > 0.1f && newHeight > 0.1f) {
                    drinksY = relY.coerceIn(0f, 0.9f)
                    drinksWidth = newWidth.coerceIn(0.1f, 1f - drinksX)
                    drinksHeight = newHeight.coerceIn(0.1f, 1f)
                }
            }
            2 -> { // Bottom-left
                val newWidth = (drinksX + drinksWidth) - relX
                val newHeight = relY - drinksY
                if (newWidth > 0.1f && newHeight > 0.1f) {
                    drinksX = relX.coerceIn(0f, 0.9f)
                    drinksWidth = newWidth.coerceIn(0.1f, 1f)
                    drinksHeight = newHeight.coerceIn(0.1f, 1f - drinksY)
                }
            }
            3 -> { // Bottom-right
                val newWidth = relX - drinksX
                val newHeight = relY - drinksY
                if (newWidth > 0.1f && newHeight > 0.1f) {
                    drinksWidth = newWidth.coerceIn(0.1f, 1f - drinksX)
                    drinksHeight = newHeight.coerceIn(0.1f, 1f - drinksY)
                }
            }
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            when (editMode) {
                EditMode.BACKGROUND -> {
                    backgroundScale *= detector.scaleFactor
                    backgroundScale = backgroundScale.coerceIn(1f, 3f)  // Min 1.0 to always cover
                }
                EditMode.LOGO -> {
                    logoScale *= detector.scaleFactor
                    logoScale = logoScale.coerceIn(0.3f, 3f)
                }
                else -> {}
            }
            onLayoutChanged?.invoke()
            invalidate()
            return true
        }
    }
}