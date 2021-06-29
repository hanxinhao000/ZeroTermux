package com.termux.zerocore.bean;

import java.util.ArrayList;

public class EditPromptBean {


    private ArrayList<EditPromptData> arrayList;

    public ArrayList<EditPromptData> getArrayList() {
        return arrayList;
    }

    public void setArrayList(ArrayList<EditPromptData> arrayList) {
        this.arrayList = arrayList;
    }

    public static class  EditPromptData{


        private String ip;
        //0 正在连接 1 已连接 2 未连接
        private int connection;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getConnection() {
            return connection;
        }

        public void setConnection(int connection) {
            this.connection = connection;
        }
    }


}
