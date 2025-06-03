package com.ghostxx.algotools.model;

public class AppInfo {
    private String packageName;
    private int pid;
    private String appName;
    
    public AppInfo(String packageName, int pid, String appName) {
        this.packageName = packageName;
        this.pid = pid;
        this.appName = appName;
    }
    
    public String getPackageName() {
        return packageName;
    }
    
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    public int getPid() {
        return pid;
    }
    
    public void setPid(int pid) {
        this.pid = pid;
    }
    
    public String getAppName() {
        return appName;
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    @Override
    public String toString() {
        return appName + " (" + packageName + ", PID: " + pid + ")";
    }
} 