package com.termux.zerocore.bean;

import java.util.List;

public class ClipboardBean {

    public Data data;

    public static class Data{
        public List<Clipboard> list;
    }

    public static class Clipboard{
        public String name;
        public int index;
    }

}
