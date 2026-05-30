package com.termux.zerocore.bean;

public class CreateSystemBean {
    public String systemName;
    public String dir;
    public String time;

    @Override
    public String toString() {
        return "CreateSystemBean{" +
            "systemName='" + systemName + '\'' +
            ", dir='" + dir + '\'' +
            ", time='" + time + '\'' +
            '}';
    }
}
