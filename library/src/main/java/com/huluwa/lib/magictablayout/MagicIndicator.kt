package com.huluwa.lib.magictablayout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class MagicIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var normalColor = 0 // 未选中颜色
    private var cursorColor = 0// 选中颜色
    private var gapSize = 0 // 间距
    private var normalWidth = 0 // 未选中宽度
    private var cursorWidth = 0 // 游标宽度
    private var itemHeight = 0 // 高度

    var count = 0 // 有多少项
        set(value) {
            field = value
            normalItemsWidth = count * normalWidth + (count - 1) * gapSize
            post {
                normalLeft = canvasWidth.toFloat() / 2 - normalItemsWidth / 2
            }
            postInvalidate()
        }

    // scroll
    private var position = 0
    private var positionOffset = 0f

    private val canvasWidth: Int by lazy { width }

    // 未选中item总宽度 = 未选中宽度*个数 + 间距 * 间距个数
    private var normalItemsWidth = 0

    // 未选中最左边的位置
    private var normalLeft = 0f

    private val paint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MagicIndicator)

        normalColor =
            ta.getColor(R.styleable.MagicIndicator_normalColor, Color.argb(50, 255, 255, 255))
        cursorColor = ta.getColor(R.styleable.MagicIndicator_cursorColor, Color.WHITE)
        gapSize = ta.getDimensionPixelSize(R.styleable.MagicIndicator_gapSize, 8.dp)
        normalWidth = ta.getDimensionPixelSize(R.styleable.MagicIndicator_normalWidth, 20.dp)
        cursorWidth = ta.getDimensionPixelSize(R.styleable.MagicIndicator_cursorWidth, 26.dp)
        itemHeight = ta.getDimensionPixelSize(R.styleable.MagicIndicator_itemHeight, 5.dp)

        ta.recycle()

        normalItemsWidth = count * normalWidth + (count - 1) * gapSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> itemHeight
            else -> heightSize
        }
        setMeasuredDimension(widthSize, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        normalLeft = canvasWidth.toFloat() / 2 - normalItemsWidth / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawNormalItems(canvas)
        drawCursor(canvas)
    }

    private fun drawNormalItems(canvas: Canvas) {
        paint.color = normalColor
        val radius = itemHeight.toFloat() / 2
        for (i in 0 until count) {
            val left = normalLeft + normalWidth * i + gapSize * i
            canvas.drawRoundRect(
                RectF(left, 0f, left + normalWidth.toFloat(), itemHeight.toFloat()),
                radius,
                radius,
                paint
            )
        }
    }

    private fun drawCursor(canvas: Canvas) {
        paint.color = cursorColor

        val left: Float
        val right: Float

        val result = calculateCurrentAndNextLeftRight()
        if (positionOffset <= 0.5) {
            if (position >= count - 1) {
                left =
                    result.currentLeft - positionOffset / 0.5f * (result.currentLeft - result.nextLeft)
                right = result.currentRight
            } else {
                left = result.currentLeft
                right =
                    result.currentRight + positionOffset / 0.5f * (result.nextRight - result.currentRight)
            }
        } else {
            if (position >= count - 1) {
                left = result.nextLeft
                right =
                    result.currentRight - (positionOffset - 0.5f) / 0.5f * (result.currentRight - result.nextRight)
            } else {
                left =
                    result.currentLeft + (positionOffset - 0.5f) / 0.5f * (result.nextLeft - result.currentLeft)
                right = result.nextRight
            }
        }

        val radius = itemHeight.toFloat() / 2
        canvas.drawRoundRect(
            RectF(left, 0f, right, itemHeight.toFloat()),
            radius,
            radius,
            paint
        )
    }

    private fun calculateCurrentAndNextLeftRight(): CurrentAndNextLeftRight {
        val currentLeft =
            normalLeft + normalWidth * position + gapSize * position - (cursorWidth - normalWidth) / 2
        val currentRight = currentLeft + cursorWidth
        val nextLeft: Float
        val nextRight: Float
        nextLeft = if (position >= count - 1) {
            normalLeft - (cursorWidth - normalWidth) / 2
        } else {
            currentLeft + normalWidth + gapSize
        }
        nextRight = nextLeft + cursorWidth

        return CurrentAndNextLeftRight(currentLeft, currentRight, nextLeft, nextRight)
    }

    fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
        Log.d(
            "MagicIndicator",
            "position:$position, positionOffset:$positionOffset, positionOffsetPixels:$positionOffsetPixels"
        )
        this.position = position
        this.positionOffset = positionOffset
        invalidate()
    }

    private data class CurrentAndNextLeftRight(
        val currentLeft: Float,
        val currentRight: Float,
        val nextLeft: Float,
        val nextRight: Float
    )
}