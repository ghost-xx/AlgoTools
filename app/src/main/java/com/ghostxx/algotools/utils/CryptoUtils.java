package com.ghostxx.algotools.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import android.util.Log;

public class CryptoUtils {
    static {
        System.loadLibrary("algotools");
    }
    
    private static final String TAG = "CryptoUtils";

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
     * 获取当前活跃线程数（包括正在运行和等待的线程）
     * @return 当前活跃的线程数，至少返回1
     */
    public static int getActiveThreadCount() {
        // 由于不再使用ForkJoinPool，返回估计值
        return Math.max(1, Runtime.getRuntime().availableProcessors());
    }


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
     * 计算字符串的SHA-512值 (Native实现)
     * @param input 输入字符串
     * @return SHA-512哈希值（128位小写十六进制），如果计算失败则返回null
     */
    public static native String calculateSHA512(String input);

    /**
     * 计算字符串的SHA-384值 (Native实现)
     * @param input 输入字符串
     * @return SHA-384哈希值（96位小写十六进制），如果计算失败则返回null
     */
    public static native String calculateSHA384(String input);

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
     * 标准化哈希值（转换为小写）
     * @param hash 输入的哈希值
     * @return 标准化后的哈希值（小写形式）
     */
    private static String normalizeHash(String hash) {
        return hash != null ? hash.toLowerCase() : null;
    }

    /**
     * 验证哈希值格式是否正确
     * @param hash 要验证的哈希值
     * @param expectedLength 期望的长度
     * @return 如果格式正确返回标准化的哈希值（小写），否则返回null
     */
    private static String validateHash(String hash, int expectedLength) {
        if (hash == null || hash.length() != expectedLength) {
            return null;
        }
        
        // 验证是否为有效的十六进制字符串（支持大小写）
        if (!HEX_PATTERN.matcher(hash).matches()) {
            return null;
        }
        
        return normalizeHash(hash);
    }

    /**
     * 使用原生方法在内存数据中查找哈希值对应的原文
     * @param hash 要查找的哈希值
     * @param memoryData 内存数据
     * @param hashType 哈希类型（"MD5", "SHA-1", "SHA-256", "SHA-512"）
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String findOriginalTextNative(String hash, byte[] memoryData, String hashType, String featureString) {
        if (hash == null || memoryData == null || hashType == null) {
            return null;
        }
        // 标准化哈希值
        hash = hash.toLowerCase();
        // 调用原生方法
        return findHashOriginal(memoryData, memoryData.length, hash, hashType, featureString);
    }
    
    /**
     * 在内存数据中查找MD5哈希值对应的原文
     * @param hash 要查找的哈希值（32位十六进制，大小写均可）
     * @param memoryData 内存数据
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackMD5(String hash, byte[] memoryData, String featureString) {
        hash = validateHash(hash, 32);
        if (hash == null || memoryData == null) {
            return null;
        }
        return findOriginalTextNative(hash, memoryData, "MD5", featureString);
    }
    
    /**
     * 在内存数据中查找SHA-1哈希值对应的原文
     * @param hash 要查找的SHA-1哈希值（40位十六进制，大小写均可）
     * @param memoryData 内存数据
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA1(String hash, byte[] memoryData, String featureString) {
        hash = validateHash(hash, 40);
        if (hash == null || memoryData == null) {
            return null;
        }
        return findOriginalTextNative(hash, memoryData, "SHA-1", featureString);
    }
    
    /**
     * 在内存数据中查找SHA-256哈希值对应的原文
     * @param hash 要查找的SHA-256哈希值（64位十六进制，大小写均可）
     * @param memoryData 内存数据
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA256(String hash, byte[] memoryData, String featureString) {
        hash = validateHash(hash, 64);
        if (hash == null || memoryData == null) {
            return null;
        }
        return findOriginalTextNative(hash, memoryData, "SHA-256", featureString);
    }
    
    /**
     * 在内存数据中查找SHA-512哈希值对应的原文
     * @param hash 要查找的SHA-512哈希值（128位十六进制，大小写均可）
     * @param memoryData 内存数据
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA512(String hash, byte[] memoryData, String featureString) {
        hash = validateHash(hash, 128);
        if (hash == null || memoryData == null) {
            return null;
        }
        return findOriginalTextNative(hash, memoryData, "SHA-512", featureString);
    }

    /**
     * 在内存数据中查找SHA-384哈希值对应的原文
     * @param hash 要查找的SHA-384哈希值（96位十六进制，大小写均可）
     * @param memoryData 内存数据
     * @param featureString 特征字符串（可选，为空则不使用）
     * @return 如果找到返回原文，否则返回null
     */
    public static String crackSHA384(String hash, byte[] memoryData, String featureString) {
        hash = validateHash(hash, 96);
        if (hash == null || memoryData == null) {
            return null;
        }
        return findOriginalTextNative(hash, memoryData, "SHA-384", featureString);
    }

    // 为了向后兼容，保留原有的方法
    public static String crackMD5(String hash, byte[] memoryData) {
        return crackMD5(hash, memoryData, "");
    }

    public static String crackSHA1(String hash, byte[] memoryData) {
        return crackSHA1(hash, memoryData, "");
    }

    public static String crackSHA256(String hash, byte[] memoryData) {
        return crackSHA256(hash, memoryData, "");
    }

    public static String crackSHA512(String hash, byte[] memoryData) {
        return crackSHA512(hash, memoryData, "");
    }

    // 为了向后兼容，添加无特征字符串参数的方法
    public static String crackSHA384(String hash, byte[] memoryData) {
        return crackSHA384(hash, memoryData, "");
    }

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
