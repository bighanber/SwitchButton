package com.luuu.switchbutton

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator

/**
 * Created by lls on 2017/10/27.
 */
class SwitchButtonView : View {

    companion object {
        val BUTTON_STATE_LEFT = 1
        val BUTTON_STATE_RIGHT = 0
    }

    private var leftText = "left"
    private var rightText = "right"
    private var textSize = 40
    private var selectTxtColor = 0XFF63B931.toInt()
    private var unSelectTxtColor = 0XFFFFFFFF.toInt()
    private lateinit var fontMetrics: Paint.FontMetricsInt
    private var baseline = 0f

    //按钮背景
    private var buttonBackgroundColor = 0XFFFFFFFF.toInt()
    //滑块背景
    private var sliderColor = 0xFF066FA5.toInt()
    private var buttonPaint = Paint()
    private var sliderPaint = Paint()
    private var textPain = Paint()
    private lateinit var buttonRectF: RectF
    private lateinit var textRectF: RectF
    private var curvePath: Path = Path()

    private var mWidth = 0
    private var mHeight = 0
    //动画变量，控制滑块移动
    private var moveAnim = 0f
    //滑块可移动的距离
    private var sliderLength= 0f
    private var circleRadius = 0f
    //右边圆形的移动距离
    private var rightMoveDis = 0f
    //左边圆形的移动距离
    private var leftMoveDis = 0f

    private var isSelect = false
    private var onSelectedChangeListener: OnSelectedChangeListener? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs, defStyleAttr, defStyleRes)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int = 0, defStyleRes: Int = 0) {
        var typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwitchButton, defStyleAttr, defStyleRes)
        leftText = typedArray.getString(R.styleable.SwitchButton_btn_leftText)
        rightText = typedArray.getString(R.styleable.SwitchButton_btn_rightText)
        textSize = typedArray.getDimensionPixelSize(R.styleable.SwitchButton_btn_textSize, textSize)
        buttonBackgroundColor = typedArray.getColor(R.styleable.SwitchButton_btn_buttonBackgroundColor, buttonBackgroundColor)
        sliderColor = typedArray.getColor(R.styleable.SwitchButton_btn_sliderColor, sliderColor)
        selectTxtColor = typedArray.getColor(R.styleable.SwitchButton_btn_selectTxtColor, selectTxtColor)
        unSelectTxtColor = typedArray.getColor(R.styleable.SwitchButton_btn_unSelectTxtColor, unSelectTxtColor)
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mWidth = w
        mHeight = h
        buttonRectF = RectF(0f, 0f, mWidth.toFloat(), mHeight.toFloat())
        textRectF = RectF(0f, 0f, mWidth.toFloat(), mHeight.toFloat())
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        var heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        widthSpecSize += paddingLeft + paddingRight
        heightSpecSize += paddingTop + paddingBottom
        val measuredWidth = resolveSizeAndState(Math.min(widthSpecSize, heightSpecSize), widthMeasureSpec, 0)
        val measuredHeight = resolveSizeAndState(Math.min(widthSpecSize, heightSpecSize), heightMeasureSpec, 0)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        circleRadius = mHeight/2f
        sliderLength = mWidth/2f

        buttonPaint.isAntiAlias = true
        buttonPaint.color = buttonBackgroundColor

        sliderPaint.isAntiAlias = true
        sliderPaint.color = sliderColor

        textPain.strokeWidth = 3f
        textPain.textSize = textSize.toFloat()
        textPain.textAlign = Paint.Align.CENTER
        fontMetrics = textPain.fontMetricsInt

        canvas?.drawRoundRect(buttonRectF, circleRadius, circleRadius, buttonPaint)

        drawSlider(canvas)
        drawLeftText(canvas)
        drawRightText(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (moveAnim == 0f || moveAnim == 1f) {
            isSelect = !isSelect
            startAnimator(isSelect)
            onSelectedChangeListener?.onSelectedChange(if (isSelect) BUTTON_STATE_RIGHT else BUTTON_STATE_LEFT)
        }
        return super.onTouchEvent(event)
    }

    private fun drawSlider(canvas: Canvas?) {
        canvas?.save()
        //右边圆球移动的距离，moveAnim在0-0.5的时候控制右边球的移动
        rightMoveDis = sliderLength - circleRadius + sliderLength * Math.min(1f, moveAnim * 2)
        canvas?.drawCircle(rightMoveDis, circleRadius, circleRadius, sliderPaint)

        //只有右边的球移到底的时候，左边的球才开始移动
        leftMoveDis = circleRadius + sliderLength * (if (moveAnim < 0.5) 0f else Math.abs(0.5f - moveAnim) * 2)
        canvas?.drawCircle(leftMoveDis, circleRadius, circleRadius, sliderPaint)

        //绘制两球的贝塞尔曲线连接
        //控制点会根据左右球移动来增大和减小
        if (moveAnim in 0.0..1.0) {
            curvePath.rewind()
            curvePath.moveTo(leftMoveDis, 0f)
            curvePath.cubicTo(leftMoveDis, 0f,
                    (rightMoveDis + leftMoveDis)/2,
                    if (moveAnim <= 0.5) circleRadius * (moveAnim * 2f) else circleRadius * ((1 - moveAnim) * 2f),
                    rightMoveDis, 0f)
            curvePath.lineTo(rightMoveDis, mHeight.toFloat())
            curvePath.cubicTo(rightMoveDis, mHeight.toFloat(),
                    (rightMoveDis + leftMoveDis)/2,
                    if (moveAnim <= 0.5) 2f * circleRadius * (1 - moveAnim) else 2f * circleRadius * moveAnim,
                    leftMoveDis, mHeight.toFloat())
            curvePath.close()
            canvas?.drawPath(curvePath, sliderPaint)
        }
        canvas?.restore()
    }

    private fun drawLeftText(canvas: Canvas?) {
        canvas?.save()
        textRectF.left = 0f
        textRectF.right = mWidth/2f
        canvas?.clipRect(textRectF)

        textPain.color = if (isSelect) if (moveAnim > 0.5) unSelectTxtColor else selectTxtColor else if (moveAnim < 0.5) selectTxtColor else unSelectTxtColor
        baseline = (textRectF.bottom + textRectF.top - fontMetrics.bottom - fontMetrics.top) / 2
        canvas?.drawText(leftText, textRectF.centerX(), baseline, textPain)
        canvas?.restore()
    }

    private fun drawRightText(canvas: Canvas?) {
        canvas?.save()
        textRectF.left = mWidth/2f
        textRectF.right = mWidth.toFloat()
        canvas?.clipRect(textRectF)

        textPain.color = if (isSelect) if (moveAnim > 0.5) selectTxtColor else unSelectTxtColor else if (moveAnim < 0.5) unSelectTxtColor else selectTxtColor
        baseline = (textRectF.bottom + textRectF.top - fontMetrics.bottom - fontMetrics.top) / 2
        canvas?.drawText(rightText, textRectF.centerX(), baseline, textPain)
        canvas?.restore()
    }

    private fun startAnimator(selectState: Boolean) {
        var anim = ValueAnimator.ofFloat(moveAnim, (if (selectState) 1f else 0f))
        anim.duration = 500
        anim.interpolator = AccelerateInterpolator()
        anim.addUpdateListener{
            moveAnim = it.animatedValue as Float
            invalidate()
        }
        anim.start()
    }

    interface OnSelectedChangeListener {
        fun onSelectedChange(state: Int)
    }

    fun setOnSelectedChangeListener(listener: OnSelectedChangeListener) {
        onSelectedChangeListener = listener
    }
}