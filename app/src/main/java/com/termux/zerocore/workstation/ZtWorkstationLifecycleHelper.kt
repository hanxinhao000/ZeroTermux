package com.termux.zerocore.workstation

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Tracks app activities so workstation session-only mode ends when the app fully exits.
 */
object ZtWorkstationLifecycleHelper {

    private var activityCount = 0
    private var registered = false

    @JvmStatic
    fun register(application: Application) {
        if (registered) return
        registered = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activityCount++
            }

            override fun onActivityDestroyed(activity: Activity) {
                activityCount--
                if (activity.isChangingConfigurations) return
                if (activityCount <= 0) {
                    activityCount = 0
                    ZtWorkstationManager.clearSessionOnAppExitIfNeeded(activity.applicationContext)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        })
    }
}
