package com.github.smaxlyb.stepview

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import kotlin.math.ceil

val Int.dp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics)


val Int.sp
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics)


val Paint.fontLineHeight: Float
    get() = fontMetrics.run { bottom - top + leading }

/**
 * 绘制多行文本, 超过自动换行截断
 * @param x 起始x坐标,
 * @param y 起始y坐标, 从文字基线开始
 * @param maxLine 最大行数, 超过会被截断
 * @param maxWidth 最大宽度, 超过会换行
 */
fun Canvas.drawMultilineText(x: Float, y: Float, paint: Paint, text: String, maxLine: Int, maxWidth: Float) {
    val textW = paint.measureText(text)
    val textLine = textW / maxWidth
    val line = if (textLine > maxLine) {
        maxLine
    } else {
        ceil(textLine).toInt()
    }
    var start = 0
    var end = 0
    repeat(line) {
        start = end
        end += paint.breakText(text, start, text.length, true, maxWidth, floatArrayOf(0F))
        val s = text.substring(start, end)
        drawText(s, x, y + paint.fontLineHeight * it, paint)
    }
}