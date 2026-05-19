package com.termux.zerocore.bean;

import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

public class ZTUserBean {
    // 是否打开ZT下载服务器
    private boolean isOpenDownloadFileServices = false;
    // 输入法调起侧边栏是否关闭
    private boolean inputMethodTriggerClose = false;
    // 美化设置关闭菜单
    private boolean styleTriggerOff = false;
    // 禁止工具箱显示
    private boolean isToolShow = false;
    // 强制使用此处小键盘设置规则模式
    private boolean forceUseNumpad = false;
    //是否输出显示LOG
    private boolean isOutputLOG = false;
    //是否显示雪花
    private boolean isSnowflakeShow = false;
    //是否还原音量加减
    private boolean isResetVolume = false;
    //显示/隐藏终端
    private boolean isShowCommand = false;
    //内部/外部通道
    private boolean isInternalPassage = false;
    //在线服务器设定本地保存
    private String mServerJsonString;
    // deepseek 本地蓝色连接字点击
    private String mCommandLink;

    //DeepSeek api Key
    private String mDeepSeekApiKey;
    // DeepSeek url
    private String mDeepSeekApiUrl;
    // 是否让DeepSeek 看见你的终端?
    private boolean mIsDeepSeekVisibleTerminal;
    // 是否折叠菜单
    private boolean isCloseFoldMenu;
    // 是否禁用主菜单配置文件
    private boolean isDisableMainConfigMenu;
    // 是否显示引导页面
    private boolean isHideGuideLayout;
    // 是否写入过菜单背景
    private boolean isWriterMenuBack;

    // LLM api Key (字段名保留，Gson序列化兼容)
    private String mCustomApiKey;
    // LLM url (字段名保留，Gson序列化兼容)
    private String mCustomApiUrl;
    private boolean mIsCustomVisibleTerminal;
    // 选择AI
    private boolean mIsCustomAi;
    // 是否显示左/右侧背景图片
    private boolean mIsBackMenuVisible;
    // 是否在内部存储/Android/data创建文件
    private boolean isCreateFolderForSdcardAndroid;
    // 是否跳过引导页面
    private boolean isJumpGuide;
    // 文本编辑器自动换行
    private boolean isEditorWordWrap = false;

    // 双击终端功能
    private int mDoubleClickFun;

    public boolean isEditorWordWrap() {
        return isEditorWordWrap;
    }

    public void setEditorWordWrap(boolean editorWordWrap) {
        isEditorWordWrap = editorWordWrap;
    }

    public int getDoubleClickFun() {
        return mDoubleClickFun;
    }

    public void setDoubleClickFun(int doubleClickFun) {
        this.mDoubleClickFun = doubleClickFun;
    }

    public boolean isJumpGuide() {
        return isJumpGuide;
    }

    public void setJumpGuide(boolean jumpGuide) {
        isJumpGuide = jumpGuide;
    }

    public boolean isCreateFolderForSdcardAndroid() {
        return isCreateFolderForSdcardAndroid;
    }

    public void setCreateFolderForSdcardAndroid(boolean createFolderForSdcard) {
        isCreateFolderForSdcardAndroid = createFolderForSdcard;
    }

    public boolean isBackMenuVisible() {
        return mIsBackMenuVisible;
    }

    public void setIsBackMenuVisible(boolean isBackMenuVisible) {
        this.mIsBackMenuVisible = isBackMenuVisible;
    }

    public boolean isCustomAi() {
        return mIsCustomAi;
    }

    public void setCustomAi(boolean deepSeekAi) {
        mIsCustomAi = deepSeekAi;
    }

    public boolean isIsCustomVisibleTerminal() {
        return mIsCustomVisibleTerminal;
    }

    public void setIsCustomVisibleTerminal(boolean isCustomVisibleTerminal) {
        this.mIsCustomVisibleTerminal = isCustomVisibleTerminal;
    }

    // 自定义AI系统提示语
    private String mCustomSystemPrompt;

    public String getCustomSystemPrompt() {
        return mCustomSystemPrompt;
    }

    public void setCustomSystemPrompt(String customSystemPrompt) {
        this.mCustomSystemPrompt = customSystemPrompt;
    }

    public String getCustomApiKey() {
        return mCustomApiKey;
    }

    public void setCustomApiKey(String customApiKey) {
        this.mCustomApiKey = customApiKey;
    }

    public String getCustomApiUrl() {
        return mCustomApiUrl;
    }

    public void setCustomApiUrl(String customApiUrl) {
        this.mCustomApiUrl = customApiUrl;
    }

