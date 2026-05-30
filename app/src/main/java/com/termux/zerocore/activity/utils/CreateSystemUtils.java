package com.termux.zerocore.activity.utils;

import static com.termux.shared.termux.TermuxConstants.TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH;

import android.util.Log;
import android.widget.Toast;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.google.gson.Gson;
import com.termux.R;
import com.termux.zerocore.bean.CreateSystemBean;
import com.termux.zerocore.bean.ReadSystemBean;
import com.termux.zerocore.url.FileUrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * 切换容器的类（SwitchActivity）写的太乱了，无力吐槽当时的编码习惯，
 * 本类是规整的切换容器工具类，等后续重写 SwitchActivity
 */
public class CreateSystemUtils {
    private final static String TAG = CreateSystemUtils.class.getSimpleName();
    private final static String STRING_FILES = "files";
    private final static String STRING_JSON_PATH = "xinhao_system.infoJson";
    private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // 创建系统
    public static void createSystem(String name) {
        //先扫描有多少文件
        File[] files = getRootFile().listFiles();
        if (files.length == 1) {
            //默认只有一个系统
            File createFile = new File(getRootFile(), STRING_FILES + "1");
            createFile.mkdirs();
            writerSystemFile(createFile, name);
        } else {
            //有多个系统
            int max = getContainerIndex(getRootFile().listFiles());
            LogUtils.i(TAG, "createSystem The current number of containers is: " + max);
            File createFile = new File(getRootFile(), STRING_FILES + (max + 1));
            createFile.mkdirs();
            writerSystemFile(createFile, name);
        }
    }

    private ArrayList<ReadSystemBean> readFile() {
        File[] files = getRootFile().listFiles();
        ArrayList<ReadSystemBean> arrayList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith(STRING_FILES)) {
                ReadSystemBean readSystemBean = new ReadSystemBean();
                readSystemBean.dir = files[i].getAbsolutePath();
                CreateSystemBean createSystemBean = readInfo(files[i].getAbsolutePath());
                LogUtils.i(TAG, "readFile read info: " + createSystemBean);
                String name = createSystemBean.systemName;
                if (name == null) {
                    new File(files[i], "/" + STRING_JSON_PATH).delete();
                    Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.item_containers_toast_config_error), Toast.LENGTH_SHORT).show();
                    return null;
                }
                readSystemBean.name = name;
                readSystemBean.time = createSystemBean.time;
                arrayList.add(readSystemBean);
            }
        }
        Log.e("XINHAO_HAN", "readFile: " + arrayList);
        return arrayList;
    }

    private CreateSystemBean readInfo(String path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(new File(new File(path), "/" + STRING_JSON_PATH))));
            String temp = "";
            String tempSystem = "";
            while ((temp = bufferedReader.readLine()) != null) {
                tempSystem += temp;
            }
            bufferedReader.close();
            CreateSystemBean createSystemBean = new Gson().fromJson(tempSystem, CreateSystemBean.class);
            if (createSystemBean == null) {
                createSystemBean.systemName = UUtils.getString(R.string.item_containers_error_system);
            }
            return createSystemBean;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CreateSystemBean createSystemBean = new CreateSystemBean();
        createSystemBean.systemName = UUtils.getString(R.string.item_containers_toast_def_system);
        return createSystemBean;
    }

    private static File getRootFile() {
        return new File(FileUrl.INSTANCE.getMainFilesUrl() + "/");
    }

    // 获取容器数量
    private static int getContainerIndex(File[] rootFiles) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < rootFiles.length; i++) {
            if (rootFiles[i].getName().startsWith(STRING_FILES)) {
                String name1 = rootFiles[i].getName();
                String substring = name1.substring(5, name1.length());
                if (substring.isEmpty()) {
                    arrayList.add(0);
                } else {
                    arrayList.add(Integer.parseInt(substring));
                }
            }
        }
        return getMax(arrayList);
    }

    // 写入到文件
    private static boolean writerSystemFile(File createFile, String name) {
        LogUtils.i(TAG, "writerSystemFile create system info createFile: "
            + createFile.getAbsolutePath() + " ,name: " + name);
        File fileInfo = getXinhaoJsonPath(createFile);
        CreateSystemBean createSystemBean = new CreateSystemBean();
        createSystemBean.dir = createFile.getAbsolutePath();
        createSystemBean.time = SIMPLE_DATE_FORMAT.format(new Date());
        createSystemBean.systemName = name;
        String s = new Gson().toJson(createSystemBean);
        PrintWriter printWriter = null;
        try {
            fileInfo.createNewFile();
            printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileInfo)));
            printWriter.print(s);
            printWriter.flush();
            printWriter.close();
            return true;
        } catch (IOException e) {
            Toast.makeText(UUtils.getContext(), UUtils.getString(R.string.item_containers_toast_create_system_error), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
    }

    public static void replaceContainer(File newPath, File oldPath) {

    }

    // 获取路径
    private static File getXinhaoJsonPath(File createFile) {
        return new File(createFile, "/" + STRING_JSON_PATH);
    }
    //比大小
    private static int getMax(ArrayList<Integer> number) {
        int temp = number.get(0);
        for (int i = 0; i < number.size(); i++) {
            if (number.get(i) > temp) {
                temp = number.get(i);
            }
        }
        return temp;
    }
}
