package com.ghostxx.algotools.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.ghostxx.algotools.MainActivity;
import com.ghostxx.algotools.model.AppInfo;
import com.ghostxx.algotools.utils.ToolsManager;
import java.io.File;
import java.io.IOException;

/**
 * 悬浮窗内存转储服务
 * 负责显示悬浮窗并执行内存转储操作
 */
public class FloatingDumpService extends Service implements FloatingWindowManager.FloatingWindowCallback {
    private static final String TAG = "FloatingDumpService";
    
    private FloatingWindowManager floatingWindowManager;
    private ProcessMonitor processMonitor;
    private AppInfo currentApp;
    private String lastLoggedPackageName = "";
    private int lastLoggedPid = -1;
    private boolean isServiceRunning = true;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 1500; // 1秒
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化进程监控器
        processMonitor = new ProcessMonitor(this);
        
        // 初始化悬浮窗管理器
        floatingWindowManager = new FloatingWindowManager(this, this);
        floatingWindowManager.createFloatingWindow();
        
        // 复制转储工具
        ToolsManager.copyDumpToolIfNeeded(this);
        
        // 初始化并启动定时刷新
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isServiceRunning) {
                    AppInfo foregroundApp = processMonitor.getForegroundApp();
                    if (foregroundApp != null) {
                        String currentPackage = foregroundApp.getPackageName();
                        int currentPid = foregroundApp.getPid();
                        
                        // 检查应用状态是否发生变化
                        if (!currentPackage.equals(lastLoggedPackageName) || currentPid != lastLoggedPid) {
                            lastLoggedPackageName = currentPackage;
                            lastLoggedPid = currentPid;
                            refreshForegroundApp();
                        }
                    }
                    refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
                }
            }
        };
        refreshHandler.post(refreshRunnable);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 如果服务被系统杀死后重启，尝试恢复状态
        if (floatingWindowManager == null) {
            floatingWindowManager = new FloatingWindowManager(this, this);
            floatingWindowManager.createFloatingWindow();
        }
        if (refreshHandler == null) { // 确保定时器也恢复
            refreshHandler = new Handler(Looper.getMainLooper());
            refreshRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isServiceRunning) {
                        refreshForegroundApp();
                        refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
                    }
                }
            };
            refreshHandler.post(refreshRunnable);
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        isServiceRunning = false;
        
        // 移除悬浮窗
        if (floatingWindowManager != null) {
            floatingWindowManager.removeFloatingWindow();
            floatingWindowManager = null;
        }
        
        // 停止定时刷新
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
        
        showToast("内存转储服务已停止", Toast.LENGTH_SHORT);
    }
    
    /**
     * 刷新前台应用信息
     */
    private void refreshForegroundApp() {
        try {
            // 获取前台应用包名
            String packageName = processMonitor.getForegroundAppPackageName();
            
            if (packageName != null && !packageName.isEmpty()) {
                // 获取应用PID
                int pid = processMonitor.getPidByPackageName(packageName);
                String appName = processMonitor.getAppName(packageName); // 获取应用名，即使PID获取失败也显示应用名
                
                if (pid > 0) {
                    // 更新当前应用信息
                    currentApp = new AppInfo(packageName, pid, appName);
                    
                    // 更新悬浮窗显示 (仅应用名)
                    if (floatingWindowManager != null) {
                        floatingWindowManager.updateProcessInfo(appName, pid); // pid is passed but not displayed by WindowManager
                    }
                    // 仅当应用或PID变化时记录日志
                    if (!packageName.equals(lastLoggedPackageName) || pid != lastLoggedPid) {
                        if (floatingWindowManager != null) {
                            floatingWindowManager.appendLog("前台应用: " + appName + " (PID: " + pid + ")");
                        }
                        lastLoggedPackageName = packageName;
                        lastLoggedPid = pid;
                    }
                } else {
                    currentApp = null; // 无有效PID，重置currentApp
                    if (floatingWindowManager != null) {
                        floatingWindowManager.updateProcessInfo(appName + " (无PID)", 0);
                        if (!packageName.equals(lastLoggedPackageName) || lastLoggedPid != 0) { // 如果之前记录的不是无PID状态
                             floatingWindowManager.appendLog("应用: " + appName + " (无法获取PID)");
                             lastLoggedPackageName = packageName;
                             lastLoggedPid = 0; //标记为无PID状态
                        }
                    }
                }
            } else {
                currentApp = null; // 无前台应用，重置currentApp
                if (floatingWindowManager != null) {
                    floatingWindowManager.updateProcessInfo("无前台应用", 0);
                    if (!"".equals(lastLoggedPackageName)) { // 如果之前记录了某个应用
                        floatingWindowManager.appendLog("无前台应用");
                        lastLoggedPackageName = "";
                        lastLoggedPid = -1;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新前台应用信息时出错: " + e.getMessage(), e);
            if (floatingWindowManager != null) {
                 floatingWindowManager.appendLog("刷新错误: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDumpButtonClicked() {
        if (currentApp == null || currentApp.getPid() <= 0) {
            showToast("没有有效的应用信息，请等待自动刷新", Toast.LENGTH_SHORT);
            if (floatingWindowManager != null) {
                floatingWindowManager.ensureExpanded(); // 即使没有PID，也展开日志区显示提示
                floatingWindowManager.appendLog("无法转储：无有效应用或PID");
            }
            return;
        }
        
        // 确保展开状态以显示日志
        if (floatingWindowManager != null) {
        floatingWindowManager.ensureExpanded();
            floatingWindowManager.appendLog("开始转储 " + currentApp.getAppName() + " (PID: " + currentApp.getPid() + ") 的内存...");
        }
        
        // 执行内存转储
        new Thread(() -> {
            try {
                // 使用固定的文件名
                String fileName = "memory_dump.bin";
                File outputDir = new File(Environment.getExternalStorageDirectory(), "AlgoTools/dumps");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }
                String outputPath = new File(outputDir, fileName).getAbsolutePath();
                
                // 执行转储
                final String result = ToolsManager.dumpProcessMemoryByPid(
                        FloatingDumpService.this, 
                        currentApp.getPid(), 
                        outputPath
                );
                
                // 更新UI
                runOnUiThread(() -> {
                    if (floatingWindowManager != null) {
                    floatingWindowManager.appendLog(result);
                    }
                    showToast(result, Toast.LENGTH_LONG);
                    
                    // 如果转储成功，启动分析界面
                    if (result.startsWith("内存转储成功")) {
                        // 复制文件到应用私有目录
                        File privateFile = new File(getExternalFilesDir(null), "memory_data.bin");
                        try {
                            java.nio.file.Files.copy(
                                new File(outputPath).toPath(),
                                privateFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            );
                            
                            // 启动分析界面
                            Intent intent = new Intent(FloatingDumpService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } catch (IOException e) {
                            Log.e(TAG, "复制转储文件失败", e);
                            showToast("复制转储文件失败: " + e.getMessage(), Toast.LENGTH_LONG);
                        }
                    }
                });
            } catch (Exception e) {
                final String errorMsg = "内存转储失败: " + e.getMessage();
                Log.e(TAG, errorMsg, e);
                
                runOnUiThread(() -> {
                    if (floatingWindowManager != null) {
                    floatingWindowManager.appendLog(errorMsg);
                    }
                    showToast(errorMsg, Toast.LENGTH_LONG);
                });
            }
        }).start();
    }
  
    @Override
    public void onCloseButtonClicked() {
        // 停止服务
        stopSelf();
    }
    
    /**
     * 在UI线程上运行
     */
    private void runOnUiThread(Runnable runnable) {
        try {
            if (floatingWindowManager != null && isServiceRunning) {
                new android.os.Handler(getMainLooper()).post(() -> {
                    try {
                        if (isServiceRunning) {
                            runnable.run();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "在UI线程执行时出错", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "发布到UI线程时出错", e);
        }
    }

    /**
     * 安全地显示Toast消息
     */
    private void showToast(String message, int duration) {
        runOnUiThread(() -> {
            try {
                Toast.makeText(getApplicationContext(), message, duration).show();
            } catch (Exception e) {
                Log.e(TAG, "显示Toast时出错: " + message, e);
        }
        });
    }
}
