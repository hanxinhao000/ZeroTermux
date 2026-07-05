package com.termux.zerocore.settings.timer;

public class TimerBean {
    public static final int TIMER_30_SECOND = 30 * 1000;
    public static final int TIMER_1_MINUTE = 60 * 1000;
    public static final int TIMER_10_MINUTE = 10 * 60 * 1000;
    public static final int TIMER_30_MINUTE = 30 * 60 * 1000;
    public static final int TIMER_OTHER = -1;

    /** 按固定间隔重复执行 */
    public static final int MODE_INTERVAL = 0;
    /** 每天在指定时刻执行（如 12:00、15:00） */
    public static final int MODE_DAILY_TIME = 1;

    private boolean isZeroTermux = false;
    /** 默认 10 分钟间隔 */
    private int timerNumber = TIMER_10_MINUTE;
    private long timerOtherNumber = 0;

    private int timerMode = MODE_INTERVAL;
    /** 每日定时：小时 0–23 */
    private int scheduledHour = 12;
    /** 每日定时：分钟 0–59 */
    private int scheduledMinute = 0;
    /** 退出 APP 后仍保留并在下次启动时恢复未完成的定时任务 */
    private boolean alwaysAllowTimer = false;

    public long getTimerOtherNumber() {
        return timerOtherNumber;
    }

    public void setTimerOtherNumber(long timerOtherNumber) {
        this.timerOtherNumber = timerOtherNumber;
    }

    public boolean isZeroTermux() {
        return isZeroTermux;
    }

    public void setIsZeroTermux(boolean shell) {
        isZeroTermux = shell;
    }

    public int getTimerNumber() {
        return timerNumber;
    }

    public void setTimerNumber(int timerNumber) {
        this.timerNumber = timerNumber;
    }

    public int getTimerMode() {
        return timerMode;
    }

    public void setTimerMode(int timerMode) {
        this.timerMode = timerMode;
    }

    public int getScheduledHour() {
        return scheduledHour;
    }

    public void setScheduledHour(int scheduledHour) {
        this.scheduledHour = scheduledHour;
    }

    public int getScheduledMinute() {
        return scheduledMinute;
    }

    public void setScheduledMinute(int scheduledMinute) {
        this.scheduledMinute = scheduledMinute;
    }

    public boolean isAlwaysAllowTimer() {
        return alwaysAllowTimer;
    }

    public void setAlwaysAllowTimer(boolean alwaysAllowTimer) {
        this.alwaysAllowTimer = alwaysAllowTimer;
    }
}
