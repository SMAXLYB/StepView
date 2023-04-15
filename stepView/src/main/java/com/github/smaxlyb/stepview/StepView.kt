package com.github.smaxlyb.stepview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
/**
 * @description: 步骤条/流程条, 在xml中使用时, 推荐使用wrap_content设置高度, 如果不合适, 再设置具体值
 */
typealias StepClickListener = (Int, StepModel) -> Unit

class StepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /*================ xml可控变量 =====================*/
    private lateinit var mOrientation: Orientation
    internal lateinit var mDirection: Direction
    internal var mLine2IconPadding: Float = 0f
    internal var mNodePadding: Float = 0f
    private var mLineWidth: Float = DEFAULT_LINE_WIDTH
    internal var mIconRadius: Float = DEFAULT_ICON_RADIUS
    internal var mIcon2TextPadding: Float = DEFAULT_ICON_TEXT_PADDING_V

    /*================ 绘图相关变量 =====================*/
    private lateinit var viewHelper: IStepViewHelper
    internal lateinit var mTitlePaint: Paint
    internal lateinit var mDetailPaint: Paint
    internal lateinit var mLinePaint: Paint
    internal var mTitleColor = ContextCompat.getColor(context, R.color.gray)
    private var mDetailColor = ContextCompat.getColor(context, R.color.gray)
    internal var mDoneColor = ContextCompat.getColor(context, R.color.blue)
    internal var mUndoColor = ContextCompat.getColor(context, R.color.light_white)
    internal var mTotalW = 0f
    internal var mTotalH = 0f
    internal var mRemainH = 0f
    internal var mRemainW = 0f

    /*==================== 数据变量 =====================*/
    internal val mNodes = mutableListOf<StepModel>()
    internal val mSize: Int get() = mNodes.size
    private var mOnStepClickListener: StepClickListener? = null

    /*====================== 常量 ========================*/
    companion object {
        // 竖直时文字和icon的默认间距
        private val DEFAULT_ICON_TEXT_PADDING_V = 10.dp

        // icon默认半径
        private val DEFAULT_ICON_RADIUS = 9.dp

        // 线条默认厚度
        private val DEFAULT_LINE_WIDTH = 2.dp
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.StepView, defStyleAttr, R.style.StepView).use {
            mOrientation = Orientation.getOrientation(it.getInt(R.styleable.StepView_android_orientation, 0))
            mDirection = Direction.getDirection(it.getInt(R.styleable.StepView_step_view_direction, 0))
            adjustDirection()
            mLine2IconPadding = it.getDimension(R.styleable.StepView_step_view_line_2_icon_padding, 0f)
            mNodePadding = it.getDimension(R.styleable.StepView_step_view_node_padding, 0f)
            mLineWidth = it.getDimension(R.styleable.StepView_step_view_line_width, DEFAULT_LINE_WIDTH)
            mIconRadius = it.getDimension(R.styleable.StepView_step_view_radius, DEFAULT_ICON_RADIUS)
        }
        initPaint()
        initHelper()
    }

    private fun initHelper() {
        viewHelper = if (mOrientation == Orientation.Horizontal) {
            HorizontalStepViewHelper(this)
        } else {
            VerticalStepViewHelper(this)
        }

        viewHelper.init()
    }

    private fun initPaint() {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        mTitlePaint = Paint().also {
            it.isDither = true
            it.isAntiAlias = true
            it.textSize = 14.sp
            it.color = mTitleColor
            it.textAlign = Paint.Align.CENTER
        }
        mDetailPaint = Paint().also {
            it.isDither = true
            it.isAntiAlias = true
            it.textSize = 12.sp
            it.color = mDetailColor
            it.textAlign = Paint.Align.CENTER
        }
        mLinePaint = Paint().also {
            it.isDither = true
            it.isAntiAlias = true
            it.strokeWidth = mLineWidth
            it.strokeCap = Paint.Cap.ROUND
            it.color = mUndoColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val pair = viewHelper.getMeasureSpec(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(pair.first, pair.second)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mTotalH = h.toFloat()
        mTotalW = w.toFloat()
        mRemainH = mTotalH - paddingTop - paddingBottom
        mRemainW = mTotalW - paddingLeft - paddingRight
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        if (mSize == 0) {
            canvas.drawText("您还没有设置数据", 0, 8, mTotalW / 2, mTotalH / 2, mTitlePaint)
            return
        }
        viewHelper.drawContent(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = RectF(paddingLeft.toFloat(), paddingTop.toFloat(), mTotalW - paddingRight, mTotalH - paddingBottom)
        if (event.action != 0 ||
            mSize == 0 ||
            mOnStepClickListener == null ||
            !rect.contains(event.x, event.y)
        ) {
            return super.onTouchEvent(event)
        }

        return viewHelper.handleEvent(event, mOnStepClickListener!!)
    }

    /**
     * 设置纵向还是横向布局
     */
    fun setOrientation(orientation: Orientation) = run {
        // 如果相等, 不改变
        if (mOrientation == orientation) {
            return@run this
        }
        mOrientation = orientation
        initHelper()
        adjustDirection()
        requestLayout()
        invalidate()
        this
    }

    /**
     * 设置进度起始方向, 会影响节点绘制的方向, 但不会影响原有的数据下标,
     * 也就是下标是跟随传入的时候的数据下标, 它在原数据下标是多少就是多少,
     * 永远不会改变的, 即使绘制的方向发生了改变, 一切以原数据下标为主
     *
     * 例如初始传入数据  a->b->c->d->e
     *
     * 如果使用FromLeft,  在屏幕上会显示: a b c d e,
     * 点击b会提示点击了第1个数据(下标从0开始)
     *
     * 如果使用FromRight, 在屏幕上会显示: e d c b a,
     * 点击b也会提示点击了第1个数据(下标从0开始), 即使b现在从左往右数是第3个,
     * 不会随方向而改变自身下标
     *
     * 例如要在index=1处新增f, 数据变为:  a->f->b->c->d->e
     *
     * 如果使用FromLeft,  在屏幕上会显示: a f b c d e
     *
     * 如果使用FromRight, 在屏幕上会显示: e d c b f a
     *
     * 删除操作也是一样, FromTop和FromBottom同理
     */
    fun setDirection(direction: Direction) = run {
        if (mDirection == direction) {
            return@run this
        }
        mDirection = direction
        adjustDirection()
        requestLayout()
        invalidate()
        this
    }

    /**
     * 设置数据, 会清空之前的数据
     */

    fun setNodes(nodes: List<StepModel>) = run {
        clearAllNodes()
        addNodes(nodes)
        requestLayout()
        invalidate()
        this
    }

    /**
     * 添加节点, 位置从0开始, 默认添加到尾部
     * @param index 参考 [setDirection]
     */
    @JvmOverloads
    fun addNode(node: StepModel, index: Int = mSize) = run {
        mNodes.add(index, node)
        requestLayout()
        invalidate()
        this
    }

    /**
     * 批量添加, 位置从0开始, 默认添加到尾部
     * @param index 参考 [setDirection]
     */
    @JvmOverloads
    fun addNodes(nodes: List<StepModel>, index: Int = mSize) = run {
        mNodes.addAll(index, nodes)
        this
    }

    /**
     * 清除所有节点
     */
    fun clearAllNodes() = run {
        mNodes.clear()
        requestLayout()
        invalidate()
        this
    }

    /**
     * 删除节点, 范围从0开始, 不超过size-1
     * @param index 参考 [setDirection]
     */
    fun removeNodeAt(index: Int) = run {
        mNodes.removeAt(index)
        requestLayout()
        invalidate()
        this
    }

    /**
     * 设置已经完成的位置 从0开始
     * 可以设置多个
     * @param indexes 参考 [setDirection]
     */
    fun setDoneAt(vararg indexes: Int) = run {
        indexes.forEach {
            mNodes[it].state = State.Done
        }
        invalidate()
        this
    }

    /**
     * 设置已完成的位置 从0开始
     * @param from 起始位置(包含)
     * @param to 结束位置(包含)
     */
    fun setDoneRange(from: Int = 0, to: Int) = run {
        for (i in from..to) {
            mNodes[i].state = State.Done
        }
        invalidate()
        this
    }

    /**
     * 设置进行中的位置 从0开始
     * 可以设置多个, 一般是最后一个位置
     * @param indexes 参考 [setDirection]
     */
    fun setDoingAt(vararg indexes: Int) = run {
        indexes.forEach {
            mNodes[it].state = State.Doing
        }
        invalidate()
        this
    }

    /**
     * 设置未完成的位置 从0开始
     * 可以设置多个
     * @param indexes 参考 [setDirection]
     */
    fun setUndoAt(vararg indexes: Int) = run {
        indexes.forEach {
            mNodes[it].state = State.Undo
        }
        invalidate()
        this
    }

    /**
     * 设置异常的位置, 从0开始
     * 可以设置多个,异常位置逻辑上一般不可以超过最后一个已完成位置
     * @param indexes 参考 [setDirection]
     */
    fun setErrorAt(vararg indexes: Int) = run {
        indexes.forEach {
            mNodes[it].state = State.Warn
        }
        invalidate()
        this
    }

    /**
     * 设置点击监听, 第一个参数是点击的位置, 从0开始
     * 绘制方向会影响点击位置的数值, 具体参考[setDirection]
     */
    fun setOnNodeClickListener(listener: ((Int, StepModel) -> Unit)) = run {
        mOnStepClickListener = listener
        this
    }

    private fun adjustDirection() {
        mDirection = when (mOrientation) {
            Orientation.Horizontal -> {
                // 在
                if (mDirection in setOf(Direction.FromLeft, Direction.FromRight)) {
                    return
                }
                Direction.FromLeft
            }
            Orientation.Vertical -> {
                if (mDirection in setOf(Direction.FromTop, Direction.FromBottom)) {
                    return
                }
                Direction.FromTop
            }
        }
    }

    sealed class State {
        object Undo : State()
        object Doing : State()
        object Done : State()
        object Warn : State()
    }

    sealed class Direction(val value: Int) {
        object FromLeft : Direction(0)
        object FromRight : Direction(1)
        object FromTop : Direction(2)
        object FromBottom : Direction(3)
        companion object {
            fun getDirection(value: Int): Direction {
                return when (value) {
                    0 -> FromLeft
                    1 -> FromRight
                    2 -> FromTop
                    else -> FromBottom
                }
            }
        }
    }

    sealed class Orientation(val value: Int) {
        object Horizontal : Orientation(0)
        object Vertical : Orientation(1)

        companion object {
            fun getOrientation(value: Int): Orientation {
                return if (value == 0) {
                    Horizontal
                } else {
                    Vertical
                }
            }
        }
    }
}