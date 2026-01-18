package com.termux.zerocore.deepseek.model;

public class Model {
    private String baseUrl = "";
    private String modelName = "";
    private String api_key = "";

    public Model(String baseUrl, String modelName, String api_key) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.api_key = api_key;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApi_key() {
        return api_key;
    }
}
