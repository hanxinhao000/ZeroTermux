package com.termux.zerocore.config.mainmenu;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.xh_lib.utils.LogUtils;
import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.zerocore.activity.EditTextActivity;
import com.termux.zerocore.activity.WebViewActivity;
import com.termux.zerocore.config.mainmenu.config.BaseMenuClickConfig;
import com.termux.zerocore.config.mainmenu.config.MainMenuClickConfig;
import com.termux.zerocore.config.mainmenu.config.OnLineCommandClickConfig;
import com.termux.zerocore.config.mainmenu.config.XMLClickConfig;
import com.termux.zerocore.config.mainmenu.data.MainMenuCategoryData;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.http.HTTPIP;
import com.termux.zerocore.utils.FileIOUtils;

import java.util.ArrayList;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import java.io.File;
import java.util.List;

public class XMLMainMenuConfig {
    private static final String TAG = XMLMainMenuConfig.class.getSimpleName();
    private static final String START_WITH_JAVA = "java:";
    private static final String START_WITH_JUMP_URL = "jumpUrl:";
    private static final String START_WITH_ZT_SHELL = "ztShell:";
    private static final String START_WITH_IMG_PATH = "imgPath:";
    private static final String START_WITH_ZT_EDIT_TEXT = "ztEditText:";
    private static final String START_WITH_START_ACTIVITY = "startActivity:";
    private static final String START_WITH_ACTION_ACTIVITY = "actionActivity:";
    private static final String START_WITH_COMMANDS = "commands:";
    private static final String START_WITH_SHELL_URL = "shellUrl:";
    private static final String START_WITH_DOWNLOAD_URL = "downloadUrl:";
    private static final String START_WITH_APP_WEB_URL = "appWebUrl:";
    private static ArrayList<MainMenuCategoryData> MAIN_MENU_CATEGORY_DATAS = new ArrayList<>();
    private static XMLErrorMessageListener xMLErrorMessageListener;

    public static ArrayList<MainMenuCategoryData> getXmlMainMenuCategoryDatas(Context context) {
        MAIN_MENU_CATEGORY_DATAS.clear();
        initMainMenuCategoryDatas(parseXMLFile(FileIOUtils.INSTANCE.getMainMenuXmlPathFile()), context);
        return MAIN_MENU_CATEGORY_DATAS;
    }

    public static void setXMLErrorMessageListener(XMLErrorMessageListener xmlErrorMessageListener) {
        xMLErrorMessageListener = xmlErrorMessageListener;
    }

