package com.ghostxx.algotools.repository.fileprocessing;

import android.util.Log;

import com.ghostxx.algotools.utils.HashCryptoUtils;

/**
 * 哈希搜索处理器
 * 在文件块中搜索哈希值对应的原文
 */
public class HashSearchProcessor implements FileChunkProcessor {
    private static final String TAG = "HashSearchProcessor";
    
    private final String hashToCrack;
    private final String featureString;
    private final String hashType;
    
    /**
     * 构造函数
     * @param hashToCrack 要破解的哈希值
     * @param featureString 特征字符串，用于缩小搜索范围
     * @param hashType 哈希类型（如MD5、SHA-1等）
     */
    public HashSearchProcessor(String hashToCrack, String featureString, String hashType) {
        this.hashToCrack = hashToCrack;
        this.featureString = featureString;
        this.hashType = hashType;
    }
    
    @Override
    public String processChunk(byte[] data, int dataSize) {
        // 如果提供了特征字符串，先检查数据是否包含该字符串
        if (featureString != null && !featureString.isEmpty()) {
            boolean containsFeature = HashCryptoUtils.containsFeatureString(data, dataSize, featureString);
            if (!containsFeature) {
                return null; // 不包含特征字符串，跳过
            }
        }
        
        // 在数据中查找哈希值的原文
        String result = HashCryptoUtils.findHashOriginal(data, dataSize, hashToCrack, hashType, featureString);
        if (result != null) {
            Log.d(TAG, "找到哈希值 " + hashToCrack + " 的原文: " + result);
        }
        return result;
    }
    
    @Override
    public String getName() {
        return "HashSearch-" + hashType;
    }
} 