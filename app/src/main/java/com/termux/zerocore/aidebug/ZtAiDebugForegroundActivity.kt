package com.termux.zerocore.aidebug

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/** Tracks foreground Activity for screenshot capture. */
object ZtAiDebugForegroundActivity {

    @Volatile
    private var activityRef: WeakReference<Activity>? = null

    private var registered = false

    fun ensureRegistered(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (activityRef?.get() === activity) {
                    activityRef = null
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun current(): Activity? = activityRef?.get()
}
