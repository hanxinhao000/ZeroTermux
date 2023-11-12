package com.termux.zerocore.bean;

import java.util.ArrayList;

public class ItemMenuBean {
    public ArrayList<Data> mList;
    public static class Data {
        public String title;
        public int id;
        public int key;
        public boolean isEg = false;
        public int backColor = -1;

        public boolean isBackAnim = false;
    }
}
