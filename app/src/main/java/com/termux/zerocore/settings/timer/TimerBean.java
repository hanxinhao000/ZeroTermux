package com.termux.zerocore.settings.timer;

public class TimerBean {
    public static final int TIMER_30_SECOND = 30 * 1000;
    public static final int TIMER_1_MINUTE = 60 * 1000;
    public static final int TIMER_10_MINUTE = 10 * 60 * 1000;
    public static final int TIMER_30_MINUTE = 30 * 60 * 1000;
    public static final int TIMER_OTHER = -1;
    private boolean isZeroTermux = false;
    //默认 10分钟
    private int timerNumber = TIMER_10_MINUTE;
    private long timerOtherNumber = 0;

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
}
