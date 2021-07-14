package com.termux.zerocore.utils;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DskUtils {

    public static void getDiskInfo() {
        StorageManager mstorageManager = (StorageManager) UUtils.getContext().getSystemService(Context.STORAGE_SERVICE);
        try {
            //DiskInfo
            Class<?> diskIndoClass = Class.forName("android.os.storage.DiskInfo");
            Method isUsb = diskIndoClass.getMethod("isUsb");
            Method isSd = diskIndoClass.getMethod("isSd");

            //VolumeInfo
            Class<?> volumeClass = Class.forName("android.os.storage.VolumeInfo");
            Method volumeDisk = volumeClass.getMethod("getDisk");
            Method fsUuid = volumeClass.getMethod("getFsUuid");
            Method path = volumeClass.getMethod("getPath");

            Method getVolumes = StorageManager.class.getDeclaredMethod("getVolumes");
            List volumeInfoList = (List) getVolumes.invoke(mstorageManager);
            for(int i= 0 ;i<volumeInfoList.size();i++){
                //反射得到diskInfo,并判断是不是usb
                if(volumeDisk.invoke(volumeInfoList.get(i)) != null && (boolean)isUsb.invoke(volumeDisk.invoke(volumeInfoList.get(i)))){
                    String path_usb = "mnt/media_rw/"+fsUuid.invoke(volumeInfoList.get(i));
                    Log.e("willie","usb path::"+path_usb);
//                    Log.e("willie","sdcard path_path::"+path.invoke(volumeInfoList.get(i)));
                   // tv_msg.setText("usb:"+path_usb);
                    Log.e("willie","fsUuid::"+fsUuid.invoke(volumeInfoList.get(i)));
                } else if(volumeDisk.invoke(volumeInfoList.get(i)) != null && (boolean)isSd.invoke(volumeDisk.invoke(volumeInfoList.get(i)))){
                    String path_sdcard =""+path.invoke(volumeInfoList.get(i));
                    Log.e("willie","sdcard path::"+path_sdcard);
                   // tv_msg1.setText("sdcard:"+path_sdcard);
                    Log.e("willie","fsUuid::"+fsUuid.invoke(volumeInfoList.get(i)));
                } else{
                    Log.e("willie","other::"+path.invoke(volumeInfoList.get(i)));
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static List<String> getAllExterSdcardPath()
    {
        List<String> SdList = new ArrayList<String>();

        String firstPath = Environment.getExternalStorageDirectory().getPath();

        try
        {
            Runtime runtime = Runtime.getRuntime();
            // 运行mount命令，获取命令的输出，得到系统中挂载的所有目录
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null)
            {
                Log.d("", line);
                // 将常见的linux分区过滤掉

                if (line.contains("proc") || line.contains("tmpfs") || line.contains("media") || line.contains("asec") || line.contains("secure") || line.contains("system") || line.contains("cache")
                    || line.contains("sys") || line.contains("data") || line.contains("shell") || line.contains("root") || line.contains("acct") || line.contains("misc") || line.contains("obb"))
                {
                    continue;
                }

                // 下面这些分区是我们需要的
                if (line.contains("fat") || line.contains("fuse") || (line.contains("ntfs")))
                {
                    // 将mount命令获取的列表分割，items[0]为设备名，items[1]为挂载路径
                    String items[] = line.split(" ");
                    if (items != null && items.length > 1)
                    {
                        String path = items[1].toLowerCase(Locale.getDefault());
                        // 添加一些判断，确保是sd卡，如果是otg等挂载方式，可以具体分析并添加判断条件
                        if (path != null && !SdList.contains(path) && path.contains("sd"))
                            SdList.add(items[1]);
                    }
                }
            }
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!SdList.contains(firstPath))
        {
            SdList.add(firstPath);
        }

        return SdList;
    }


}
