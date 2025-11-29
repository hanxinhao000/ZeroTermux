package com.termux.zerocore.bean;

import java.io.Serializable;

public class SSHDeviceBean implements Serializable {
    private String alias;
    private String host;
    private int port = 22;
    private String username;
    private String password;
    private String keyPath;
    private String command;
    private boolean useKey = false;

    public SSHDeviceBean() {
    }

    public SSHDeviceBean(String alias, String host, int port, String username, String password, boolean useKey) {
        this.alias = alias;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.useKey = useKey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isUseKey() {
        return useKey;
    }

    public void setUseKey(boolean useKey) {
        this.useKey = useKey;
    }
    public String generateConnectCommand() {
        StringBuilder cmdBuilder = new StringBuilder();

        if (useKey) {
            cmdBuilder.append("ssh -p ").append(port).append(" ");
            String safeName = alias.replaceAll("[^a-zA-Z0-9_\\-]", "_");
            String autoKeyPath = "/data/data/com.termux/files/home/.ssh/" + safeName + ".key";
            cmdBuilder.append("-i ").append(autoKeyPath).append(" ");

        } else {
            if (password != null && !password.isEmpty()) {
                cmdBuilder.append("sshpass -p '").append(password).append("' ");
            }
            //sshpass -p '1314' ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -p 22 xinhao@192.168.31.121
            cmdBuilder.append("ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null").append(" -p ").append(port).append(" ");
        }
        cmdBuilder.append(username).append("@").append(host);
        return cmdBuilder.toString();
    }
}
