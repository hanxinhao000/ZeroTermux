package com.termux.zerocore.bean;

public class ZeroRunCommandBean {


    //0标题 1内容
    private int type;
    //标题
    private String title;
    //选项内容
    private String msg;
    //写入到文件名称
    private String fileName;
    //作者地址
    private String address;
    //资源名称
    private String assetsName;
    //显示文字
    private String name;
    //运行的命令
    private String runCommand;
    //是否显示
    private boolean isShow = true;
    //是否是网络命令
    private boolean isHttpCommand = false;
    //自定义事件
    private RunCommit mRunCommit;

    public RunCommit getRunCommit() {
        return mRunCommit;
    }

    public void setRunCommit(RunCommit mRunCommit) {
        this.mRunCommit = mRunCommit;
    }

    public interface RunCommit{

        public void run();

    }

    public boolean isHttpCommand() {
        return isHttpCommand;
    }

    public void setHttpCommand(boolean httpCommand) {
        isHttpCommand = httpCommand;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssetsName() {
        return assetsName;
    }

    public void setAssetsName(String assetsName) {
        this.assetsName = assetsName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