    public boolean isWriterMenuBack() {
        return isWriterMenuBack;
    }

    public void setWriterMenuBack(boolean writerMenuBack) {
        isWriterMenuBack = writerMenuBack;
    }

    public boolean isHideGuideLayout() {
        return isHideGuideLayout;
    }

    public void setHideGuideLayout(boolean hideGuideLayout) {
        isHideGuideLayout = hideGuideLayout;
    }

    public boolean isDisableMainConfigMenu() {
        return isDisableMainConfigMenu;
    }

    public void setDisableMainConfigMenu(boolean disableMainConfigMenu) {
        isDisableMainConfigMenu = disableMainConfigMenu;
    }

    public boolean isCloseFoldMenu() {
        return isCloseFoldMenu;
    }

    public void setCloseFoldMenu(boolean foldMenu) {
        isCloseFoldMenu = foldMenu;
    }

    public boolean isIsDeepSeekVisibleTerminal() {
        return mIsDeepSeekVisibleTerminal;
    }

    public void setIsDeepSeekVisibleTerminal(boolean mIsDeepSeekVisibleTerminal) {
        this.mIsDeepSeekVisibleTerminal = mIsDeepSeekVisibleTerminal;
    }

    public String getDeepSeekApiUrl() {
        return mDeepSeekApiUrl;
    }

    public void setDeepSeekApiUrl(String mDeepSeekApiUrl) {
        this.mDeepSeekApiUrl = mDeepSeekApiUrl;
    }

    public String getDeepSeekApiKey() {
        return mDeepSeekApiKey;
    }

    public void setDeepSeekApiKey(String mDeepSeekApiKey) {
        this.mDeepSeekApiKey = mDeepSeekApiKey;
    }

    public String getCommandLink() {
        return mCommandLink;
    }

    public void setCommandLink(String mCommandLink) {
        this.mCommandLink = mCommandLink;
    }

    public String getServerJsonString() {
        return mServerJsonString;
    }

    public void setServerJsonString(String serverJsonString) {
        this.mServerJsonString = serverJsonString;
    }

    public boolean isInternalPassage() {
        return isInternalPassage;
    }

    public void setInternalPassage(boolean internalPassage) {
        isInternalPassage = internalPassage;
    }

    public boolean isShowCommand() {
        return isShowCommand;
    }

    public void setShowCommand(boolean showCommand) {
        isShowCommand = showCommand;
    }

    public boolean isResetVolume() {
        return isResetVolume;
    }

    public void setResetVolume(boolean resetVolume) {
        isResetVolume = resetVolume;
    }

    public boolean isSnowflakeShow() {
        return isSnowflakeShow;
    }

    public void setSnowflakeShow(boolean snowflakeShow) {
        isSnowflakeShow = snowflakeShow;
    }

    public boolean isRainShow() {
        return isRainShow;
    }

    public void setRainShow(boolean rainShow) {
        isRainShow = rainShow;
    }

    //是否显示下雨
    private boolean isRainShow = false;


    public boolean isOutputLOG() {
        return isOutputLOG;
    }

    public void setOutputLOG(boolean outputLOG) {
        isOutputLOG = outputLOG;
    }

    //小键盘规则
    private String numpad = TermuxPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS;

    public boolean isOpenDownloadFileServices() {
        return isOpenDownloadFileServices;
    }

    public void setOpenDownloadFileServices(boolean openDownloadFileServices) {
        isOpenDownloadFileServices = openDownloadFileServices;
    }

    public boolean isInputMethodTriggerClose() {
        return inputMethodTriggerClose;
    }

    public void setInputMethodTriggerClose(boolean inputMethodTriggerClose) {
        this.inputMethodTriggerClose = inputMethodTriggerClose;
    }

    public boolean isStyleTriggerOff() {
        return styleTriggerOff;
    }

    public void setStyleTriggerOff(boolean styleTriggerOff) {
        this.styleTriggerOff = styleTriggerOff;
    }

    public boolean isToolShow() {
        return isToolShow;
    }

    public void setToolShow(boolean toolShow) {
        isToolShow = toolShow;
    }

    public boolean isForceUseNumpad() {
        return forceUseNumpad;
    }

    public void setForceUseNumpad(boolean forceUseNumpad) {
        this.forceUseNumpad = forceUseNumpad;
    }

    public String getNumpad() {
        return numpad;
    }

    public void setNumpad(String numpad) {
        this.numpad = numpad;
    }
}
