package com.github.smaxlyb.stepview

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.github.smaxlyb.stepview.*
import kotlin.math.ceil

/**
 * @description: 水平绘制
 */
class HorizontalStepViewHelper(
    private val stepView: StepView
) : IStepViewHelper {

    companion object {
        // 不包含副标题的固定高度, 减少测量
        private val DEFAULT_HEIGHT_H = 80.dp

        // 含有副标题时固定高度, 减少测量
        private val DEFAULT_HEIGHT_WITH_ASSIST_TEXT_H = 120.dp

        private const val MAIN_TEXT_MAX_SIZE_H = 4
        private const val ASSIST_TEXT_MAX_LINE_H = 2
    }

    private var mUndoIcon: Drawable? = NumberCircleDrawable()
    private var mDoingIcon: Drawable? = NumberCircleDrawable()
    private var mDoneIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_done)
    private var mWarnIcon: Drawable? = AppCompatResources.getDrawable(stepView.context, R.drawable.ic_warn)

    override fun init() {
        tintColor()
    }

    override fun getMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Pair<Int, Int> {
        val h = View.resolveSize(
            // 如果没有副标题文字
            if (stepView.mNodes.firstOrNull { !it.detail.isNullOrBlank() } == null) {
                DEFAULT_HEIGHT_H.toInt()
            } else {
                DEFAULT_HEIGHT_WITH_ASSIST_TEXT_H.toInt()
            }, heightMeasureSpec
        )
        return widthMeasureSpec to h
    }

    override fun drawContent(canvas: Canvas) {
        stepView.run {
            val halfOfItemW = mRemainW / mSize / 2
            val iconY = paddingTop + mRemainH / 4
            val mainTextY = paddingTop + mRemainH / 4 * 2
            val assistTextY = mainTextY + mTitlePaint.fontLineHeight

            mNodes.forEachIndexed { index, node ->
                // 每个item的起始位置x
                val iconX =
                    if (mDirection == StepView.Direction.FromLeft) {
                        paddingLeft + (index * 2 + 1) * halfOfItemW
                    } else {
                        paddingLeft + ((mSize - index) * 2 - 1) * halfOfItemW
                    }
                // 画横线
                kotlin.runCatching {
                    val next = mNodes[index + 1]
                    val lineStartX: Float
                    val lineEndX =
                        if (mDirection == StepView.Direction.FromLeft) {
                            lineStartX = iconX + mLine2IconPadding
                            iconX + 2 * halfOfItemW - mLine2IconPadding
                        } else {
                            lineStartX = iconX - mLine2IconPadding
                            iconX - 2 * halfOfItemW + mLine2IconPadding
                        }
                    canvas.drawLine(
                        lineStartX, iconY, lineEndX, iconY,
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
                drawIcon(canvas, index, node, bound)

                // 画主标题
                canvas.drawText(
                    node.title, 0,
                    if (node.title.length > MAIN_TEXT_MAX_SIZE_H) {
                        MAIN_TEXT_MAX_SIZE_H
                    } else {
                        node.title.length
                    },
                    iconX, mainTextY, mTitlePaint.apply {
                        color = if (node.state == StepView.State.Undo) {
                            mTitleColor
                        } else {
                            ContextCompat.getColor(context, R.color.black)
                        }
                        textAlign = Paint.Align.CENTER
                    }
                )

                // 画副标题
                node.detail?.let {
                    canvas.drawMultilineText(
                        iconX, assistTextY, mDetailPaint.apply { textAlign = Paint.Align.CENTER }, it,
                        ASSIST_TEXT_MAX_LINE_H, mRemainW / mSize - mNodePadding * 2
                    )
                }
            }
        }
    }

    override fun handleEvent(event: MotionEvent, listener: StepClickListener): Boolean {
        stepView.run {
            val x = event.x - paddingLeft
            // 平均分, 不分左右方向
            val n = ceil(x / (mRemainW / mSize)).toInt() - 1
            when (mDirection) {
                StepView.Direction.FromLeft -> {
                    listener.invoke(n, mNodes[n])
                }
                else -> {
                    listener.invoke(mSize - 1 - n, mNodes[mSize - 1 - n])
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

    private fun drawIcon(canvas: Canvas, index: Int, node: StepModel, bound: Rect) {
        val drawable = when (node.state) {
            StepView.State.Undo -> {
                (mUndoIcon as NumberCircleDrawable)
                    .setNum(index + 1)
                    .setTint(stepView.mUndoColor)
                mUndoIcon
            }
            StepView.State.Doing -> {
                (mDoingIcon as NumberCircleDrawable)
                    .setNum(index + 1)
                    .setTint(stepView.mDoneColor)
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