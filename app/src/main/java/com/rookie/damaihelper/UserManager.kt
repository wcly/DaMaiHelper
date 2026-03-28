package com.rookie.damaihelper

object UserManager {

    enum class TaskMode {
        DAMAI,
        I_MAOTAI
    }

    interface IStartListener {
        fun onStart()
    }

    private const val DEFAULT_DAMAI_KEYWORD = "五月天"
    private const val DEFAULT_I_MAOTAI_KEYWORD = "飞天"

    var startListener: IStartListener? = null
    var taskMode: TaskMode = TaskMode.DAMAI

    private var damaiKeyword: String = DEFAULT_DAMAI_KEYWORD
    private var iMaotaiKeyword: String = DEFAULT_I_MAOTAI_KEYWORD

    var singer: String
        get() = damaiKeyword
        set(value) {
            damaiKeyword = value
        }

    fun keywordOf(mode: TaskMode): String {
        return when (mode) {
            TaskMode.DAMAI -> damaiKeyword
            TaskMode.I_MAOTAI -> iMaotaiKeyword
        }
    }

    fun currentKeyword(): String = keywordOf(taskMode)

    fun updateKeyword(mode: TaskMode, keyword: String) {
        if (keyword.isBlank()) {
            return
        }
        when (mode) {
            TaskMode.DAMAI -> damaiKeyword = keyword
            TaskMode.I_MAOTAI -> iMaotaiKeyword = keyword
        }
    }

    fun startQp() {
        startListener?.onStart()
    }

}
