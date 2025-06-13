package com.ghostxx.algotools.data.repository;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.ghostxx.algotools.domain.entity.AppProcess;
import com.ghostxx.algotools.domain.entity.MemoryDump;
import com.ghostxx.algotools.domain.repository.MemoryDumpRepository;
import com.ghostxx.algotools.utils.ToolsManager;

import java.io.File;
import java.util.Date;

/**
 * 内存转储仓库实现
 */
public class MemoryDumpRepositoryImpl implements MemoryDumpRepository {
    
    private static final String TAG = "MemoryDumpRepoImpl";
    private static final String DUMPS_DIR = "AlgoTools/dumps";
    
    private final Context context;
    
    public MemoryDumpRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
    }
    
    @Override
    public MemoryDump dumpProcessMemory(AppProcess process) {
        if (process == null || !process.isValid()) {
            Log.e(TAG, "无效的进程信息");
            return null;
        }
        
        try {
            // 使用固定的文件名
            String fileName = "memory_dump.bin";
            File outputDir = new File(Environment.getExternalStorageDirectory(), DUMPS_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            String outputPath = new File(outputDir, fileName).getAbsolutePath();
            
            // 执行转储
            String result = ToolsManager.dumpProcessMemoryByPid(context, process.getPid(), outputPath);
            
            if (result.startsWith("内存转储成功")) {
                File dumpFile = new File(outputPath);
                // 创建内存转储实体
                return new MemoryDump(
                        outputPath,
                        dumpFile.length(),
                        new Date(),
                        process
                );
            } else {
                Log.e(TAG, "转储失败: " + result);
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "执行内存转储时出错", e);
            return null;
        }
    }
    
    @Override
    public MemoryDump getLatestDump() {
        try {
            // 检查私有文件夹中的副本
            File privateFile = new File(context.getExternalFilesDir(null), "memory_data.bin");
            if (privateFile.exists() && privateFile.length() > 0) {
                return new MemoryDump(
                        privateFile.getAbsolutePath(),
                        privateFile.length(),
                        new Date(privateFile.lastModified()),
                        null // 无源进程信息
                );
            }
            
            // 检查公共文件夹
            File publicDir = new File(Environment.getExternalStorageDirectory(), DUMPS_DIR);
            File[] files = publicDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".bin"));
            
            if (files != null && files.length > 0) {
                // 按修改时间降序排序
                File latestFile = null;
                long latestTime = 0;
                
                for (File file : files) {
                    if (file.lastModified() > latestTime) {
                        latestTime = file.lastModified();
                        latestFile = file;
                    }
                }
                
                if (latestFile != null) {
                    return new MemoryDump(
                            latestFile.getAbsolutePath(),
                            latestFile.length(),
                            new Date(latestFile.lastModified()),
                            null // 无源进程信息
                    );
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取最新转储文件时出错", e);
            return null;
        }
    }
    
    @Override
    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11及以上版本
            return Environment.isExternalStorageManager();
        } else {
            // Android 10及以下版本
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    @Override
    public void requestStoragePermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11及以上版本
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } 
            // 对于Android 10及以下版本，需要在Activity中请求权限
        } catch (Exception e) {
            Log.e(TAG, "请求存储权限时出错", e);
        }
    }
    
    @Override
    public boolean needsCopyDumpTool() {
        return ToolsManager.needCopyDumpTool(context);
    }
    
    @Override
    public boolean copyDumpTool() {
        return ToolsManager.copyDumpToolIfNeeded(context);
    }
} 