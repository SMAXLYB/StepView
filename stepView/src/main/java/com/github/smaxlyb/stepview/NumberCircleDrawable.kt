package com.github.smaxlyb.stepview

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.TintAwareDrawable
import kotlin.math.min

/**
 * @description: 数字圆①
 */
@SuppressLint("RestrictedApi")
class NumberCircleDrawable : Drawable(), TintAwareDrawable {

    private val mPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }
    private var mBgColor = 0
    private var mNum = 0

    override fun draw(canvas: Canvas) {
        val r = min(bounds.width(), bounds.height()) / 2f - 1.dp
        val x = bounds.left + bounds.width() / 2f
        val y = bounds.top + bounds.height() / 2f
        mPaint.color = mBgColor
        canvas.drawCircle(x, y, r, mPaint)
        mPaint.apply {
            color = Color.WHITE
            textSize = 12.sp
            textAlign = Paint.Align.CENTER
        }
        val rect = Rect()
        mPaint.getTextBounds("$mNum", 0, 1, rect)
        // 因为baseline, -1稍微上移一点点, 减少计算
        canvas.drawText("$mNum", 0, 1, x, y + rect.height() / 2 - 1, mPaint)
    }

    override fun setTint(tintColor: Int) {
        mBgColor = tintColor
    }

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    fun setNum(num: Int) = run {
        mNum = num
        this
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}