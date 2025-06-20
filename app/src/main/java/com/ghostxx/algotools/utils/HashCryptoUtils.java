package com.ghostxx.algotools.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HashCryptoUtils {
    private static final String TAG = "HashCryptoUtils";
    
    static {
        try {
        System.loadLibrary("algotools");
            Log.i(TAG, "algotools loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load algotools", e);
        }
    }

    /**
     * 获取系统可用的处理器核心数
     * @return CPU核心数
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取当前线程池的并行度（线程数）
     * @return 当前线程数
     */
    public static int getThreadCount() {
        // 使用可用处理器数量的2倍作为并行度
        return Math.max(2, Runtime.getRuntime().availableProcessors() * 2);
    }

    /**
     * 使用Boyer-Moore算法在数据块中搜索特征字符串 (Native实现)
     * @param data 要搜索的数据块
     * @param dataLength 数据块的长度
     * @param featureString 要搜索的特征字符串
     * @return 如果找到特征字符串返回true，否则返回false
     */
    public static native boolean containsFeatureString(byte[] data, int dataLength, String featureString);

    /**
     * 在内存块中搜索哈希值对应的原文 (Native实现)
     * @param data 要搜索的数据块
     * @param dataLength 数据块的长度
     * @param hashValue 要查找的哈希值
     * @param hashType 哈希类型（"MD5", "SHA-1", "SHA-256", "SHA-512"）
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static native String findHashOriginal(byte[] data, int dataLength, String hashValue, 
                                               String hashType, String featureString);

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
        if (enable) {
            Log.d(TAG, "JNI logging in Java set to: " + enable);
        }
    }
    
    // 修改正则表达式以匹配大小写十六进制字符
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
        if (len == 96) {
            possibleTypes.add("SHA-384");
        }
        if (len == 128) {
            possibleTypes.add("SHA-512");
        }
        
        return possibleTypes;
    }

}
