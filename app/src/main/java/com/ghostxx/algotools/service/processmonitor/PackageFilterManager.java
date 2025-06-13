package com.ghostxx.algotools.service.processmonitor;

import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 包过滤管理器
 * 负责管理应用包名的黑名单/白名单过滤逻辑
 */
public class PackageFilterManager {
    private static final String TAG = "PackageFilterManager";

    // 包名黑名单，这些包名将被忽略
    private static final Set<String> PACKAGE_BLACKLIST = new HashSet<>(Arrays.asList(
            "android", // 系统级 "android" 包
            // 常见的输入法包名示例
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin", // Gboard
            "com.sohu.inputmethod.sogou", // 搜狗输入法
            "com.baidu.inputmethod_oppo", // 百度输入法 OPPO 版
            "com.baidu.inputmethod_vivo", // 百度输入法 VIVO 版
            "com.baidu.inputmethod_huawei", // 百度输入法华为版
            "com.baidu.inputmethod_xiaomi", // 百度输入法小米版
            "com.iflytek.inputmethod", // 讯飞输入法
            "com.touchtype.swiftkey", // SwiftKey
            "com.ghostxx.algotools", // 自身应用

            // 其他可能需要忽略的系统界面或服务
            "com.android.systemui",
            "com.miui.home"
    ));

    /**
     * 检查包名是否在黑名单中
     * @param packageName 要检查的包名
     * @return 如果在黑名单中则返回 true，否则返回 false
     */
    public boolean isBlacklisted(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        boolean blacklisted = PACKAGE_BLACKLIST.contains(packageName);
        if (blacklisted) {
            Log.d(TAG, "包名 '" + packageName + "' 在黑名单中。");
        }
        return blacklisted;
    }
    
    /**
     * 向黑名单添加包名
     * @param packageName 要添加的包名
     */
    public void addToBlacklist(String packageName) {
        if (packageName != null && !packageName.isEmpty()) {
            PACKAGE_BLACKLIST.add(packageName);
            Log.d(TAG, "已添加包名 '" + packageName + "' 到黑名单。");
        }
    }
    
    /**
     * 从黑名单移除包名
     * @param packageName 要移除的包名
     */
    public void removeFromBlacklist(String packageName) {
        if (packageName != null && PACKAGE_BLACKLIST.contains(packageName)) {
            PACKAGE_BLACKLIST.remove(packageName);
            Log.d(TAG, "已从黑名单移除包名 '" + packageName + "'。");
        }
    }
    
    /**
     * 获取黑名单包名集合的副本
     * @return 黑名单包名集合
     */
    public Set<String> getBlacklistedPackages() {
        return new HashSet<>(PACKAGE_BLACKLIST);
    }
} 