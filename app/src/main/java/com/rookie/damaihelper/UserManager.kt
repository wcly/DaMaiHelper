package com.rookie.damaihelper

object UserManager {

    interface IStartListener {
        fun onStart()
    }

    private const val DEFAULT_KEYWORD = "飞天"

    var startListener: IStartListener? = null
    var keyword: String = DEFAULT_KEYWORD

    fun updateKeyword(keyword: String) {
        if (keyword.isNotBlank()) {
            this.keyword = keyword
        }
    }

    fun startQp() {
        startListener?.onStart()
    }
}