package com.github.smaxlyb.stepview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.valueIterator
import kotlin.math.ceil

/**
 * @description: 垂直绘制
 */
class VerticalStepViewHelper(
    private val stepView: StepView
) : IStepViewHelper {

    companion object {
        // 节点的高度
        private val DEFAULT_NODE_HEIGHT_V = 66.dp
        private const val MAIN_TEXT_MAX_LINE_V = 2
        private const val ASSIST_TEXT_MAX_LINE_V = 2
    }

    private var mUndoIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_undo)
    private var mDoingIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_doing)
    private var mDoneIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_done)
    private var mWarnIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_warn)

    // 记录每个节点额外添加的主+副标题行高, 减少计算
    private val mTextHeightAdded = SparseArray<Float>()

    // 记录每个节点的主标题行数, 减少计算
    private val mTitleLine = SparseIntArray()

    override fun init() {
        tintColor()
    }

    override fun getMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Pair<Int, Int> {
        // 重置数据
        mTextHeightAdded.clear()
        // 如果为空数据, 返回一个节点的高度
        val h = if (stepView.mNodes.isEmpty()) {
            View.resolveSize(DEFAULT_NODE_HEIGHT_V.toInt(), heightMeasureSpec)
        } else {
            // 计算剩余宽度
            var totalH = DEFAULT_NODE_HEIGHT_V * stepView.mNodes.size
            val remainWForText = View.MeasureSpec.getSize(widthMeasureSpec) -
                    stepView.paddingLeft - stepView.paddingRight -
                    stepView.mIconRadius * 2 - stepView.mIcon2TextPadding

            // 记录增加的行高
            stepView.mNodes.forEachIndexed { index, node ->
                // 主标题行数
                val mainTextNeedW = stepView.mTitlePaint.measureText(node.title)
                var mainTextNeedLine = ceil(mainTextNeedW / remainWForText).toInt()
                if (mainTextNeedLine > MAIN_TEXT_MAX_LINE_V) {
                    mainTextNeedLine = MAIN_TEXT_MAX_LINE_V
                }
                mTitleLine.put(index, mainTextNeedLine)
                var heightAdded = (mainTextNeedLine - 1) * stepView.mTitlePaint.fontLineHeight
                // 副标题行数
                node.detail.takeIf { !it.isNullOrBlank() }?.run {
                    val assistTextNeedW = stepView.mDetailPaint.measureText(this)
                    var assistTextNeedLine = ceil(assistTextNeedW / remainWForText).toInt()
                    if (assistTextNeedLine > ASSIST_TEXT_MAX_LINE_V) {
                        assistTextNeedLine = ASSIST_TEXT_MAX_LINE_V
                    }
                    heightAdded += (assistTextNeedLine - 1) * stepView.mDetailPaint.fontLineHeight
                }
                mTextHeightAdded.put(index, heightAdded)
            }
            totalH += mTextHeightAdded.valueIterator().asSequence().sum().toInt()
            View.resolveSize(totalH.toInt(), heightMeasureSpec)
        }
        return widthMeasureSpec to h
    }

    override fun drawContent(canvas: Canvas) {
        stepView.run {
            // 真正剩余的高度
            val remainH = mTotalH - paddingTop - paddingBottom - mTextHeightAdded.valueIterator().asSequence().sum()
            // 每个节点理应均分高度
            val itemH = remainH / mSize
            // icon的x坐标是固定死的
            val iconX = paddingLeft + mIconRadius
            // 文字的x坐标是固定死的
            val textX = iconX + mIconRadius + mIcon2TextPadding
            // 每个节点绘制区域的上边界
            var startY =
                if (mDirection == StepView.Direction.FromTop) {
                    paddingTop.toFloat()
                } else {
                    // 反向的时候第一个上边界
                    mTotalH - paddingBottom - itemH - mTextHeightAdded.get(0)
                }
            mNodes.forEachIndexed { index, node ->
                // 5dp是图标距上边界的距离
                val iconY = startY + mIconRadius * 2 + 5.dp
                // 画横线
                kotlin.runCatching {
                    val next = mNodes[index + 1]
                    val lineStartY: Float
                    val lineEndY =
                        if (mDirection == StepView.Direction.FromTop) {
                            lineStartY = iconY + mLine2IconPadding
                            iconY + (itemH + mTextHeightAdded.get(index)) - mLine2IconPadding
                        } else {
                            lineStartY = iconY - mLine2IconPadding
                            iconY - (itemH + mTextHeightAdded.get(index + 1)) + mLine2IconPadding
                        }

                    canvas.drawLine(
                        iconX, lineStartY, iconX, lineEndY,
                        if (node.state == StepView.State.Done && next.state != StepView.State.Undo) {
                            mLinePaint.apply { color = mDoneColor }
                        } else {
                            mLinePaint.apply { color = mUndoColor }
                        })
                }

                // 画icon
                val bound = Rect(
                    (iconX - mIconRadius).toInt(), (iconY - mIconRadius).toInt(),
                    (iconX + mIconRadius).toInt(), (iconY + mIconRadius).toInt()
                )
                drawIcon(canvas, node, bound)

                // 画主标题
                canvas.drawMultilineText(
                    textX, iconY + mTitlePaint.fontLineHeight / 4, mTitlePaint.apply {
                        textAlign = Paint
                            .Align.LEFT
                        color = if (node.state == StepView.State.Undo) {
                            mTitleColor
                        } else {
                            ContextCompat.getColor(context, R.color.black)
                        }
                    },
                    node.title,
                    MAIN_TEXT_MAX_LINE_V,
                    mTotalW - paddingLeft - paddingRight - mIconRadius * 2 - mIcon2TextPadding
                )

                // 画副标题
                node.detail?.let {
                    canvas.drawMultilineText(
                        textX,
                        iconY + mTitlePaint.fontLineHeight * (mTitleLine.get(index)) + mTitlePaint
                            .fontLineHeight / 4 + 6.dp,
                        mDetailPaint.apply { textAlign = Paint.Align.LEFT },
                        it,
                        ASSIST_TEXT_MAX_LINE_V,
                        mTotalW - paddingLeft - paddingRight - mIconRadius * 2 - mIcon2TextPadding
                    )
                }
                // 刷新下一个的起始点
                kotlin.runCatching {
                    if (mDirection == StepView.Direction.FromTop) {
                        startY += itemH + mTextHeightAdded.get(index)
                    } else {
                        startY -= itemH + mTextHeightAdded.get(index + 1)
                    }
                }
            }
        }
    }

    override fun handleEvent(event: MotionEvent, listener: StepClickListener): Boolean {
        stepView.run {
            val y = event.y - paddingTop
            // 真正剩余的高度
            val remainH = mTotalH - paddingTop - paddingBottom - mTextHeightAdded.valueIterator().asSequence().sum()
            // 每个节点理应均分高度
            val itemH = remainH / mSize
            run out@{
                when (mDirection) {
                    StepView.Direction.FromTop -> {
                        var startY = paddingTop.toFloat()
                        repeat(mSize) {
                            val h = itemH + mTextHeightAdded.get(it)
                            if (startY < y && y < h + startY) {
                                listener.invoke(it, mNodes[it])
                                return@out
                            }
                            startY += h
                        }
                    }
                    else -> {
                        // 反向的时候第一个下边界
                        var startY = mTotalH - paddingBottom
                        repeat(mSize) {
                            val h = itemH + mTextHeightAdded.get(it)
                            if (startY > y && y > startY - h) {
                                listener.invoke(it, mNodes[it])
                                return@out
                            }
                            startY -= h
                        }
                    }
                }
            }
        }
        return true
    }

    private fun tintColor() {
        mUndoIcon?.let { DrawableCompat.setTint(it, stepView.mUndoColor) }
        mDoneIcon?.let { DrawableCompat.setTint(it, stepView.mDoneColor) }
        mDoingIcon?.let { DrawableCompat.setTint(it, stepView.mDoneColor) }
        mWarnIcon?.let { DrawableCompat.setTint(it, stepView.mDoneColor) }
    }

    private fun drawIcon(canvas: Canvas, node: StepModel, bound: Rect) {
        val drawable = when (node.state) {
            StepView.State.Undo -> {
                mUndoIcon
            }
            StepView.State.Doing -> {
                mDoingIcon
            }
            StepView.State.Done -> {
                mDoneIcon
            }
            StepView.State.Warn -> {
                mWarnIcon
            }
        }
        drawable?.run {
            bounds = bound
            draw(canvas)
        }
    }
}