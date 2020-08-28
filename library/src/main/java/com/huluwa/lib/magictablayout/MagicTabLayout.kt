package com.huluwa.lib.magictablayout

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View


class MagicTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

    private var bgColor: Int = 0xFFFFFF
    private var normalTextColor: Int = 0x666666
    private var selectTextColor: Int = 0xFFFFFF

    private val paint: Paint by lazy {
        Paint().apply {
            textSize = 14.sp.toFloat()
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

    // bottom space
    private var bottomSpace = 0

    // gap
    private var gapSize = 2.dp

    // radius
    private var topRadius = 0
    private var bottomRadius = 0

    // titles
    private val titles = arrayListOf<Title>()
    private var selectedIndex = 0
    private var normalTitleWidth = 0f // unselected item width
    private var selectTitleWidth = 0f // selected item width
    private var bgHeight = 0f // background height
    private var lineLength = 0f // line length of selected item
    private var normalTextSize = 0
    private var selectedTextSize = 0

    // icon of title
    private var titleIconBitmap: Bitmap? = null
    private var titleScale = 1.0f // scale of title
    private var titleIconPadding = 0

    // Animation
    private var xOffsetAnimator: ValueAnimator? = null
    private var animateSelected = false
    private var selectedTextScaleAnimator: ValueAnimator? = null
    var translateAnimDuration = 300L
    var scaleAnimDuration = 500L

    // Touch
    private var detector: GestureDetector? = null

    // callback
    var onSelectChangeListener: ((index: Int) -> Unit)? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MagicTabLayout)

        bgColor = ta.getColor(R.styleable.MagicTabLayout_bgColor, Color.WHITE)
        selectBitmap = ta.getDrawable(R.styleable.MagicTabLayout_selectDrawable)?.toBitmap()
        titleIconBitmap = ta.getDrawable(R.styleable.MagicTabLayout_titleIconDrawable)?.toBitmap()
        normalTextColor = ta.getColor(R.styleable.MagicTabLayout_normalTextColor, Color.GRAY)
        selectTextColor = ta.getColor(R.styleable.MagicTabLayout_selectTextColor, Color.WHITE)
        animateSelected = ta.getBoolean(R.styleable.MagicTabLayout_animateSelected, false)
        titleIconPadding =
            ta.getDimensionPixelSize(R.styleable.MagicTabLayout_titleIconPadding, 2.dp)
        normalTextSize = ta.getDimensionPixelSize(R.styleable.MagicTabLayout_normalTextSize, 14.sp)
        selectedTextSize =
            ta.getDimensionPixelSize(R.styleable.MagicTabLayout_selectedTextSize, 14.sp)
        gapSize = ta.getDimensionPixelSize(R.styleable.MagicTabLayout_gapSize, 2.dp)
        topRadius = ta.getDimensionPixelSize(R.styleable.MagicTabLayout_topRadius, 5.dp)
        bottomRadius = ta.getDimensionPixelSize(R.styleable.MagicTabLayout_bottomRadius, 10.dp)
        bottomSpace = ta.getDimensionPixelSize(R.styleable.MagicTabLayout_bottomSpace, 6.dp)

        ta.recycle()

        detector = GestureDetector(context, this)
    }

    /**
     * select expected index
     */
    fun select(index: Int) {
        if (index > titles.size - 1 || index == selectedIndex) return
        if (index < titles.size) {
            selectedIndex = index
        }
        xOffsetAnimator?.cancel()
        xOffsetAnimator = ValueAnimator.ofFloat(targetXOffset, normalTitleWidth * index).apply {
            duration = translateAnimDuration
            addUpdateListener {
                targetXOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
        if (animateSelected) {
            selectedTextScaleAnimator?.cancel()
            titleScale = 0f
            selectedTextScaleAnimator = ValueAnimator.ofFloat(titleScale, 0f, 1f).apply {
                duration = scaleAnimDuration
                addUpdateListener {
                    titleScale = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
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
                    40f,
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

        val validWidth = measuredWidth.toFloat() - paddingStart - paddingEnd
        bgHeight = measuredHeight.toFloat() - bottomSpace
        lineLength = (validWidth - bgHeight - measuredHeight) / titles.size
        selectTitleWidth = topRadius * 2 + bottomRadius * 2 + lineLength
        normalTitleWidth = (validWidth - selectTitleWidth) / (titles.size - 1)

        val layerId =
            canvas.saveLayerCompat(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), null)
        // draw background
        paint.color = bgColor
        canvas.drawRect(0f, 0f, canvasWidth.toFloat(), bgHeight, paint)
        if (titles.size == 0) return
        // draw holo part
        paint.xfermode = porterDuffXfermode

        calculateHolePath(bgHeight, lineLength)
        paint.color = Color.YELLOW
        canvas.drawPath(targetPath, paint)
        paint.xfermode = null
        canvas.restoreToCount(layerId)

        drawSelected(canvas, bgHeight.toInt(), (topRadius * 2 + lineLength).toInt(), canvasHeight)

        drawTitles(canvas)
    }

    /**
     * calculate the path of the holo part
     */
    private fun calculateHolePath(bgHeight: Float, lineLength: Float) {
        targetPath.reset()
        targetPath.moveTo(targetXOffset, bgHeight)
        val startX = targetXOffset + paddingStart
        var rectFTo =
            RectF(
                startX - bottomRadius,
                bgHeight - bottomRadius * 2,
                startX + bottomRadius,
                bgHeight
            )
        targetPath.arcTo(rectFTo, 90f, -90f)
        rectFTo = RectF(
            startX + bottomRadius,
            0f,
            startX + bottomRadius + topRadius * 2,
            topRadius * 2f
        )
        targetPath.arcTo(rectFTo, 180f, 90f)
        targetPath.lineTo(startX + topRadius + bottomRadius + lineLength, 0f)
        rectFTo = RectF(
            startX + bottomRadius + lineLength,
            0f,
            startX + bottomRadius + lineLength + topRadius * 2,
            topRadius * 2f
        )
        targetPath.arcTo(rectFTo, 270f, 90f)
        rectFTo = RectF(
            startX + bottomRadius + lineLength + topRadius * 2,
            bgHeight - bottomRadius * 2,
            startX + bottomRadius * 3 + lineLength + topRadius * 2,
            bgHeight
        )
        targetPath.arcTo(rectFTo, 180f, -90f)
        targetPath.close()
    }

    // draw image of the selected part
    private fun drawSelected(canvas: Canvas, pathHeight: Int, outWidth: Int, outHeight: Int) {
        selectBitmap?.let {
            if (roundedBitmap == null) {
                roundedBitmap = roundBottomBitmapByShader(
                    it,
                    outWidth - gapSize * 2,
                    outHeight - gapSize,
                    topRadius - gapSize
                )
            }
            roundedBitmap?.run {
                val startX = targetXOffset + paddingStart
                canvas.drawBitmap(
                    this,
                    null,
                    RectF(
                        startX + bottomRadius + gapSize,
                        gapSize.toFloat(),
                        startX + bottomRadius + outWidth - gapSize,
                        outHeight.toFloat()
                    ),
                    Paint()
                )
            }
        }
    }

    /**
     * get a round corner image
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

        // create a new bitmap
        val targetBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(targetBitmap)
        canvas.drawARGB(0, 0, 0, 0)

        val paint = Paint()
        paint.isAntiAlias = true

        val rectF = RectF(0f, 0f, outWidth.toFloat(), outHeight.toFloat())

        // draw round corner
        canvas.drawRoundRect(rectF, radius.toFloat(), radius.toFloat(), paint)

        // set overlay mode
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        // draw bitmap on canvas
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
     * draw titles
     */
    private fun drawTitles(canvas: Canvas) {
        if (titles.isEmpty()) return
        val normalTitleWidth =
            (canvas.width - paddingStart - paddingEnd - selectTitleWidth).toInt() / (titles.size - 1)
        for (i in 0 until titles.size) {
            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = normalTextSize.toFloat()

            val fontMetrics: Paint.FontMetrics = paint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离
            val bottom = fontMetrics.bottom //为基线到字体下边框的距离
            when {
                i < selectedIndex -> {
                    paint.color = normalTextColor
                    val rect =
                        Rect(
                            paddingStart + normalTitleWidth * i,
                            0,
                            paddingStart + normalTitleWidth * (i + 1),
                            canvas.height
                        )
                    val baseLineY = rect.centerY() - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    canvas.drawText(titles[i].title, rect.centerX().toFloat(), baseLineY, paint)
                }
                i > selectedIndex -> {
                    paint.color = normalTextColor
                    val rect = Rect(
                        paddingStart + normalTitleWidth * (i - 1) + selectTitleWidth.toInt(),
                        0,
                        paddingStart + normalTitleWidth * i + selectTitleWidth.toInt(),
                        canvas.height
                    )
                    val baseLineY = rect.centerY() - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    canvas.drawText(titles[i].title, rect.centerX().toFloat(), baseLineY, paint)
                }
                else -> {
                    // draw selected text
                    paint.textSize = selectedTextSize.toFloat()
                    val rect = RectF(
                        paddingStart + normalTitleWidth * i.toFloat(),
                        gapSize.toFloat(),
                        paddingStart + normalTitleWidth * i + selectTitleWidth,
                        canvas.height.toFloat()
                    )
                    canvas.saveLayerCompat(rect, paint)
                    canvas.scale(titleScale, titleScale, rect.centerX(), rect.centerY())
                    paint.color = selectTextColor
                    val baseLineY =
                        (canvas.height + gapSize) / 2 - top / 2 - bottom / 2 //基线中间点的y轴计算公式
                    var bitmapWidth = 0
                    paint.textAlign = Paint.Align.LEFT
                    val bounds = Rect()
                    paint.getTextBounds(titles[i].fullTitle, 0, titles[i].fullTitle.length, bounds)
                    titleIconBitmap?.let {
                        bitmapWidth = it.width
                    }
                    val widthOfIconAndText = bitmapWidth + titleIconPadding + bounds.width()
                    titleIconBitmap?.let {
                        canvas.drawBitmap(
                            it,
                            rect.centerX() - widthOfIconAndText / 2,
                            rect.centerY() - it.height / 2f,
                            paint
                        )
                    }
                    canvas.drawText(
                        titles[i].fullTitle,
                        rect.centerX() - widthOfIconAndText / 2f + bitmapWidth + titleIconPadding,
                        baseLineY,
                        paint
                    )
                    canvas.restore()
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

    private fun Canvas.saveLayerCompat(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        paint: Paint?
    ): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            saveLayer(left, top, right, bottom, paint)
        } else {
            saveLayer(
                left,
                top,
                right,
                bottom,
                paint,
                Canvas.ALL_SAVE_FLAG
            )
        }
    }

    private fun Canvas.saveLayerCompat(bounds: RectF?, paint: Paint?): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            saveLayer(bounds, paint)
        } else {
            saveLayer(bounds, paint, Canvas.ALL_SAVE_FLAG)
        }
    }

    private fun Drawable.toBitmap(
        width: Int = intrinsicWidth,
        height: Int = intrinsicHeight,
        config: Bitmap.Config? = null
    ): Bitmap {
        if (this is BitmapDrawable) {
            if (config == null || bitmap.config == config) {
                // Fast-path to return original. Bitmap.createScaledBitmap will do this check, but it
                // involves allocation and two jumps into native code so we perform the check ourselves.
                if (width == intrinsicWidth && height == intrinsicHeight) {
                    return bitmap
                }
                return Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
        }

        // 创建bitmap
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            if (!isOpaque) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        setBounds(0, 0, width, height)
        // 将drawable 内容画到画布中
        draw(Canvas(bitmap))
        setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        return bitmap
    }
}

/**
 * class for each title
 */
data class Title(val title: String, val fullTitle: String, val subTitle: String = "")