package com.termux.zerocore.ai.agent

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

/** 主界面顶部 AI 运行条显示/隐藏：淡出时高度收缩，终端区域同步上移。 */
object ZtAiAgentTopBannerAnimator {

    private const val DURATION_MS = 260L
    private var activeAnimator: ValueAnimator? = null

    private fun cancelRunning(banner: View) {
        activeAnimator?.cancel()
        activeAnimator = null
        banner.animate().cancel()
    }

    fun show(banner: View) {
        cancelRunning(banner)
        if (banner.visibility == View.VISIBLE && banner.height > 0 && banner.alpha >= 0.99f) {
            return
        }
        val targetHeight = measureHeight(banner)
        if (targetHeight <= 0) {
            banner.visibility = View.VISIBLE
            banner.alpha = 1f
            return
        }
        val params = banner.layoutParams ?: return
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        params.height = 0
        banner.layoutParams = params
        activeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                banner.alpha = fraction
                params.height = (targetHeight * fraction).toInt().coerceAtLeast(0)
                banner.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeAnimator = null
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    banner.layoutParams = params
                    banner.alpha = 1f
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    activeAnimator = null
                }
            })
            start()
        }
    }

    fun hide(banner: View) {
        cancelRunning(banner)
        if (banner.visibility != View.VISIBLE) {
            return
        }
        val startHeight = banner.height.takeIf { it > 0 } ?: measureHeight(banner)
        if (startHeight <= 0) {
            banner.visibility = View.GONE
            banner.alpha = 1f
            return
        }
        val params = banner.layoutParams ?: return
        activeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                banner.alpha = fraction
                params.height = (startHeight * fraction).toInt().coerceAtLeast(0)
                banner.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeAnimator = null
                    banner.visibility = View.GONE
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    banner.layoutParams = params
                    banner.alpha = 1f
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    activeAnimator = null
                }
            })
            start()
        }
    }

    private fun measureHeight(banner: View): Int {
        if (banner.width <= 0) {
            banner.measure(
                View.MeasureSpec.makeMeasureSpec(banner.resources.displayMetrics.widthPixels, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        } else {
            banner.measure(
                View.MeasureSpec.makeMeasureSpec(banner.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        return banner.measuredHeight
    }
}
