package com.termux.zerocore.utils;

import android.app.DownloadManager;


import com.termux.zerocore.bean.EditPromptBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;



/**
 * @author ZEL
 * @create By ZEL on 2020/7/15 19:11
 **/
public class UUUtils {




    public static ArrayList<String> removeDuplicate_1(ArrayList<String> list){
        for(int i =0;i<list.size()-1;i++){
            for(int j=list.size()-1;j>i;j--){
                if(list.get(i).equals(list.get(j)))
                    list.remove(j);
            }
        }
        return list;
    }

    public static ArrayList<EditPromptBean.EditPromptData> removeDuplicate_2(ArrayList<EditPromptBean.EditPromptData> list){
        for(int i =0;i<list.size()-1;i++){
            for(int j=list.size()-1;j>i;j--){
                if(list.get(i).getIp().equals(list.get(j).getIp()))
                    list.remove(j);
            }
        }

        return list;
    }

    public static String readFileContent(File file) {

        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }



    public static String[] parseHostGetIPAddress(String host) {

        host = host.replace("https://","");
        String[] ipAddressArr = null;
        try {
            InetAddress[] inetAddressArr = InetAddress.getAllByName(host);
            if (inetAddressArr != null && inetAddressArr.length > 0) {
                ipAddressArr = new String[inetAddressArr.length];
                for (int i = 0; i < inetAddressArr.length; i++) {
                    ipAddressArr[i] = inetAddressArr[i].getHostAddress();
                }
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return new String[]{"0.0.0.0"};
        }
        return ipAddressArr;
    }

}
