package com.ghostxx.algotools.service.processmonitor;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 使用Shell命令检测前台应用的策略实现
 */
public class ShellCommandStrategy implements ProcessDetectionStrategy {
    private static final String TAG = "ShellCommandStrategy";
    private Context context;
    private PackageFilterManager packageFilterManager;
    private boolean hasRootAccess = false;

    @Override
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.packageFilterManager = new PackageFilterManager();
        this.hasRootAccess = checkRootAccess();
    }

    @Override
    public String getForegroundAppPackageName() {
        if (!hasRootAccess) {
            Log.d(TAG, "无ROOT权限，Shell命令策略不可用");
            return "";
        }

        // 按优先级尝试不同的Shell命令方法
        String packageName = getPackageNameUsingWindowDumpsys();
        if (!packageName.isEmpty()) {
            return packageName;
        }

        packageName = getPackageNameUsingRecentsDumpsys();
        if (!packageName.isEmpty()) {
            return packageName;
        }

        packageName = getPackageNameUsingActivitiesDumpsys();
        if (!packageName.isEmpty()) {
            return packageName;
        }

        Log.d(TAG, "所有Shell命令方法均未获取到有效包名");
        return "";
    }

    @Override
    public int getPriority() {
        return 2; // 第二优先级
    }

    @Override
    public boolean isAvailable() {
        return hasRootAccess;
    }

    @Override
    public String getName() {
        return "ShellCommand";
    }

    /**
     * 使用 "dumpsys window windows" 命令获取前台应用包名
     */
    private String getPackageNameUsingWindowDumpsys() {
        try {
            java.lang.Process process = Runtime.getRuntime().exec("su -c dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("mCurrentFocus") || line.contains("mFocusedApp")) {
                    Log.d(TAG, "窗口焦点信息: " + line);
                    String extractedPart = ""; // 当前行提取到的非黑名单包名
                    if (line.contains("/")) {
                        String[] parts = line.split("/")[0].split(" ");
                        for (String part : parts) {
                            if (part.contains(".")) { // 基础检查，判断是否像包名
                                if (!packageFilterManager.isBlacklisted(part)) {
                                    extractedPart = part;
                                    Log.d(TAG, "Window dumpsys提取到候选包名: " + extractedPart);
                                    break; // 找到一个非黑名单的，跳出内层循环
                                } else {
                                    Log.d(TAG, "Window dumpsys提取到的包名在黑名单中: " + part);
                                }
                            }
                        }
                    }
                    if (!extractedPart.isEmpty()) {
                        reader.close();
                        process.waitFor();
                        return extractedPart;
                    }
                }
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "使用window dumpsys获取前台应用出错: " + e.getMessage(), e);
        }
        return "";
    }

    /**
     * 使用 "dumpsys activity recents" 命令获取前台应用包名
     */
    private String getPackageNameUsingRecentsDumpsys() {
        try {
            Log.d(TAG, "尝试使用dumpsys recents获取前台应用");
            java.lang.Process process = Runtime.getRuntime().exec("su -c dumpsys activity recents | grep 'Recent #0' -A2");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String tempCandidate = null;

            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "近期任务信息: " + line);
                if (line.contains(" packageName=")) {
                    String[] parts = line.trim().split("packageName=");
                    if (parts.length > 1) {
                        String extractedPkg = parts[1].split(" ")[0];
                        if (!packageFilterManager.isBlacklisted(extractedPkg)) {
                            tempCandidate = extractedPkg;
                            Log.d(TAG, "Recents dumpsys提取到候选包名: " + tempCandidate);
                            break; // 找到非黑名单包，跳出循环
                        } else {
                            Log.d(TAG, "Recents dumpsys提取到的包名在黑名单中: " + extractedPkg);
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();

            if (tempCandidate != null && !tempCandidate.isEmpty()) {
                return tempCandidate;
            }
        } catch (Exception e) {
            Log.e(TAG, "使用recents dumpsys获取前台应用出错: " + e.getMessage(), e);
        }
        return "";
    }

    /**
     * 使用 "dumpsys activity activities" 命令获取前台应用包名
     */
    private String getPackageNameUsingActivitiesDumpsys() {
        try {
            Log.d(TAG, "尝试使用dumpsys activities获取前台应用");
            java.lang.Process process = Runtime.getRuntime().exec("su -c dumpsys activity activities | grep mResumedActivity");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine(); // 此命令通常只关心第一行输出
            String tempCandidate = null;
            
            if (line != null) {
                Log.d(TAG, "已恢复活动信息: " + line);
                if (line.contains(" ")) {
                    String[] segments = line.split("\\s+");
                    for (String segment : segments) {
                        if (segment.contains("/") && segment.contains(".")) {
                            String extractedPkg = segment.split("/")[0];
                            if (!packageFilterManager.isBlacklisted(extractedPkg)) {
                                tempCandidate = extractedPkg;
                                Log.d(TAG, "Activities dumpsys提取到候选包名: " + tempCandidate);
                                break;
                            } else {
                                Log.d(TAG, "Activities dumpsys提取到的包名在黑名单中: " + extractedPkg);
                            }
                        }
                    }
                }
            }
            reader.close();
            process.waitFor();
            
            if (tempCandidate != null && !tempCandidate.isEmpty()) {
                return tempCandidate;
            }
        } catch (Exception e) {
            Log.e(TAG, "使用activities dumpsys获取前台应用出错: " + e.getMessage(), e);
        }
        return "";
    }

    /**
     * 检查是否有ROOT权限
     */
    private boolean checkRootAccess() {
        try {
            java.lang.Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            reader.close();
            process.waitFor();
            return output != null && output.contains("uid=0");
        } catch (Exception e) {
            Log.e(TAG, "检查ROOT权限时出错: " + e.getMessage(), e);
            return false;
        }
    }
} 