package com.ghostxx.algotools.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.Arrays;
import android.util.Log;
// Import the new class if needed, or use fully qualified name
// import com.ghostxx.algotools.utils.HashSearcherTasks;

public class CryptoUtils {
    static {
        System.loadLibrary("algotools");
    }
    
    private static final String TAG = "CryptoUtils";
    private static boolean gEnableJniLog = false;

    /**
     * 计算字符串的MD5值
     * @param input 输入字符串
     * @return MD5哈希值（32位小写十六进制）
     */
    public static native String calculateMD5(String input);

    /**
     * 计算字符串的SHA-1值 (Native实现)
     * @param input 输入字符串
     * @return SHA-1哈希值（40位小写十六进制），如果计算失败则返回null
     */
    public static native String calculateSHA1(String input);

    /**
     * 计算字符串的SHA-256值 (Native实现)
     * @param input 输入字符串
     * @return SHA-256哈希值（64位小写十六进制），如果计算失败则返回null
     */
    public static native String calculateSHA256(String input);

    /**
     * 控制JNI层日志记录的启用状态。
     * @param enabled true启用日志，false禁用日志。
     */
    public static native void setJniLoggingEnabled(boolean enabled);

    /**
     * 公共方法，用于启用或禁用JNI层日志。
     * @param enable true则启用，false则禁用。
     */
    public static void enableJniLogging(boolean enable) {
        setJniLoggingEnabled(enable);
        gEnableJniLog = enable;
        if (gEnableJniLog) {
            Log.d(TAG, "JNI logging in Java set to: " + enable);
        }
    }
    
