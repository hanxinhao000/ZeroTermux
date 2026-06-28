package com.termux.zerocore.config.mainmenu;

import android.content.Context;
import android.text.TextUtils;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.config.mainmenu.config.BaseMenuClickConfig;
import com.termux.zerocore.config.mainmenu.config.MainMenuClickConfig;
import com.termux.zerocore.config.mainmenu.data.MainMenuCategoryData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 程序内置菜单：按 assets/mainmenu 的分组与顺序，仅加载 java: 开头的 Config 类。
 */
public class ProgramMainMenuConfig {

    private static final String START_WITH_JAVA = "java:";

    public static ArrayList<MainMenuCategoryData> getProgramMainMenuCategoryDatas(Context context) {
        ArrayList<MainMenuCategoryData> result = new ArrayList<>();
        List<XMLMainMenuConfig.GroupItem> groups = parseAssetsMenuGroups(context);
        for (XMLMainMenuConfig.GroupItem group : groups) {
            ArrayList<MainMenuClickConfig> configs = new ArrayList<>();
            for (XMLMainMenuConfig.MenuItem item : group.getItems()) {
                String clickAction = item.getClickAction();
                if (TextUtils.isEmpty(clickAction) || !clickAction.startsWith(START_WITH_JAVA)) {
                    continue;
                }
                String clazz = clickAction.replace(START_WITH_JAVA, "").trim();
                try {
                    Object object = Class.forName(clazz).getDeclaredConstructor().newInstance();
                    if (object instanceof BaseMenuClickConfig) {
                        BaseMenuClickConfig config = (BaseMenuClickConfig) object;
                        String name = item.getName();
                        if (!TextUtils.isEmpty(name)) {
                            config.setXmlName(name);
                        }
                        configs.add(config);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!configs.isEmpty()) {
                int groupId = configs.get(0).getType();
                result.add(new MainMenuCategoryData(
                    localizedGroupTitle(context, groupId, group.getGroupName()),
                    groupId,
                    configs
                ));
            }
        }
        return result;
    }

    private static String localizedGroupTitle(Context context, int groupId, String fallback) {
        switch (groupId) {
            case MainMenuConfig.CODE_COMMON_FUNCTIONS:
                return context.getString(R.string.common_functions);
            case MainMenuConfig.CODE_CREATE_PROJECT:
                return context.getString(R.string.menu_create_project);
            case MainMenuConfig.CODE_X11_FEATURES:
                return context.getString(R.string.x11_features);
            case MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION:
                return context.getString(R.string.beautification_function);
            case MainMenuConfig.CODE_ZT_ENGINE:
                return context.getString(R.string.zt_engine);
            case MainMenuConfig.CODE_ZT_ROOT:
                return context.getString(R.string.zt_root_fun);
            case MainMenuConfig.CODE_ONLINE_FEATURES:
                return context.getString(R.string.online_features);
            case MainMenuConfig.CODE_ZT_CONFIG:
                return context.getString(R.string.zt_menu_title_config);
            case MainMenuConfig.CODE_ZT_FEATURES:
                return context.getString(R.string.zt_features);
            default:
                return fallback;
        }
    }

    private static List<XMLMainMenuConfig.GroupItem> parseAssetsMenuGroups(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String lang = (!TextUtils.isEmpty(locale.getLanguage()) && locale.getLanguage().equals("en"))
            ? "en" : "cn";
        File tempFile = new File(context.getCacheDir(), "program_menu_template.xml");
        UUtils.writerFile("mainmenu/" + lang + "/zt_menu_config.xml", tempFile);
        return XMLMainMenuConfig.parseXMLFile(tempFile.getAbsolutePath());
    }
}
