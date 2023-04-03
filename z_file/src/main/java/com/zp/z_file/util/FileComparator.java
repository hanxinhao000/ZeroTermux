package com.zp.z_file.util;

import com.zp.z_file.content.ZFileBean;

import java.io.File;
import java.util.Comparator;


public class FileComparator implements Comparator {
    public int compare(Object file1Path, Object file2Path) {
        File file1 = new File(((ZFileBean)file1Path).getFilePath());
        File file2 = new File(((ZFileBean)file2Path).getFilePath());

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
