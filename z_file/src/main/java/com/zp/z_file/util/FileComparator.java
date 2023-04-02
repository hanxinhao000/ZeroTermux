package com.zp.z_file.util;

import com.zp.z_file.content.ZFileBean;

import java.io.File;
import java.util.Comparator;


public class FileComparator implements Comparator {
    public int compare(Object file1Path, Object file2Path) {
        File obj1 = new File(((ZFileBean)file1Path).getFilePath());
        File obj2 = new File(((ZFileBean)file2Path).getFilePath());
        if(obj1.isDirectory() && obj2.isFile()){
            return 1;
        }
        else if(obj1.isFile() && obj2.isDirectory()){
            return -1;
        }
        else{
            return obj1.getName().compareTo(obj2.getName());
        }
    }

}
