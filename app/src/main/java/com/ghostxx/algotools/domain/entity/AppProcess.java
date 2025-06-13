package com.ghostxx.algotools.domain.entity;

/**
 * 应用进程实体
 * 表示运行中的应用进程信息
 */
public class AppProcess {
    private final String packageName;
    private final int pid;
    private final String appName;
    
    public AppProcess(String packageName, int pid, String appName) {
        this.packageName = packageName;
        this.pid = pid;
        this.appName = appName;
    }
    
    /**
     * 获取应用包名
     */
    public String getPackageName() {
        return packageName;
    }
    
    /**
     * 获取进程ID
     */
    public int getPid() {
        return pid;
    }
    
    /**
     * 获取应用名称
     */
    public String getAppName() {
        return appName;
    }
    
    /**
     * 进程是否有效（PID > 0）
     */
    public boolean isValid() {
        return pid > 0;
    }
    
    /**
     * 创建一个表示无效进程的实例
     */
    public static AppProcess createInvalid(String packageName, String appName) {
        return new AppProcess(packageName, -1, appName);
    }
    
    @Override
    public String toString() {
        return appName + " (" + packageName + ", PID: " + pid + ")";
    }
} 