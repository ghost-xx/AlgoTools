package com.ghostxx.algotools.data.model;

/**
 * 应用进程数据传输对象
 */
public class AppProcessDto {
    private String packageName;
    private int pid;
    private String appName;
    
    public AppProcessDto() {
        // 默认构造函数，用于序列化/反序列化
    }
    
    public AppProcessDto(String packageName, int pid, String appName) {
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
        return "AppProcessDto{" +
                "packageName='" + packageName + '\'' +
                ", pid=" + pid +
                ", appName='" + appName + '\'' +
                '}';
    }
} 