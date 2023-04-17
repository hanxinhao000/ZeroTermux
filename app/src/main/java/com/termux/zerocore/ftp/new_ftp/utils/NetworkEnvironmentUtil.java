package com.termux.zerocore.ftp.new_ftp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

public class NetworkEnvironmentUtil {

    /**
     * 反射拿热点是否开启
     */
    public static boolean isAPEnabled(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
            Field field = wifiManager.getClass().getDeclaredField("WIFI_AP_STATE_ENABLED");
            int value_wifi_enabled = (int) field.get(wifiManager);
            return ((int) method.invoke(wifiManager)) == value_wifi_enabled;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 获取WiFi网络的IPv4地址
     *
     * @return 可能为null
     */
    public static @Nullable
    String getWifiIp(@NonNull Context context) {
        try {
            return Formatter.formatIpAddress(((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                    .getConnectionInfo().getIpAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取本地网络环境所有可用的IPv4地址
     *
     * @return 类似"192.168.1.101"的IP地址集
     */
    public static ArrayList<String> getLocalIpv4Addresses() {
        final ArrayList<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface networkInterface;
            while ((networkInterface = networkInterfaces.nextElement()) != null) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                try {
                    InetAddress inetAddress;
                    while ((inetAddress = inetAddresses.nextElement()) != null) {
                        if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                            String ip = inetAddress.getHostAddress();
                            if (!result.contains(ip)) result.add(ip);
                        }
                    }
                } catch (Exception e) {
                    //
                }
            }
        } catch (Exception e) {
            //
        }
        return result;
    }

    public static boolean isWifiConnected(@NonNull Context context) {
        return isNetworkConnected(context, ConnectivityManager.TYPE_WIFI);
    }

    static boolean isCellularNetworkConnected(@NonNull Context context) {
        return isNetworkConnected(context, ConnectivityManager.TYPE_MOBILE);
    }

    private static boolean isNetworkConnected(@NonNull Context context, int type) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.getType() == type;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
