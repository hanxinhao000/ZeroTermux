package com.termux.zerocore.config.ztcommand.navigation

/** TermuxActivity 实现此接口，打开页面前收起 AI 浮层和侧栏 */
interface ZtNavigationHost {
    fun prepareForPageNavigation()
}