    private static void initMainMenuCategoryDatas(List<GroupItem> groupItems, Context context) {
        Log.i(TAG, "getXmlMainMenuCategoryDatas groupItems: " + groupItems);
        for (int i = 0; i < groupItems.size(); i++) {
            ArrayList<MainMenuClickConfig> configs = new ArrayList<>();
            GroupItem groupItem = groupItems.get(i);
            for (int j = 0; j < groupItem.items.size(); j++) {
                MenuItem menuItem = groupItem.items.get(j);
                String name = menuItem.name;
                String icon = menuItem.icon;
                String packageName = menuItem.packageName;
                String intentData = menuItem.intentData;
                boolean autoRunShell = menuItem.autoRunShell;

                boolean isDialogConfirm = menuItem.isDialogConfirm();
                String dialogTitle = menuItem.getDialogTitle();
                String dialogMessage = menuItem.getDialogMessage();

                String listTitle = menuItem.getListTitle();
                String activityTitle = menuItem.getActivityTitle();
                // 跳转
                if (menuItem.clickAction.startsWith(START_WITH_JAVA)) {
                    String clazz = menuItem.clickAction.replace(START_WITH_JAVA, "").trim();
                    try {
                        Object object = createObject(clazz);
                        if (object instanceof BaseMenuClickConfig) {
                            BaseMenuClickConfig baseMenuClickConfig = (BaseMenuClickConfig) object;
                            baseMenuClickConfig.setXmlName(name);
                            configs.add(baseMenuClickConfig);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (menuItem.clickAction.startsWith(START_WITH_JUMP_URL)) {
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        String url = menuItem.clickAction.replace(START_WITH_JUMP_URL, "").trim();
                        Intent intent = new Intent();
                        intent.setData(Uri.parse(url));
                        intent.setAction(Intent.ACTION_VIEW);
                        context.startActivity(intent);
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_ZT_SHELL)) {
                    String shell = menuItem.clickAction.replace(START_WITH_ZT_SHELL, "").trim();
                    if (context instanceof TermuxActivity) {
                        TermuxActivity termuxActivity = (TermuxActivity) context;
                        configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                            if (isDialogConfirm) {
                                SwitchDialog switchDialog = new SwitchDialog(context);
                                switchDialog.createSwitchDialog(dialogMessage);
                                switchDialog.getTitle().setText(dialogTitle);
                                switchDialog.getOk().setOnClickListener(v -> {
                                    switchDialog.dismiss();
                                    if (autoRunShell) {
                                        termuxActivity.sendTextToTerminal(shell + "\n");
                                    } else {
                                        termuxActivity.sendTextToTerminal(shell);
                                    }
                                });
                                switchDialog.show();
                            } else {
                                if (autoRunShell) {
                                    termuxActivity.sendTextToTerminal(shell + "\n");
                                } else {
                                    termuxActivity.sendTextToTerminal(shell);
                                }
                            }
                        }));
                    }
                } else if (menuItem.clickAction.startsWith(START_WITH_ZT_EDIT_TEXT)) {
                    String path = menuItem.clickAction.replace(START_WITH_ZT_EDIT_TEXT, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        Intent intent = new Intent(context1, EditTextActivity.class);
                        intent.putExtra("edit_path", path);
                        context1.startActivity(intent);
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_START_ACTIVITY)) {
                    String className = menuItem.clickAction.replace(START_WITH_START_ACTIVITY, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                            Class<?> clazz = Class.forName(className);
                            Intent intent = new Intent(context, clazz);
                            if (!TextUtils.isEmpty(intentData)) {
                                intent.putExtra(intentData.split("@@")[0], intentData.split("@@")[1]);
                            }
                            Log.i(TAG, "initMainMenuCategoryDatas packageName: " + packageName);
                            if (!TextUtils.isEmpty(packageName)) {
                                intent.setPackage(packageName);
                            }
                            context.startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_ACTION_ACTIVITY)) {
                    String action = menuItem.clickAction.replace(START_WITH_ACTION_ACTIVITY, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                            Intent intent = new Intent(action);
                            if (!TextUtils.isEmpty(packageName)) {
                                intent.setPackage(packageName);
                            }
                            if (!TextUtils.isEmpty(intentData)) {
                                intent.putExtra(intentData.split("@@")[0], intentData.split("@@")[1]);
                            }
                            context1.startActivity(intent);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_COMMANDS)) {
                    String commands = menuItem.clickAction.replace(START_WITH_COMMANDS, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                            String[] split = commands.split(",");
                            showListDialog(listTitle, context, split, autoRunShell);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_SHELL_URL)) {
                    String url = menuItem.clickAction.replace(START_WITH_SHELL_URL, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                          new OnLineCommandClickConfig().showOnLineShDialog(url, context);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_DOWNLOAD_URL)) {
                    String url = menuItem.clickAction.replace(START_WITH_DOWNLOAD_URL, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                           TermuxActivity termuxActivity = (TermuxActivity) context;
                           termuxActivity.startHttp1(url);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else if (menuItem.clickAction.startsWith(START_WITH_APP_WEB_URL)) {
                    String url = menuItem.clickAction.replace(START_WITH_APP_WEB_URL, "").trim();
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        try {
                            Intent intent2 = new Intent(context, WebViewActivity.class);
                            if (!TextUtils.isEmpty(activityTitle)) {
                                intent2.putExtra("title", activityTitle);
                            } else {
                                intent2.putExtra("title", "title");
                            }
                            intent2.putExtra("content", url);
                            context.startActivity(intent2);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }));
                } else {
                    configs.add(getXmlClickConfig(context, name, icon, (view, context1) -> {
                        Toast.makeText(context1, UUtils.getString(R.string.zt_xml_menu), Toast.LENGTH_SHORT).show();
                    }));
                }
            }
            MAIN_MENU_CATEGORY_DATAS.add(new MainMenuCategoryData(groupItem.groupName, groupItem.id, configs));
        }
    }

    private static void showListDialog(String listTitle, Context context, String[] commands, boolean autoRunShell) throws Exception {
        String[] commandTitle = new String[commands.length];
        String[] commandContent = new String[commands.length];
        for (int i = 0; i < commands.length; i++) {
            commandTitle[i] = commands[i].split("@@")[0];
            commandContent[i] = commands[i].split("@@")[1];
        }
        if (TextUtils.isEmpty(listTitle)) {
            listTitle = "";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(listTitle)
            .setItems(commandTitle, (dialog, which) -> {
               TermuxActivity termuxActivity = (TermuxActivity) context;
               if (autoRunShell) {
                   termuxActivity.getTerminalView().sendTextToTerminal(commandContent[which] + "\n");
               } else {
                   termuxActivity.getTerminalView().sendTextToTerminal(commandContent[which]);
               }
            })
            .setNegativeButton(UUtils.getString(com.example.xh_lib.R.string.cancel),
                (dialog, which) -> dialog.dismiss())
            .setCancelable(true); // 点击外部可取消
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static XMLClickConfig getXmlClickConfig(Context context, String name, String icon, XMLClickConfig.ConfigClickListener configClickListener) {
        XMLClickConfig xmlClickConfig = new XMLClickConfig();
        xmlClickConfig.setName(name);
        xmlClickConfig.setDrawable(getDrawable(context, icon));
        xmlClickConfig.setConfigClickListener(configClickListener);
        return xmlClickConfig;
    }

    private static Drawable getDrawable(Context context, String icon) {
        if (TextUtils.isEmpty(icon)) {
            return null;
        }

        if (icon.startsWith(START_WITH_IMG_PATH)) {
            String path = icon.replace(START_WITH_IMG_PATH, "").trim();
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            return fileToDrawable(context, file);
        }
        return null;
    }

    private static Drawable fileToDrawable(Context context, File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                return new BitmapDrawable(context.getResources(), bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static Object createObject(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        return clazz.getDeclaredConstructor().newInstance();
    }

    public static class GroupItem {
        private String groupName;
        private int id = MainMenuConfig.CODE_COMMON_FUNCTIONS;
        private List<MenuItem> items;

        public GroupItem(String groupName) {
            this.groupName = groupName;
            this.items = new ArrayList<>();
        }

        // getters and setters
        public String getGroupName() {
            return groupName;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public List<MenuItem> getItems() {
            return items;
        }

        public void addItem(MenuItem item) {
            items.add(item);
        }

        @Override
        public String toString() {
            return "GroupItem{" +
                "groupName='" + groupName + '\'' +
                ", id=" + id +
                ", items=" + items +
                '}';
        }
    }

    public static class MenuItem {
        private String name;
        private String clickAction;
        private String icon;
        private String packageName;
        private String intentData;
        private boolean autoRunShell;

        private boolean dialogConfirm;
        private String dialogTitle;
        private String dialogMessage;
        private String listTitle;
        private String activityTitle;

        public MenuItem(String name, String clickAction, String icon,
                        boolean autoRunShell, String packageName,
                        boolean dialogConfirm, String dialogTitle,
                        String dialogMessage, String intentData,
                        String listTitle, String activityTitle) {
            this.name = name;
            this.clickAction = clickAction;
            this.icon = icon;
            this.packageName = packageName;
            this.autoRunShell = autoRunShell;
            this.intentData = intentData;
            this.dialogConfirm = dialogConfirm;
            this.dialogTitle = dialogTitle;
            this.dialogMessage = dialogMessage;
            this.listTitle = listTitle;
            this.activityTitle = activityTitle;
        }

        public String getActivityTitle() {
            return activityTitle;
        }

        public String getListTitle() {
            return listTitle;
        }

        public boolean isDialogConfirm() {
            return dialogConfirm;
        }

        public String getDialogTitle() {
            return dialogTitle;
        }

        public String getDialogMessage() {
            return dialogMessage;
        }

        public String getIntentData() {
            return intentData;
        }

        public String getPackageName() {
            return packageName;
        }

        // getters
        public String getName() {
            return name;
        }



        public String getClickAction() {
            return clickAction;
        }

        public String getIcon() {
            return icon;
        }

        @Override
        public String toString() {
            return String.format("Item{name='%s', click='%s', icon='%s'}",
                name, clickAction, icon);
        }
    }

    /**
     * 解析XML文件的方法
     *
     * @param xmlFile XML文件路径
     * @return 解析后的GroupItem列表
     */
    private static List<GroupItem> parseXMLFile(File xmlFile) {
        List<GroupItem> result = new ArrayList<>();

        try {
            // 创建DocumentBuilder
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析XML文件
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();

            // 获取所有group节点
            NodeList groupList = document.getElementsByTagName("group");

            for (int i = 0; i < groupList.getLength(); i++) {
                Element groupElement = (Element) groupList.item(i);

                // 获取group名称
                String groupName = groupElement.getAttribute("name");
                GroupItem groupItem = new GroupItem(groupName);

                // 获取group的id（如果有）
                int groupId = parseIdAttribute(groupElement, "id");
                if (groupId != MainMenuConfig.CODE_COMMON_FUNCTIONS) {
                    groupItem.setId(groupId);
                }
                Log.i(TAG, "parseXMLFilexxxx groupId: " + groupId);

                // 获取group下的所有item节点
                NodeList itemList = groupElement.getElementsByTagName("item");

                for (int j = 0; j < itemList.getLength(); j++) {
                    Element itemElement = (Element) itemList.item(j);

                    // 获取item属性
                    String itemName = itemElement.getAttribute("name");
                    String clickAction = itemElement.getAttribute("click");
                    String icon = itemElement.getAttribute("icon");
                    String autoRunShell = itemElement.getAttribute("autoRunShell");
                    String packageName = itemElement.getAttribute("packageName");
                    String intentData = itemElement.getAttribute("intentData");

                    String dialogConfirm = itemElement.getAttribute("dialogConfirm");
                    String dialogTitle = itemElement.getAttribute("dialogTitle");
                    String dialogMessage = itemElement.getAttribute("dialogMessage");
                    String listTitle = itemElement.getAttribute("listTitle");
                    String activityTitle = itemElement.getAttribute("activityTitle");

                    boolean isAutoRunShell = false;
                    boolean isDialogConfirm = false;
                    if (!TextUtils.isEmpty(autoRunShell)) {
                        isAutoRunShell = autoRunShell.trim().equals("true");
                    }
                    if (!TextUtils.isEmpty(dialogConfirm)) {
                        isDialogConfirm = dialogConfirm.trim().equals("true");
                    }
                    LogUtils.i(TAG, "parseXMLFile icon:" + icon
                        + " ,packageName: " + packageName
                        + ", isDialogConfirm: " + isDialogConfirm
                    );
                    // 创建MenuItem并添加到group中
                    MenuItem menuItem = new MenuItem(itemName, clickAction, icon,
                        isAutoRunShell, packageName,
                        isDialogConfirm, dialogTitle, dialogMessage,
                        intentData, listTitle, activityTitle);
                    groupItem.addItem(menuItem);
                }

                result.add(groupItem);
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (xMLErrorMessageListener != null) {
                xMLErrorMessageListener.Error(UUtils.getString(R.string.zt_xml_menu_error) + e.toString());
            }
        }

        return result;
    }
    /**
     * 安全获取属性值并解析为整数
     */
    private static int parseIdAttribute(Element element, String attributeName) {
        String idValue = element.getAttribute(attributeName);
        if (idValue != null && !idValue.trim().isEmpty()) {
            try {
                return Integer.parseInt(idValue.trim());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return MainMenuConfig.CODE_COMMON_FUNCTIONS;
            }
        }
        return MainMenuConfig.CODE_COMMON_FUNCTIONS; // 默认值，表示没有id
    }
    /**
     * 重载方法，支持字符串路径
     *
     * @param filePath 文件路径字符串
     * @return 解析后的GroupItem列表
     */
    public static List<GroupItem> parseXMLFile(String filePath) {
        return parseXMLFile(new File(filePath));
    }

    public static interface XMLErrorMessageListener{
        void Error(String msg);
    }
}
