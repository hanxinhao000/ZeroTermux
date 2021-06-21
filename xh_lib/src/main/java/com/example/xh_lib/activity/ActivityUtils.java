package com.example.xh_lib.activity;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ZHT on 2017/4/17.
 * Activity管理工具类(包含对Fragment的添加切换)
 */

public class ActivityUtils {

    private static Map<String, Activity> destoryMap = new HashMap<>();

    //将Activity添加到队列中
    public static void addDestoryActivityToMap(Activity activity, String activityName) {
        destoryMap.put(activityName, activity);
    }

    //根据名字销毁制定Activity
    public static void destoryActivity(String activitySet) {
        Set<String> keySet = destoryMap.keySet();
        if (keySet.size() > 0) {
            for (String key : keySet) {
                if (key.equals(activitySet)) {
                    destoryMap.get(key).finish();
                }
            }
        }
    }

    //销毁所有Activity
    public static void destoryAllActivity() {
        Set<String> keySet = destoryMap.keySet();
        if (keySet.size() > 0) {
            for (String key : keySet) {
                destoryMap.get(key).finish();
            }
        }
    }

}
