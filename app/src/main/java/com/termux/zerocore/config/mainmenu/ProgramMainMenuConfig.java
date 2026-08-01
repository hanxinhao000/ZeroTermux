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
        int groupIndex = 0;
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
                // 标题以 assets 分组名为准；不能用 configs[0].getType()——多数 Config 默认是「常用功能」
                String xmlTitle = group.getGroupName();
                int groupId = resolveGroupId(context, xmlTitle, groupIndex);
                result.add(new MainMenuCategoryData(
                    localizedGroupTitle(context, groupId, xmlTitle),
                    groupId,
                    configs
                ));
                groupIndex++;
            }
        }
        return result;
    }

    /**
     * 按 XML 分组名解析稳定 id（展开状态持久化用）。
     * 勿用条目 getType()：BaseMenuClickConfig 默认返回 CODE_COMMON_FUNCTIONS。
     */
    private static int resolveGroupId(Context context, String groupName, int groupIndex) {
        if (groupNameMatches(context, groupName, R.string.common_functions,
            "常用功能", "Common Functions")) {
            return MainMenuConfig.CODE_COMMON_FUNCTIONS;
        }
        if (groupNameMatches(context, groupName, R.string.menu_create_project,
            "创建项目", "Create Project")) {
            return MainMenuConfig.CODE_CREATE_PROJECT;
        }
        if (groupNameMatches(context, groupName, R.string.x11_features,
            "X11功能", "X11 Functions")) {
            return MainMenuConfig.CODE_X11_FEATURES;
        }
        if (groupNameMatches(context, groupName, R.string.beautification_function,
            "美化/UI 功能", "Beautification/UI Functions", "美化/UI")) {
            return MainMenuConfig.CODE_BEAUTIFICATION_FUNCTION;
        }
        if (groupNameMatches(context, groupName, R.string.zt_engine,
            "需要插件/引擎(安装完成请重启APP)",
            "Requires Plugin/Engine (Restart APP after installation)")) {
            return MainMenuConfig.CODE_ZT_ENGINE;
        }
        if (groupNameMatches(context, groupName, R.string.zt_root_fun,
            "ROOT功能", "ROOT Functions")) {
            return MainMenuConfig.CODE_ZT_ROOT;
        }
        if (groupNameMatches(context, groupName, R.string.online_features,
            "线上功能", "Online Functions")) {
            return MainMenuConfig.CODE_ONLINE_FEATURES;
        }
        if (groupNameMatches(context, groupName, R.string.zt_menu_title_config,
            "配置终端", "Configure Terminal")) {
            return MainMenuConfig.CODE_ZT_CONFIG;
        }
        if (groupNameMatches(context, groupName, R.string.zt_features,
            "ZT功能", "ZT 功能", "ZT Functions")) {
            return MainMenuConfig.CODE_ZT_FEATURES;
        }
        // 未知分组：保证 id 互不冲突，避免展开状态串组
        return 1000 + groupIndex;
    }

    private static boolean groupNameMatches(Context context, String groupName, int stringRes, String... aliases) {
        if (TextUtils.isEmpty(groupName)) {
            return false;
        }
        String localized = context.getString(stringRes);
        if (namesEqualLoose(groupName, localized)) {
            return true;
        }
        for (String alias : aliases) {
            if (namesEqualLoose(groupName, alias)) {
                return true;
            }
        }
        // 兼容「美化/UI」与「美化/UI 功能」、「ZT」与「ZT Functions」等
        String g = normalizeGroupName(groupName);
        String l = normalizeGroupName(localized);
        return (!l.isEmpty() && (g.startsWith(l) || l.startsWith(g) || g.contains(l)));
    }

    private static boolean namesEqualLoose(String a, String b) {
        return normalizeGroupName(a).equals(normalizeGroupName(b));
    }

    private static String normalizeGroupName(String name) {
        return name == null ? "" : name.replace(" ", "").trim();
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
