package com.termux.zerocore.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class XMLMergeUtils {

    private static final String TAG = "XMLMergeUtils";

    public static void smartUpdateMenuLanguage(Context context, String targetLang) {
        String targetAssetPath;
        String sourceAssetPath;

        String dir = ZtMenuLocale.assetSubdir(targetLang);
        targetAssetPath = ZtMenuLocale.menuAssetPath(dir);
        if (ZtMenuLocale.EN.equals(dir)) {
            sourceAssetPath = ZtMenuLocale.menuAssetPath(ZtMenuLocale.CN);
        } else {
            sourceAssetPath = ZtMenuLocale.menuAssetPath(ZtMenuLocale.EN);
        }

        File userFile = FileIOUtils.INSTANCE.getMainMenuXmlPathFile();
        if (!userFile.exists()) {
            Log.i(TAG, "User file not exists, create new one from assets: " + targetAssetPath);
            UUtils.writerFile(targetAssetPath, userFile);
            return;
        }

        try {
            Log.i(TAG, "Start smart merging. Target: " + targetLang);

            Map<String, String> clickToTargetName = new HashMap<>();
            Map<String, String> clickToTargetGroupName = new HashMap<>();
            parseAssetFile(context, targetAssetPath, clickToTargetName, clickToTargetGroupName);

            Map<String, String> clickToSourceName = new HashMap<>();
            Map<String, String> clickToSourceGroupName = new HashMap<>();
            parseAssetFile(context, sourceAssetPath, clickToSourceName, clickToSourceGroupName);

            Document userDoc;
            try (FileInputStream fis = new FileInputStream(userFile)) {
                userDoc = Jsoup.parse(fis, "UTF-8", "", Parser.xmlParser());
            }

            int updatedItems = 0;
            int updatedGroups = 0;

            Elements userGroups = userDoc.select("group");
            for (Element group : userGroups) {
                String userGroupName = group.attr("name").trim();
                Elements groupItems = group.select("item");

                for (Element item : groupItems) {
                    String click = item.attr("click").trim();

                    if (clickToTargetGroupName.containsKey(click) && clickToSourceGroupName.containsKey(click)) {
                        String targetGroupName = clickToTargetGroupName.get(click);
                        String sourceGroupName = clickToSourceGroupName.get(click);

                        if (userGroupName.equals(targetGroupName) || userGroupName.equals(sourceGroupName)) {
                            if (!userGroupName.equals(targetGroupName)) {
                                group.attr("name", targetGroupName);
                                updatedGroups++;
                            }
                            break;
                        }
                    }
                }
            }

            Elements userItems = userDoc.select("item");
            for (Element item : userItems) {
                String click = item.attr("click").trim();
                String userName = item.attr("name").trim();

                if (clickToTargetName.containsKey(click) && clickToSourceName.containsKey(click)) {
                    String targetName = clickToTargetName.get(click);
                    String sourceName = clickToSourceName.get(click);

                    if (userName.equals(targetName) || userName.equals(sourceName)) {
                         if (!userName.equals(targetName)) {
                             item.attr("name", targetName);
                             updatedItems++;
                         }
                    }
                }
            }

            Log.i(TAG, "Merge finished. Updated " + updatedGroups + " groups and " + updatedItems + " items.");

            userDoc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
            userDoc.outputSettings().prettyPrint(false);
            userDoc.outputSettings().charset("UTF-8");
            userDoc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
            UUtils.setFileString(userFile, userDoc.outerHtml());

        } catch (Exception e) {
            Log.e(TAG, "Smart merge failed", e);
            e.printStackTrace();
        }
    }

    private static void parseAssetFile(Context context, String assetPath,
                                     Map<String, String> clickToNameMap,
                                     Map<String, String> clickToGroupNameMap) throws Exception {
        try (InputStream is = context.getAssets().open(assetPath)) {
            Document doc = Jsoup.parse(is, "UTF-8", "", Parser.xmlParser());

            Elements groups = doc.select("group");
            for (Element group : groups) {
                String groupName = group.attr("name").trim();
                Elements items = group.select("item");
                for (Element item : items) {
                    String click = item.attr("click").trim();
                    String name = item.attr("name").trim();
                    if (!TextUtils.isEmpty(click)) {
                        if (!TextUtils.isEmpty(name)) {
                            clickToNameMap.put(click, name);
                        }
                        if (!TextUtils.isEmpty(groupName)) {
                            clickToGroupNameMap.put(click, groupName);
                        }
                    }
                }
            }
        }
    }
}
