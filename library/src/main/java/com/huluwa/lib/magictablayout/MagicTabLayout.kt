package com.huluwa.lib.magictablayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GestureDetectorCompat

class MagicTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

    private var bgColor: Int = 0xFFFFFF
    private var normalTextColor: Int = 0x666666
    private var selectTextColor: Int = 0xFFFFFF

    private val paint: Paint by lazy {
        Paint().apply {
            textSize = 14.dp.toFloat()
        }
    }
    private val porterDuffXfermode: PorterDuffXfermode by lazy {
        PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    // path of the holo part
    private val targetPath: Path by lazy {
        Path()
    }
    // start x of the selected tab
    private var targetXOffset = 0f
    // bitmap of the selected tab
    private var selectBitmap: Bitmap? = null
    private var roundedBitmap: Bitmap? = null

    // 标题
    private val titles = arrayListOf<Title>()
    private var selectedIndex = 0
    private var normalTitleWidth = 0f // 未选中标题所占宽度
    private var selectTitleWidth = 0f // 选中标题所占宽度
    private var bgHeight = 0f // 背景非透明部分的高度
    private var lineLength = 0f // 凹槽横线部分的长度

    // 动画
    private var xOffsetAnimator: ValueAnimator? = null

    // 触摸
    private var detector: GestureDetectorCompat? = null

    // 回调
    var onSelectChangeListener: ((index: Int) -> Unit)? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MagicTabLayout)

        bgColor = ta.getColor(R.styleable.MagicTabLayout_bgColor, Color.WHITE)
        selectBitmap = ta.getDrawable(R.styleable.MagicTabLayout_selectDrawable)?.toBitmap()
        normalTextColor = ta.getColor(R.styleable.MagicTabLayout_normalTextColor, Color.GRAY)
        selectTextColor = ta.getColor(R.styleable.MagicTabLayout_selectTextColor, Color.WHITE)

        ta.recycle()

        detector = GestureDetectorCompat(context, this)
    }

    /**
     * 选中指定index
     */
    fun select(index: Int) {
        if (index > titles.size - 1 || index == selectedIndex) return
        if (index < titles.size) {
            selectedIndex = index
        }
        xOffsetAnimator?.cancel()
        xOffsetAnimator = ValueAnimator.ofFloat(targetXOffset, normalTitleWidth * index).apply {
            duration = 300
            addUpdateListener {
                targetXOffset = it.animatedValue as Float
                invalidate()
            }
        }
        xOffsetAnimator?.start()
        onSelectChangeListener?.invoke(index)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val height: Int = when (heightMode) {
            MeasureSpec.EXACTLY -> {
                heightSize
            }
            MeasureSpec.AT_MOST -> {
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    50f,
                    Resources.getSystem().displayMetrics
                ).toInt()
            }
            else -> heightSize
        }
        setMeasuredDimension(widthSize, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val canvasWidth = width
        val canvasHeight = height

        bgHeight = measuredHeight.toFloat() - 8.dp
        lineLength = (measuredWidth.toFloat() - bgHeight - measuredHeight) / titles.size
        selectTitleWidth = bgHeight + measuredHeight + lineLength
        normalTitleWidth = (measuredWidth - selectTitleWidth) / (titles.size - 1)

        val layerId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.saveLayer(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), null)
        } else {
            canvas.saveLayer(
                0f,
                0f,
                canvasWidth.toFloat(),
                canvasHeight.toFloat(),
                null,
                Canvas.ALL_SAVE_FLAG
            )
        }
        //绘制背景
        paint.color = bgColor
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), bgHeight, paint)
        if (titles.size == 0) return
        // 绘制剪切区域
        paint.xfermode = porterDuffXfermode

        calculateHolePath(canvasHeight.toFloat(), bgHeight, lineLength)
        paint.color = Color.YELLOW
        canvas.drawPath(targetPath, paint)
        paint.xfermode = null
        canvas.restoreToCount(layerId)

        drawSelected(canvas, bgHeight.toInt(), (canvasHeight + lineLength).toInt(), canvasHeight)

        drawTitles(canvas, bgHeight)
    }

    /**
     * 挖空区域的path
     */
    private fun calculateHolePath(canvasHeight: Float, pathHeight: Float, lineLength: Float) {
        targetPath.reset()
        targetPath.moveTo(targetXOffset, pathHeight)
        var rectFTo =
            RectF(targetXOffset - pathHeight / 2, 0f, targetXOffset + pathHeight / 2, pathHeight)
        targetPath.arcTo(rectFTo, 90f, -90f)
        rectFTo = RectF(
            targetXOffset + pathHeight / 2,
            0f,
            targetXOffset + pathHeight / 2 + canvasHeight,
            canvasHeight
        )
        targetPath.arcTo(rectFTo, 180f, 90f)
        targetPath.lineTo(targetXOffset + pathHeight + lineLength, 0f)
        rectFTo = RectF(
            targetXOffset + pathHeight / 2 + lineLength,
            0f,
            targetXOffset + pathHeight / 2 + lineLength + canvasHeight,
            canvasHeight
        )
        targetPath.arcTo(rectFTo, 270f, 90f)
        rectFTo = RectF(
            targetXOffset + pathHeight / 2 + canvasHeight + lineLength,
            0f,
            targetXOffset + pathHeight / 2 + canvasHeight + lineLength + pathHeight,
            pathHeight
        )
        targetPath.arcTo(rectFTo, 180f, -90f)
        targetPath.close()
    }

    // 绘制选中区域的图片
    private fun drawSelected(canvas: Canvas, pathHeight: Int, outWidth: Int, outHeight: Int) {
        selectBitmap?.let {
            val margin = 2.dp
            if (roundedBitmap == null) {
                roundedBitmap = roundBottomBitmapByShader(
                    it,
                    outWidth - margin * 2,
                    outHeight - margin,
                    (outHeight - margin) / 2
                )
            }
            roundedBitmap?.run {
                canvas.drawBitmap(
                    this,
                    null,
                    RectF(
                        targetXOffset + pathHeight / 2 + margin,
                        margin.toFloat(),
                        targetXOffset + pathHeight / 2 + outWidth - margin,
                        outHeight.toFloat()
                    ),
                    Paint()
                )
            }
        }
    }

    /**
     * 利用BitmapShader绘制圆角图片
     *
     * @param bitmap
     * 待处理图片
     * @param outWidth
     * 结果图片宽度，一般为控件的宽度
     * @param outHeight
     * 结果图片高度，一般为控件的高度
     * @param radius
     * 圆角半径大小
     * @return
     * 结果图片
     */
    private fun roundBottomBitmapByShader(
        bitmap: Bitmap?,
        outWidth: Int,
        outHeight: Int,
        radius: Int
    ): Bitmap {
        if (bitmap == null) {
            throw NullPointerException("Bitmap can't be null")
        }
        // 等比例缩放拉伸
        val widthScale: Float = outWidth.toFloat() / bitmap.width
        val heightScale: Float = outHeight.toFloat() / bitmap.height
        val matrix = Matrix()
        matrix.setScale(widthScale, heightScale)
        val newBt: Bitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // 初始化目标bitmap
        val targetBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(targetBitmap)
        canvas.drawARGB(0, 0, 0, 0)

        val paint = Paint()
        paint.isAntiAlias = true

        val rectF = RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat())

        // 在画布上绘制圆角图
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        // 设置叠加模式
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // 在画布上绘制原图片
        val ret = Rect(0, 0, outWidth, outHeight)
        canvas.drawBitmap(newBt, ret, ret, paint)
        paint.xfermode = null
        return targetBitmap
    }

    fun setTitles(titles: List<Title>) {
        this.titles.clear()
        this.titles.addAll(titles)
        invalidate()
    }

    /**
     * 绘制标题文字
     */
    private fun drawTitles(canvas: Canvas, bgHeight: Float) {
        if (titles.isEmpty()) return
        val normalTitleWidth = (canvas.width - selectTitleWidth).toInt() / (titles.size - 1)
        for (i in 0 until titles.size) {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 14.sp.toFloat()

            val fontMetrics: Paint.FontMetrics = paint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离,即上图中的top
            val bottom = fontMetrics.bottom //为基线到字体下边框的距离,即上图中的bottom
            when {
                i < selectedIndex -> {
                    paint.color = normalTextColor
                    val rect =
                        Rect(normalTitleWidth * i, 0, normalTitleWidth * (i + 1), canvas.height)
                    val baseLineY = rect.centerY() - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    canvas.drawText(titles[i].title, rect.centerX().toFloat(), baseLineY, paint)
                }
                i > selectedIndex -> {
                    paint.color = normalTextColor
                    val rect = Rect(
                        normalTitleWidth * (i - 1) + selectTitleWidth.toInt(),
                        0,
                        normalTitleWidth * i + selectTitleWidth.toInt(),
                        canvas.height
                    )
                    val baseLineY = rect.centerY() - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    canvas.drawText(titles[i].title, rect.centerX().toFloat(), baseLineY, paint)
                }
                else -> {
                    paint.color = selectTextColor
                    val rect = Rect(
                        normalTitleWidth * i,
                        0,
                        normalTitleWidth * i + selectTitleWidth.toInt(),
                        canvas.height
                    )
                    val baseLineY = rect.centerY() - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    canvas.drawText(titles[i].fullTitle, rect.centerX().toFloat(), baseLineY, paint)
                }
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return detector?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent) {
    }

    /**
     * click event, check which index is clicked
     */
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val x = e.x
        val selectStartX = selectedIndex * normalTitleWidth
        when {
            x < selectStartX -> select((x / normalTitleWidth).toInt())
            x > selectStartX + selectTitleWidth -> select(((x - selectTitleWidth) / normalTitleWidth + 1).toInt())
        }
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
    }
}

/**
 * class for each title
 */
data class Title(val title: String, val fullTitle: String, val subTitle: String = "")