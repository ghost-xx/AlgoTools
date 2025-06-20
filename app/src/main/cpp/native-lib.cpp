#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>
#include "string_search.h"
#include "memory_utils.h"

// Define a log tag
#define LOG_TAG "JNI信息"

// 定义全局日志开关变量，供其他模块使用
bool gEnableJniLog = false; // 默认关闭JNI日志

// 更新LOG宏以检查gEnableJniLog
#define LOG(...)\
    if (gEnableJniLog) { \
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); \
    }

// 原生函数的实现
static void setJniLoggingEnabled_native(__attribute__((unused)) JNIEnv* env,
                                        __attribute__((unused)) jclass clazz, jboolean enabled) {
    gEnableJniLog = enabled;
    LOG("JNI 日志记录设置为: %s", enabled ? "开启" : "关闭")
}

// 使用增强版Boyer-Moore算法在内存块中搜索特征字符串
static jboolean containsFeatureString_native(JNIEnv* env, __attribute__((unused)) jclass clazz, 
                                            jbyteArray data, jint dataLength, jstring featureStr) {
    if (data == nullptr || featureStr == nullptr) {
        LOG("containsFeatureString_native: 输入为 null")
        return JNI_FALSE;
    }
    
    // 获取特征字符串
    const char* nativeFeatureStr = env->GetStringUTFChars(featureStr, nullptr);
    if (nativeFeatureStr == nullptr) {
        LOG("containsFeatureString_native: GetStringUTFChars 失败")
        return JNI_FALSE;
    }
    
    int featureLength = env->GetStringUTFLength(featureStr);
    if (featureLength == 0) {
        env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
        return JNI_TRUE; // 空特征字符串总是匹配
    }
    
    // 获取数据数组
    jbyte* nativeData = env->GetByteArrayElements(data, nullptr);
    if (nativeData == nullptr) {
        LOG("containsFeatureString_native: GetByteArrayElements 失败")
        env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
        return JNI_FALSE;
    }
    
    // 执行搜索
    bool found = searchInMemory(
        reinterpret_cast<const uint8_t*>(nativeData), 
        dataLength,
        reinterpret_cast<const uint8_t*>(nativeFeatureStr), 
        featureLength
    );
    
    // 释放资源
    env->ReleaseByteArrayElements(data, nativeData, JNI_ABORT);
    env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
    
    return found ? JNI_TRUE : JNI_FALSE;
}

// 在内存块中搜索哈希值对应的原文
static jstring findHashOriginal_native(JNIEnv* env, __attribute__((unused)) jclass clazz,
                                      jbyteArray data, jint dataLength, jstring hashValue,
                                      jstring hashType, jstring featureStr) {
    if (data == nullptr || hashValue == nullptr || hashType == nullptr) {
        LOG("findHashOriginal_native: 输入参数为 null")
        return nullptr;
    }
    
    // 获取哈希值
    const char* nativeHashValue = env->GetStringUTFChars(hashValue, nullptr);
    if (nativeHashValue == nullptr) {
        LOG("findHashOriginal_native: GetStringUTFChars 失败 (hashValue)")
        return nullptr;
    }
    
    // 转换哈希值为小写
    std::string hashValueStr(nativeHashValue);
    std::transform(hashValueStr.begin(), hashValueStr.end(), hashValueStr.begin(), 
                  [](unsigned char c){ return std::tolower(c); });
    
    // 获取哈希类型
    const char* nativeHashType = env->GetStringUTFChars(hashType, nullptr);
    if (nativeHashType == nullptr) {
        env->ReleaseStringUTFChars(hashValue, nativeHashValue);
        LOG("findHashOriginal_native: GetStringUTFChars 失败 (hashType)")
        return nullptr;
    }
    
    // 获取特征字符串（如果有）
    const char* nativeFeatureStr = nullptr;
    if (featureStr != nullptr) {
        nativeFeatureStr = env->GetStringUTFChars(featureStr, nullptr);
    }
    // 获取数据数组
    jbyte* nativeData = env->GetByteArrayElements(data, nullptr);
    if (nativeData == nullptr) {
        env->ReleaseStringUTFChars(hashValue, nativeHashValue);
        env->ReleaseStringUTFChars(hashType, nativeHashType);
        if (nativeFeatureStr != nullptr) {
            env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
        }
        LOG("findHashOriginal_native: GetByteArrayElements 失败")
        return nullptr;
    }
    
    // 如果有特征字符串，先检查是否包含它
    if (nativeFeatureStr != nullptr && strlen(nativeFeatureStr) > 0) {
        bool containsFeature = searchInMemory(
            reinterpret_cast<const uint8_t*>(nativeData),
            dataLength,
            reinterpret_cast<const uint8_t*>(nativeFeatureStr),
            strlen(nativeFeatureStr)
        );
        
        if (!containsFeature) {
            // 释放资源
            env->ReleaseByteArrayElements(data, nativeData, JNI_ABORT);
            env->ReleaseStringUTFChars(hashValue, nativeHashValue);
            env->ReleaseStringUTFChars(hashType, nativeHashType);
            env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
            return nullptr;
        }
    }
    
    // 启用JNI日志以帮助调试中文处理问题
    bool originalLogState = gEnableJniLog;
    gEnableJniLog = true;
    
    // 在内存中查找哈希值对应的原文
    std::string foundPlaintext = findHashOriginalInMemory(
        reinterpret_cast<const uint8_t*>(nativeData),
        dataLength,
        hashValueStr,
        nativeHashType
    );
    
    // 恢复日志状态
    gEnableJniLog = originalLogState;
    
    // 释放资源
    env->ReleaseByteArrayElements(data, nativeData, JNI_ABORT);
    env->ReleaseStringUTFChars(hashValue, nativeHashValue);
    env->ReleaseStringUTFChars(hashType, nativeHashType);
    if (nativeFeatureStr != nullptr) {
        env->ReleaseStringUTFChars(featureStr, nativeFeatureStr);
    }
    
    // 返回结果
    if (!foundPlaintext.empty()) {
        LOG("返回找到的原文: %s", foundPlaintext.c_str());
        return env->NewStringUTF(foundPlaintext.c_str());
    } else {
        LOG("未找到原文");
        return nullptr;
    }
}

// JNINativeMethod 数组，用于动态注册
static const JNINativeMethod gMethods[] = {
    {
        "setJniLoggingEnabled",
        "(Z)V",
        (void*)setJniLoggingEnabled_native
    },

    {
        "containsFeatureString",
        "([BILjava/lang/String;)Z",
        (void*)containsFeatureString_native
    },
    {
        "findHashOriginal",
        "([BILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        (void*)findHashOriginal_native
    }
};

// JNI_OnLoad 函数，在库加载时调用
jint JNI_OnLoad(JavaVM* vm, __attribute__((unused)) void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOG("JNI_OnLoad: GetEnv 失败")
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/ghostxx/algotools/utils/HashCryptoUtils");
    if (clazz == nullptr) {
        LOG("JNI_OnLoad: 找不到类 com/ghostxx/algotools/utils/HashCryptoUtils")
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        LOG("JNI_OnLoad: RegisterNatives 失败")
        env->DeleteLocalRef(clazz); // 注册失败时也需要释放
        return JNI_ERR;
    }
    
    env->DeleteLocalRef(clazz);
    LOG("JNI_OnLoad: 原生方法动态注册成功")
    return JNI_VERSION_1_6;
}



