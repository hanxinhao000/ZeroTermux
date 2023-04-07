package com.termux.zerocore.http_service;

import com.zp.z_file.content.ZFileBean;

import java.io.File;
import java.util.Comparator;


public class FileComparator implements Comparator {
    public int compare(Object file1Path, Object file2Path) {
        File file1 = (File) file1Path;
        File file2 = (File) file2Path;

        int value1 = 0, value2 = 0;
        if (file1.isDirectory()) {
            value1 = 1;
        }
        if (file2.isDirectory()) {
            value2 = 1;
        }
        if (value1 != value2) {
            return value2 - value1;
        } else {
            return file1.getName().compareTo(file2.getName());
        }
    }

}
