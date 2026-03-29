package com.rookie.damaihelper

object UserManager {

    private const val DEFAULT_KEYWORD = "飞天"

    var keyword: String = DEFAULT_KEYWORD

    fun updateKeyword(keyword: String) {
        if (keyword.isNotBlank()) {
            this.keyword = keyword
        }
    }
}