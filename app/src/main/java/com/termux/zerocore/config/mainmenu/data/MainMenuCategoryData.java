package com.termux.zerocore.config.mainmenu.data;

import com.termux.zerocore.config.mainmenu.config.MainMenuClickConfig;

import java.util.ArrayList;

public class MainMenuCategoryData {
    public int mId;
    public String mTitle;

    public ArrayList<MainMenuClickConfig> mClickArrayList;
    public MainMenuCategoryData(String title, int id, ArrayList<MainMenuClickConfig> clickArrayList) {
        mTitle = title;
        mId = id;
        mClickArrayList = clickArrayList;
    }
}
