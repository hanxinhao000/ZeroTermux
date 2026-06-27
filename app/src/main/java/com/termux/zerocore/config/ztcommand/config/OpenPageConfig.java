package com.termux.zerocore.config.ztcommand.config;

import static com.termux.zerocore.config.ztcommand.config.ZTKeyConstants.ZT_ID_OPEN_PAGE;

import android.content.Context;
import android.text.TextUtils;

import com.termux.zerocore.config.ztcommand.navigation.ZtNavigationHelper;

public class OpenPageConfig extends BaseOkJsonConfig {
    @Override
    public String getCommand(Context context, String command) {
        try {
            String[] parts = command.trim().split(" ", 2);
            if (parts.length < 2 || TextUtils.isEmpty(parts[1])) {
                return ZtNavigationHelper.listPagesText();
            }
            String pageId = parts[1].trim();
            if ("list".equalsIgnoreCase(pageId)) {
                return ZtNavigationHelper.listPagesText();
            }
            return ZtNavigationHelper.openPage(context, pageId, null);
        } catch (Exception e) {
            e.printStackTrace();
            return getJson(1, e.toString(), "");
        }
    }

    @Override
    public int getId() {
        return ZT_ID_OPEN_PAGE;
    }
}
