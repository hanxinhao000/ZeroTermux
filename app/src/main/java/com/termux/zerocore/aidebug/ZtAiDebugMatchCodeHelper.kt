package com.termux.zerocore.aidebug

import com.termux.zerocore.ftp.utils.UserSetManage
import kotlin.random.Random

object ZtAiDebugMatchCodeHelper {

    const val QUERY_PARAM = "code"
    const val HEADER_NAME = "X-Zt-Ai-Debug-Code"
    private val CODE_PATTERN = Regex("^\\d{7}$")
    const val MASKED_DISPLAY = "•••••••"

    fun generateNewCode(): String {
        return (1_000_000 + Random.nextInt(9_000_000)).toString()
    }

    fun ensureCode(): String {
        val bean = UserSetManage.get().getZTUserBean()
        val existing = bean.ztAiDebugMatchCode?.trim().orEmpty()
        if (CODE_PATTERN.matches(existing)) {
            return existing
        }
        val code = generateNewCode()
        bean.ztAiDebugMatchCode = code
        UserSetManage.get().setZTUserBean(bean)
        return code
    }

    fun getStoredCode(): String? {
        val code = UserSetManage.get().getZTUserBean().ztAiDebugMatchCode?.trim().orEmpty()
        return if (CODE_PATTERN.matches(code)) code else null
    }

    fun rotateCode(): String {
        val code = generateNewCode()
        val bean = UserSetManage.get().getZTUserBean()
        bean.ztAiDebugMatchCode = code
        UserSetManage.get().setZTUserBean(bean)
        return code
    }

    fun clearCode() {
        val bean = UserSetManage.get().getZTUserBean()
        bean.ztAiDebugMatchCode = ""
        UserSetManage.get().setZTUserBean(bean)
    }

    fun isValid(input: String?): Boolean {
        val stored = getStoredCode() ?: return false
        val candidate = input?.trim().orEmpty()
        return candidate.isNotEmpty() && candidate == stored
    }
}
