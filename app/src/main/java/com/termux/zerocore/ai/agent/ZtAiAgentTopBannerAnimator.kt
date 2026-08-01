package com.termux.zerocore.ai.agent

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

/** 主界面顶部 AI 运行条显示/隐藏：高度展开/收缩 + 淡入淡出。 */
object ZtAiAgentTopBannerAnimator {

    private const val DURATION_MS = 220L
    private var activeAnimator: ValueAnimator? = null
    private var pendingShow: Runnable? = null

    private fun cancelRunning(banner: View) {
        pendingShow?.let { banner.removeCallbacks(it) }
        pendingShow = null
        activeAnimator?.cancel()
        activeAnimator = null
        banner.animate().cancel()
    }

    fun show(banner: View) {
        cancelRunning(banner)
        if (banner.visibility == View.VISIBLE
            && banner.height > 0
            && banner.alpha >= 0.99f
            && banner.translationY == 0f
        ) {
            return
        }
        // GONE 时先改为 INVISIBLE，保证 measure 能拿到真实高度。
        if (banner.visibility != View.VISIBLE) {
            banner.alpha = 0f
            banner.translationY = 0f
            banner.visibility = View.INVISIBLE
        }
        val targetHeight = measureHeight(banner)
        if (targetHeight <= 0) {
            val retry = Runnable {
                pendingShow = null
                val height = measureHeight(banner)
                if (height <= 0) {
                    forceVisible(banner)
                } else {
                    animateShow(banner, height)
                }
            }
            pendingShow = retry
            banner.post(retry)
            return
        }
        animateShow(banner, targetHeight)
    }

    fun hide(banner: View) {
        cancelRunning(banner)
        if (banner.visibility == View.GONE) {
            resetHidden(banner)
            return
        }
        val startHeight = banner.height.takeIf { it > 0 } ?: measureHeight(banner)
        if (startHeight <= 0) {
            resetHidden(banner)
            return
        }
        val params = banner.layoutParams ?: run {
            resetHidden(banner)
            return
        }
        params.height = startHeight
        banner.layoutParams = params
        banner.visibility = View.VISIBLE
        activeAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                banner.alpha = fraction
                banner.translationY = -startHeight * (1f - fraction)
                params.height = (startHeight * fraction).toInt().coerceAtLeast(0)
                banner.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeAnimator = null
                    resetHidden(banner)
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    activeAnimator = null
                }
            })
            start()
        }
    }

    private fun animateShow(banner: View, targetHeight: Int) {
        val params = banner.layoutParams ?: run {
            forceVisible(banner)
            return
        }
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.translationY = -targetHeight.toFloat()
        params.height = 0
        banner.layoutParams = params
        banner.requestLayout()
        activeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                banner.alpha = fraction
                banner.translationY = -targetHeight * (1f - fraction)
                params.height = (targetHeight * fraction).toInt().coerceAtLeast(0)
                banner.layoutParams = params
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    activeAnimator = null
                    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    banner.layoutParams = params
                    banner.alpha = 1f
                    banner.translationY = 0f
                    banner.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    activeAnimator = null
                }
            })
            start()
        }
    }

    private fun forceVisible(banner: View) {
        val params = banner.layoutParams
        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            banner.layoutParams = params
        }
        banner.alpha = 1f
        banner.translationY = 0f
        banner.visibility = View.VISIBLE
    }

    private fun resetHidden(banner: View) {
        val params = banner.layoutParams
        if (params != null) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            banner.layoutParams = params
        }
        banner.alpha = 1f
        banner.translationY = 0f
        banner.visibility = View.GONE
    }

    private fun measureHeight(banner: View): Int {
        val widthSpec = if (banner.width > 0) {
            View.MeasureSpec.makeMeasureSpec(banner.width, View.MeasureSpec.EXACTLY)
        } else {
            val parentWidth = (banner.parent as? View)?.width?.takeIf { it > 0 }
                ?: banner.resources.displayMetrics.widthPixels
            View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
        }
        banner.measure(widthSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return banner.measuredHeight
    }
}
