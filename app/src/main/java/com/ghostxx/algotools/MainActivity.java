package com.ghostxx.algotools;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ghostxx.algotools.fragment.HashAnalysisFragment;
import com.ghostxx.algotools.service.FloatingDumpService;
import com.ghostxx.algotools.service.ProcessMonitor;
import com.ghostxx.algotools.utils.ToolsManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityLOG";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_OVERLAY_PERMISSION = 2;
    private static final int REQUEST_USAGE_STATS_PERMISSION = 3;
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 4;
    private static final String PREF_SERVICE_ENABLED = "service_enabled";
    
    private SharedPreferences preferences;
    private MenuItem toggleServiceItem;
    private boolean isInitialLaunch = true;
    private ProcessMonitor processMonitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 设置工具栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    
        // 获取SharedPreferences
        preferences = getSharedPreferences("settings", MODE_PRIVATE);
        processMonitor = new ProcessMonitor(this);
        
        // 加载HashAnalysisFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HashAnalysisFragment())
                .commit();
        }
        
        // 复制转储工具
        boolean toolCopied = ToolsManager.copyDumpToolIfNeeded(this);
        if (!toolCopied) {
            Toast.makeText(this, "转储工具准备失败，部分功能可能不可用", Toast.LENGTH_LONG).show();
        }

        // 延迟500ms自动启动服务
        new Handler().postDelayed(() -> {
            if (isInitialLaunch) {
                isInitialLaunch = false;
                autoStartService();
            }
        }, 500);
    }

    private void autoStartService() {
        if (!isServiceRunning()) {
            Log.d(TAG, "Auto-starting service attempt via tryStartService.");
            tryStartService();
        } else {
            Log.d(TAG, "Auto-start skipped: Service already running.");
            if (toggleServiceItem != null) toggleServiceItem.setChecked(true);
            preferences.edit().putBoolean(PREF_SERVICE_ENABLED, true).apply();
        }
    }

    private void tryStartService() {
        if (!isServiceRunning()) {
            if (!checkStoragePermission()) {
                Log.d(TAG, "Storage permission pending.");
                return;
            }
            if (!checkOverlayPermission()) {
                Log.d(TAG, "Overlay permission pending.");
                return;
            }
            if (!processMonitor.hasUsageStatsPermission()) {
                Log.d(TAG, "Usage stats permission pending.");
                Toast.makeText(this, "请授予应用使用情况访问权限以获取前台应用信息", Toast.LENGTH_LONG).show();
                processMonitor.requestUsageStatsPermission();
                return;
            }

            Log.d(TAG, "All permissions granted, starting service.");
            startFloatingServiceActual();
        } else {
            Log.d(TAG, "Service already running, no need to start.");
            if (toggleServiceItem != null) toggleServiceItem.setChecked(true);
            preferences.edit().putBoolean(PREF_SERVICE_ENABLED, true).apply();
        }
    }

    private void startFloatingServiceActual() {
        Intent intent = new Intent(MainActivity.this, FloatingDumpService.class);
        startService(intent);
        Toast.makeText(this, "悬浮窗服务已启动", Toast.LENGTH_SHORT).show();
        if (toggleServiceItem != null) {
            toggleServiceItem.setChecked(true);
        }
        preferences.edit().putBoolean(PREF_SERVICE_ENABLED, true).apply(); // 保存服务状态
    
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        toggleServiceItem = menu.findItem(R.id.action_toggle_service);
        
        toggleServiceItem.setChecked(isServiceRunning());
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_toggle_service) {
            boolean newState = !item.isChecked();
            if (newState) {
                Log.d(TAG, "Menu toggle: attempting to start service.");
                tryStartService();
            } else {
                Log.d(TAG, "Menu toggle: stopping service.");
                stopFloatingService();
            }
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_exit) {
            exitApp();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 退出应用
     */
    private void exitApp() {
        new AlertDialog.Builder(this)
            .setTitle("确认退出")
            .setMessage("是否要退出应用？\n退出后悬浮窗服务也会停止。")
            .setPositiveButton("确定", (dialog, which) -> {
                stopFloatingService();
                preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
                finishAffinity();
                System.exit(0);
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (toggleServiceItem != null) {
            toggleServiceItem.setChecked(isServiceRunning());
        }
        
        boolean shouldBeEnabled = preferences.getBoolean(PREF_SERVICE_ENABLED, false);
        if (shouldBeEnabled && !isServiceRunning()) {
            Log.d(TAG, "onResume: Service should be enabled (pref=true) but isn't running. Attempting to start.");
            tryStartService();
        }
    }
    
    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("内存转储工具可以帮助您监控和分析Android应用的内存使用情况。\n\n" +
                    "使用方法：\n" +
                    "1. 应用启动后会自动开启悬浮窗服务\n" +
                    "2. 切换到目标应用\n" +
                    "3. 点击悬浮窗中的转储按钮\n" +
                    "4. 在 AlgoTools/dumps 目录下查看转储文件")
            .setPositiveButton("确定", null)
            .show();
    }
    
    /**
     * 检查存储权限
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
                    Toast.makeText(this, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting manage external storage permission", e);
                    Toast.makeText(this, "请求所有文件访问权限时出错", Toast.LENGTH_LONG).show();
                    // Optionally, direct user to settings manually if intent fails
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION); // General action
                    try {
                         startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
                         Toast.makeText(this, "请在此页面授予所有文件访问权限", Toast.LENGTH_LONG).show();
                    } catch (Exception e2){
                        Log.e(TAG, "Error requesting manage external storage permission again", e2);
                        Toast.makeText(this, "无法自动打开权限页面，请手动授予", Toast.LENGTH_LONG).show();
                    }
                }
                return false;
            }
            return true;
        } else {
            // Below Android 11
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
                return false;
            }
            return true;
        }
    }
    
    /**
     * 检查悬浮窗权限
     */
    private boolean checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请授予悬浮窗权限以使用内存转储功能", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            return false;
        }
        return true;
    }
    
    /**
     * 停止悬浮窗服务
     */
    private void stopFloatingService() {
        Intent intent = new Intent(MainActivity.this, FloatingDumpService.class);
        stopService(intent);
        Toast.makeText(this, "悬浮窗服务已停止", Toast.LENGTH_SHORT).show();
        if (toggleServiceItem != null) {
            toggleServiceItem.setChecked(false);
        }
        preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
    }
    
    /**
     * 检查服务是否正在运行
     */
    private boolean isServiceRunning() {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingDumpService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission granted after returning from settings. Attempting to start service.");
                tryStartService();
            } else {
                Toast.makeText(this, "没有获得悬浮窗权限，无法使用内存转储功能", Toast.LENGTH_LONG).show();
                if (toggleServiceItem != null) {
                    toggleServiceItem.setChecked(false);
                }
                preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
            }
        } else if (requestCode == REQUEST_USAGE_STATS_PERMISSION) {
            if (processMonitor.hasUsageStatsPermission()) {
                Log.d(TAG, "Usage stats permission seems granted after returning from settings. Attempting to start service.");
                tryStartService();
            } else {
                Toast.makeText(this, "没有获得应用使用情况访问权限，部分功能可能受限", Toast.LENGTH_LONG).show();
                if (toggleServiceItem != null) {
                    toggleServiceItem.setChecked(false);
                }
                preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
            }
        } else if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d(TAG, "Manage external storage permission granted. Attempting to start service.");
                    tryStartService();
                } else {
                    Toast.makeText(this, "需要所有文件访问权限才能运行服务", Toast.LENGTH_LONG).show();
                    if (toggleServiceItem != null) {
                        toggleServiceItem.setChecked(false);
                    }
                    preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            boolean allPermissionsGranted = true;
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        break;
                    }
                }
            } else {
                allPermissionsGranted = false; // No results, assume not granted
            }

            if (allPermissionsGranted) {
                Log.d(TAG, "Storage permissions granted. Attempting to start service.");
                tryStartService();
            } else {
                Toast.makeText(this, "需要读写存储权限才能执行内存转储", Toast.LENGTH_LONG).show();
                if (toggleServiceItem != null) {
                    toggleServiceItem.setChecked(false);
                }
                preferences.edit().putBoolean(PREF_SERVICE_ENABLED, false).apply();
            }
        }
    }
}