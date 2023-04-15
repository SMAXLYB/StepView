package com.github.smaxlyb.stepview

/**
 * @description: stepView数据模型
 */
data class StepModel(
    /**
     * 主标题文字, 不可以为null, 也不可以为空字符串
     */
    var title: String,

    /**
     * 副标题文字, 可以为null
     * 当这个节点不需要副标题文字时, 设置为null, 而不是空字符串
     */
    var detail: String? = null,

    /**
     * 节点状态, 控制图标和颜色
     */
    var state: StepView.State = StepView.State.Undo,

    /**
     *  是否完成 保留字段, 后续如果要将图标和颜色分离控制就使用这个字段
     */
    var isFinished: Boolean = false
)