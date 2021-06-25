package com.termux.zerocore.bean;

import java.util.ArrayList;

public class JoinBean {


    private ArrayList<Data> arrayList;

    public ArrayList<Data> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<Data> arrayList) {
        this.arrayList = arrayList;
    }

    public static class Data{


        private String id;
        private String name;
        private String ip;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }


}
