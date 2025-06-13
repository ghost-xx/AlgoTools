package com.ghostxx.algotools.data.repository;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.ghostxx.algotools.data.mapper.AppProcessMapper;
import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.repository.ProcessRepository;
import com.ghostxx.algotools.service.ProcessMonitor;

/**
 * 进程仓库实现
 */
public class ProcessRepositoryImpl implements ProcessRepository {
    
    private static final String TAG = "ProcessRepositoryImpl";
    
    private final Context context;
    private final ProcessMonitor processMonitor; // 临时使用旧的实现，后续可以直接使用ProcessDetector
    
    public ProcessRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.processMonitor = new ProcessMonitor(context);
    }
    
    @Override
    public AppProcess getForegroundProcess() {
        try {
            // 使用旧的ProcessMonitor，后续可以直接使用ProcessDetector
            com.ghostxx.algotools.model.AppInfo appInfo = processMonitor.getForegroundApp();
            return AppProcessMapper.fromLegacyAppInfo(appInfo);
        } catch (Exception e) {
            Log.e(TAG, "获取前台进程时出错", e);
            return null;
        }
    }
    
    @Override
    public AppProcess getProcessByPackageName(String packageName) {
        try {
            if (packageName == null || packageName.isEmpty()) {
                return null;
            }
            
            int pid = processMonitor.getPidByPackageName(packageName);
            String appName = processMonitor.getAppName(packageName);
            
            if (pid > 0) {
                return new AppProcess(packageName, pid, appName);
            } else {
                return AppProcess.createInvalid(packageName, appName);
            }
        } catch (Exception e) {
            Log.e(TAG, "根据包名获取进程时出错", e);
            return null;
        }
    }
    
    @Override
    public boolean hasUsageStatsPermission() {
        return processMonitor.hasUsageStatsPermission();
    }
    
    @Override
    public void requestUsageStatsPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "请求使用情况统计权限时出错", e);
        }
    }
} 