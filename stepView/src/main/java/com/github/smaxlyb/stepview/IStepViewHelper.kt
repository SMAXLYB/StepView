package com.github.smaxlyb.stepview

import android.graphics.Canvas
import android.view.MotionEvent

interface IStepViewHelper {

    fun init()

    /**
     * 测量
     * 根据传入的父view测量规格和当前实体数据, 计算出当前view要显示的宽高
     * @return 返回pair对象, 第一个int代表计算出的宽规格, 第二个int代表计算出的高规格
     */
    fun getMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Pair<Int, Int>

    /**
     * 绘制内容
     */
    fun drawContent(canvas: Canvas)

    /**
     * 处理点击事件
     * @return 是否终止事件传递
     */
    fun handleEvent(event: MotionEvent, listener: StepClickListener): Boolean
}