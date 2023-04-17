package com.termux.zerocore.ftp.new_ftp.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Environment;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StorageUtil {
    public static String getMainStoragePath() {
        try {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get all available storage paths on the device.
     */
    public static List<String> getAvailableStoragePaths() {
        try {
            List<String> paths = new ArrayList<>();
            String mainStorage = getMainStoragePath().toLowerCase(Locale.getDefault()).trim();
            try {
                paths.add(mainStorage);
            } catch (Exception e) {
            }

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("mount");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                //Log.d("", line);
                if (line.contains("proc") || line.contains("tmpfs") || line.contains("media") || line.contains("asec") || line.contains("secure") || line.contains("system") || line.contains("cache")
                        || line.contains("sys") || line.contains("data") || line.contains("shell") || line.contains("root") || line.contains("acct") || line.contains("misc") || line.contains("obb")) {
                    continue;
                }
                if (line.contains("fat") || line.contains("fuse") || (line.contains("ntfs"))) {

                    String items[] = line.split(" ");
                    if (items != null && items.length > 1) {
                        String path = items[1].toLowerCase(Locale.getDefault());
                        if (!path.toLowerCase(Locale.getDefault()).trim().equals(mainStorage))
                            paths.add(path);
                    }
                }


            }
            //Log.d("StoragePaths", Arrays.toString(paths.toArray()));
            return paths;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    @TargetApi(19)
    public static List<String> getAvailableStoragePaths(@NonNull Context context) {
        final ArrayList<String> arrayList = new ArrayList<>();
        try {
            File[] exFiles = context.getExternalFilesDirs(null);
            if (exFiles != null) {
                for (File file : exFiles) {
                    try {
                        String path = file.getAbsolutePath().toLowerCase();
                        path = path.substring(0, path.indexOf("/android/data"));
                        arrayList.add(path);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrayList;
    }
}
