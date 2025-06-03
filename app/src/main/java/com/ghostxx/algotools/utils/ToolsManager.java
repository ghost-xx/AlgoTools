package com.ghostxx.algotools.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ToolsManager {
    
    public static final String TAG = "ToolsManager";
    
    /**
     * 复制内存转储工具到/data/local/tmp目录
     * @param context 上下文
     */
    public static void copyDumpToolIfNeeded(Context context) {
        String abi = Build.SUPPORTED_ABIS[0];
        String assetDir;
        if (abi.contains("arm64")) assetDir = "arm64-v8a";
        else if (abi.contains("armeabi")) assetDir = "armeabi-v7a";
        else if (abi.contains("x86_64")) assetDir = "x86_64";
        else if (abi.contains("x86")) assetDir = "x86";
        else assetDir = "armeabi-v7a";
        String toolName = "dumpmm";
        File target = new File("/data/local/tmp/" + toolName);
        // 检查工具是否已存在且可执行
        boolean needCopy = true;
        if (target.exists()) {
            try {
                // 检查文件是否可执行
                java.lang.Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "test -x /data/local/tmp/" + toolName});
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    Log.i(TAG, "dumpmm工具已存在且可执行");
                    needCopy = false;
                } else {
                    Log.w(TAG, "dumpmm工具存在但不可执行，将重新复制");
                }
            } catch (Exception e) {
                Log.e(TAG, "检查dumpmm工具可执行性失败: " + e.getMessage());
            }
        }

        if (needCopy) {
            Log.i(TAG, "开始复制dumpmm工具...");
            try {
                // 1. 先复制到 app 私有 cache 目录
                File cacheFile = new File(context.getCacheDir(), toolName);
                InputStream is = null;
                try {
                    // 尝试从指定架构目录读取
                    is = context.getAssets().open(assetDir + "/" + toolName);
                    Log.i(TAG, "从 " + assetDir + " 目录找到dumpmm工具");
                } catch (IOException e) {
                    Log.w(TAG, "无法从 " + assetDir + " 目录读取dumpmm工具，尝试其他目录: " + e.getMessage());
                    // 尝试其他架构目录
                    String[] dirs = {"arm64-v8a", "armeabi-v7a", "x86_64", "x86"};
                    for (String dir : dirs) {
                        if (dir.equals(assetDir)) continue;
                        try {
                            is = context.getAssets().open(dir + "/" + toolName);
                            Log.i(TAG, "从 " + dir + " 目录找到dumpmm工具");
                            break;
                        } catch (IOException ignored) {
                            Log.d(TAG, "在 " + dir + " 目录中未找到dumpmm工具");
                        }
                    }
                    // 如果还是找不到，尝试直接从assets根目录读取
                    if (is == null) {
                        try {
                            is = context.getAssets().open(toolName);
                            Log.i(TAG, "从assets根目录找到dumpmm工具");
                        } catch (IOException ignored) {
                            Log.e(TAG, "在assets根目录中未找到dumpmm工具");
                        }
                    }
                }

                if (is == null) {
                    Log.e(TAG, "无法在assets中找到dumpmm工具");
                    Toast.makeText(context, "无法找到内存转储辅助工具，部分功能可能受限", Toast.LENGTH_LONG).show();
                    return;
                }

                FileOutputStream fos = new FileOutputStream(cacheFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                // 2. 用 root 权限移动到 /data/local/tmp 并赋予 777
                String cmd = "cp " + cacheFile.getAbsolutePath() + " /data/local/tmp/" + toolName + " && chmod 777 /data/local/tmp/" + toolName;
                Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                int exitCode = p.waitFor();
                if (exitCode == 0) {
                    Log.i(TAG, "成功复制dumpmm工具到/data/local/tmp并设置权限");
                } else {
                    Log.e(TAG, "复制dumpmm工具失败，退出码: " + exitCode);
                    // 尝试使用cat命令作为备用方案
                    cmd = "cat " + cacheFile.getAbsolutePath() + " > /data/local/tmp/" + toolName + " && chmod 777 /data/local/tmp/" + toolName;
                    p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
                    exitCode = p.waitFor();
                    if (exitCode == 0) {
                        Log.i(TAG, "使用cat命令成功复制dumpmm工具");
                    } else {
                        Log.e(TAG, "使用cat命令复制dumpmm工具失败，退出码: " + exitCode);
                        Toast.makeText(context, "复制内存转储辅助工具失败，部分功能可能受限", Toast.LENGTH_LONG).show();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "复制dumpmm工具出错: " + e.getMessage(), e);
                Toast.makeText(context, "复制内存转储辅助工具失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * 使用dumpmm工具转储指定进程的内存
     * @param context 上下文
     * @param packageName 要转储的应用包名，如果为null则使用当前应用
     * @param customOutputPath 自定义输出路径，如果为null则使用默认路径
     * @return 转储结果，成功返回输出文件路径，失败返回错误信息
     */
    public static String dumpProcessMemory(Context context, String packageName, String customOutputPath) {
        try {
            // 1. 获取进程PID
            int pid;
            if (packageName == null || packageName.isEmpty() || packageName.equals(context.getPackageName())) {
                // 获取当前进程PID
                pid = android.os.Process.myPid();
            } else {
                // 获取指定包名的PID
                pid = getPidByPackageName(packageName);
                if (pid <= 0) {
                    return "无法获取进程 " + packageName + " 的PID";
                }
            }
            
            // 使用PID进行转储
            return dumpProcessMemoryByPid(context, pid, customOutputPath);
            
        } catch (Exception e) {
            Log.e(TAG, "内存转储过程出错: " + e.getMessage(), e);
            return "内存转储出错: " + e.getMessage();
        }
    }
    
    /**
     * 使用dumpmm工具直接通过PID转储进程内存
     * @param context 上下文
     * @param pid 进程PID
     * @param customOutputPath 自定义输出路径，如果为null则使用默认路径
     * @return 转储结果，成功返回输出文件路径，失败返回错误信息
     */
    public static String dumpProcessMemoryByPid(Context context, int pid, String customOutputPath) {
        try {
            if (pid <= 0) {
                return "无效的PID: " + pid;
            }
            
            // 确定输出路径
            String outputPath;
            if (customOutputPath != null && !customOutputPath.isEmpty()) {
                outputPath = customOutputPath;
            } else {
                // 使用默认路径
                File externalDir = Environment.getExternalStorageDirectory();
                outputPath = externalDir.getAbsolutePath() + "/memory_data.bin";
            }
            
            // 执行转储命令
            String command = "su -c /data/local/tmp/dumpmm " + pid + " " + outputPath;
            Log.i(TAG, "执行内存转储命令: " + command);
            
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                Log.i(TAG, "内存转储成功，输出文件: " + outputPath);
                return "内存转储成功: " + outputPath;
            } else {
                // 读取错误输出
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                
                Log.e(TAG, "内存转储失败，退出码: " + exitCode + ", 错误: " + errorOutput.toString());
                return "内存转储失败: " + errorOutput.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "内存转储过程出错: " + e.getMessage(), e);
            return "内存转储出错: " + e.getMessage();
        }
    }
    
    /**
     * 执行shell命令并返回输出结果
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    public static String runShellCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        
        try {
            Log.d(TAG, "执行Shell命令: " + command);
            
            // 先尝试直接执行
            try {
                if (command.startsWith("su ") || command.startsWith("su -c ")) {
                    process = Runtime.getRuntime().exec(command);
                } else {
                    process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                }
            } catch (Exception e) {
                // 如果失败，尝试不使用su执行
                Log.w(TAG, "使用su执行命令失败: " + e.getMessage() + "，尝试不使用su执行");
                process = Runtime.getRuntime().exec(command);
            }
            
            // 读取标准输出
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            // 也读取错误输出
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                Log.w(TAG, "Shell命令错误输出: " + line);
            }
            
            // 等待命令执行完毕
            try {
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    Log.w(TAG, "Shell命令执行结束，退出码: " + exitCode);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "等待Shell命令执行被中断: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "执行Shell命令出错: " + e.getMessage(), e);
            output.append("错误: ").append(e.getMessage());
        } finally {
            // 关闭所有资源
            try {
                if (reader != null) reader.close();
                if (errorReader != null) errorReader.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                Log.e(TAG, "关闭Shell命令资源出错: " + e.getMessage());
            }
        }
        
        String result = output.toString();
        if (result.length() > 500) {
            Log.d(TAG, "Shell命令输出 (截断): " + result.substring(0, 500) + "...");
        } else {
            Log.d(TAG, "Shell命令输出: " + result);
        }
        
        return result;
    }
    
    /**
     * 根据包名获取进程PID
     * @param packageName 应用包名
     * @return 进程PID，如果未找到返回-1
     */
    private static int getPidByPackageName(String packageName) {
        try {
            // 先使用ps -A命令查找
            String psOutput = runShellCommand("ps -A | grep " + packageName);
            if (psOutput == null || psOutput.trim().isEmpty()) {
                // 尝试不带-A参数
                psOutput = runShellCommand("ps | grep " + packageName);
            }
            
            // 解析输出
            if (psOutput != null && !psOutput.trim().isEmpty()) {
                String[] lines = psOutput.split("\n");
                for (String line : lines) {
                    if (line.contains(packageName) && !line.contains("grep")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                return Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "解析PID失败: " + line, e);
                            }
                        }
                    }
                }
            }
            
            // 如果ps命令无法获取，尝试读取/proc目录
            String cmdOutput = runShellCommand("ls -l /proc | grep -E '[0-9]+' | grep -v self");
            String[] lines = cmdOutput.split("\n");
            
            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 8) {
                    try {
                        String dirName = parts[parts.length-1];
                        if (dirName.matches("\\d+")) {
                            int pid = Integer.parseInt(dirName);
                            // 读取进程的cmdline文件获取包名
                            String cmdlineOutput = runShellCommand("cat /proc/" + pid + "/cmdline");
                            if (cmdlineOutput != null && cmdlineOutput.trim().contains(packageName)) {
                                return pid;
                            }
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取进程PID出错: " + e.getMessage(), e);
        }
        
        return -1;
    }
} 