    /**
     * 在内存数据中查找哈希值对应的原文
     * @param hash 要查找的哈希值
     * @param memoryData 内存数据
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackMD5(String hash, byte[] memoryData) {
        if (hash == null || hash.length() != 32 || memoryData == null) {
            return null;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();//
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Callable<String>> tasks = new ArrayList<>();
        int chunkSize = memoryData.length / numThreads;
        int overlapSize = 1024;
        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numThreads - 1) ? memoryData.length : (i + 1) * chunkSize + overlapSize;
            if (end > memoryData.length) {
                end = memoryData.length;
            }
            if (start >= end && i < numThreads -1) {
                continue;
            }
             if (start >= end && i == numThreads -1 && tasks.isEmpty()){
                start = 0;
            } else if (start >=end) {
                 continue;
            }

            byte[] chunk = Arrays.copyOfRange(memoryData, start, end);
            tasks.add(new HashSearcherTasks.MD5Searcher(hash, chunk)); // Updated
        }
        
        if (tasks.isEmpty() && memoryData.length > 0) {
            tasks.add(new HashSearcherTasks.MD5Searcher(hash, memoryData)); // Updated
        }

        String result = null;
        try {
            List<Future<String>> futures = executorService.invokeAll(tasks, 300, TimeUnit.SECONDS);
            for (Future<String> future : futures) {
                if (Thread.currentThread().isInterrupted()) break;
                try {
                    String taskResult = future.get(50, TimeUnit.MILLISECONDS);
                    if (taskResult != null) {
                        result = taskResult;
                        break;
                    }
                } catch (TimeoutException e) {
                    // Task timed out, continue
                } catch (ExecutionException e) {
                    if (gEnableJniLog) Log.e(TAG, "MD5Searcher task ExecutionException", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (gEnableJniLog) Log.e(TAG, "MD5Searcher task InterruptedException", e);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (gEnableJniLog) Log.e(TAG, "crackMD5 interrupted during invokeAll", e);
        } finally {
            executorService.shutdownNow();
        }
        return result;
    }

    /**
     * 在内存数据中查找SHA-1哈希值对应的原文
     * @param hash 要查找的SHA-1哈希值 (40位十六进制)
     * @param memoryData 内存数据
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA1(String hash, byte[] memoryData) {
        if (hash == null || hash.length() != 40 || memoryData == null) {
            return null;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Callable<String>> tasks = new ArrayList<>();
        int chunkSize = memoryData.length / numThreads;
        int overlapSize = 1024;

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            int end = (i == numThreads - 1) ? memoryData.length : (i + 1) * chunkSize + overlapSize;
            if (end > memoryData.length) end = memoryData.length;
            if (start >= end && i < numThreads -1) continue;
            if (start >= end && i == numThreads -1 && tasks.isEmpty()){ start = 0;}
            else if (start >=end) continue;

            byte[] chunk = Arrays.copyOfRange(memoryData, start, end);
            tasks.add(new HashSearcherTasks.SHA1Searcher(hash, chunk)); // Updated
        }

        if (tasks.isEmpty() && memoryData.length > 0) {
            tasks.add(new HashSearcherTasks.SHA1Searcher(hash, memoryData)); // Updated
        }

        String result = null;
        try {
            List<Future<String>> futures = executorService.invokeAll(tasks, 300, TimeUnit.SECONDS);
            for (Future<String> future : futures) {
                if (Thread.currentThread().isInterrupted()) break;
                try {
                    String taskResult = future.get(50, TimeUnit.MILLISECONDS);
                    if (taskResult != null) {
                        result = taskResult;
                        break;
                    }
                } catch (TimeoutException e) {
                    // Task timed out
                } catch (ExecutionException e) {
                    if (gEnableJniLog) Log.e(TAG, "SHA1Searcher task ExecutionException", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (gEnableJniLog) Log.e(TAG, "SHA1Searcher task InterruptedException", e);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (gEnableJniLog) Log.e(TAG, "crackSHA1 interrupted during invokeAll", e);
        } finally {
            executorService.shutdownNow();
        }
        return result;
    }

    /**
     * 在内存数据中查找SHA-256哈希值对应的原文
     * @param hash 要查找的SHA-256哈希值 (64位十六进制)
     * @param memoryData 内存数据
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA256(String hash, byte[] memoryData) {
        if (hash == null || hash.length() != 64 || memoryData == null) {
            return null;
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Callable<String>> tasks = new ArrayList<>();
        int chunkSize = Math.max(1024*512, memoryData.length / numThreads); // Min 512KB or average
        if (memoryData.length < chunkSize) { 
             chunkSize = memoryData.length;
             numThreads = 1; 
        }
        int overlapSize = 256;

        for (int i = 0; i < numThreads; i++) {
            int start = i * chunkSize;
            if (start >= memoryData.length) {
                break;
            }
            int end = Math.min(memoryData.length, (i + 1) * chunkSize + overlapSize);
            if (i == numThreads - 1) { 
                 end = memoryData.length;
            }

            if (start >= end) {
                if (i == 0) { // Ensure at least one task if data exists
                    end = memoryData.length;
                    byte[] chunk = Arrays.copyOfRange(memoryData, start, end);
                    tasks.add(new HashSearcherTasks.SHA256Searcher(hash, chunk)); // Updated
                    break; 
                }
                continue;
            }

            byte[] chunk = Arrays.copyOfRange(memoryData, start, end);
            tasks.add(new HashSearcherTasks.SHA256Searcher(hash, chunk)); // Updated
        }
        
        if (tasks.isEmpty() && memoryData.length > 0) {
            tasks.add(new HashSearcherTasks.SHA256Searcher(hash, memoryData)); // Updated
        }

        String result = null;
        try {
            List<Future<String>> futures = executorService.invokeAll(tasks, 300, TimeUnit.SECONDS);
            for (Future<String> future : futures) {
                if (Thread.currentThread().isInterrupted()) break;
                try {
                    String taskResult = future.get(100, TimeUnit.MILLISECONDS);
                    if (taskResult != null) {
                        result = taskResult;
                        break;
                    }
                } catch (TimeoutException e) {
                    // Task timed out
                } catch (ExecutionException e) {
                    if (gEnableJniLog) Log.e(TAG, "SHA256Searcher task ExecutionException", e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (gEnableJniLog) Log.e(TAG, "SHA256Searcher task InterruptedException", e);
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (gEnableJniLog) Log.e(TAG, "crackSHA256 InterruptedException during invokeAll", e);
        } finally {
            executorService.shutdownNow();
        }
        return result;
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");

    /**
     * 识别哈希字符串可能的类型
     * @param hash 输入的哈希字符串
     * @return 包含可能哈希类型的列表 (例如 ["MD5", "SHA-1"])，如果无法识别则为空列表
     */
    public static List<String> identifyHashType(String hash) {
        List<String> possibleTypes = new ArrayList<>();
        if (hash == null || hash.isEmpty()) {
            return possibleTypes;
        }
        
        if (!HEX_PATTERN.matcher(hash).matches()) {
            return possibleTypes;
        }

        int len = hash.length();
        if (len == 32) {
            possibleTypes.add("MD5");
        }
        if (len == 40) {
            possibleTypes.add("SHA-1");
        }
        if (len == 64) {
            possibleTypes.add("SHA-256");
        }
        if (len == 128) {
            possibleTypes.add("SHA-512");
        }
        
        return possibleTypes;
    }
}