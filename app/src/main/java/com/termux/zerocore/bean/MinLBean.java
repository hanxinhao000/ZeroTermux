package com.termux.zerocore.bean;

import java.util.List;

/**
 * @author ZEL
 * @create By ZEL on 2020/12/17 14:43
 **/
public class MinLBean {
    public Data data;
    public static class Data{
        public List<DataNum> list;
    }
    public static class DataNum{
        public String name;
        public String value;
        public boolean isChecked;
        public long id;
        public boolean isPinTop = false;
    }
}
