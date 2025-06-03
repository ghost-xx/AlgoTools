#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <android/log.h>
#include "md5.h"
#include "sha1.h"
#include "sha256.h"

// Define a log tag
#define LOG_TAG "JNI信息"

static bool gEnableJniLog = false; // 默认关闭JNI日志

// 更新LOG宏以检查gEnableJniLog
#define LOG(...)\
    if (gEnableJniLog) { \
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); \
    }

// 辅助函数：从内存数据中提取可打印字符串
__attribute__((unused)) std::vector<std::string> extractPrintableStrings(const uint8_t* data, size_t length) {
    std::vector<std::string> result;
    std::string current;
    
    for (size_t i = 0; i < length; i++) {
        if ((data[i] >= 32 && data[i] < 127) || data[i] < 0) {
            current += (char)data[i];
        } else if (!current.empty()) {
            if (current.length() >= 4) { // 只保留长度大于等于4的字符串
                result.push_back(current);
            }
            current.clear();
        }
    }
    if (!current.empty() && current.length() >= 4) {
        result.push_back(current);
    }
    return result;
}

// 原生函数的实现
static void setJniLoggingEnabled_native(JNIEnv* env, jclass clazz, jboolean enabled) {
    gEnableJniLog = enabled;
    LOG("JNI 日志记录设置为: %s", enabled ? "开启" : "关闭");
}

static jstring calculateMD5_native(JNIEnv* env, jclass clazz, jstring input) {
    const char* nativeString = env->GetStringUTFChars(input, 0);
    LOG("正在为输入计算 MD5: %s", nativeString)
    MD5 md5;
    std::string result_str = md5.calculate(nativeString);
    LOG("MD5 结果: %s", result_str.c_str());
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// 为 calculateSHA1 添加 JNI 实现
static jstring calculateSHA1_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA1_native: 输入字符串为 null");
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA1_native: GetStringUTFChars 失败");
        return nullptr; // GetStringUTFChars 失败可能因为内存不足
    }
    LOG("正在为输入计算 SHA1: %s", nativeString);
    std::string input_str(nativeString);
    std::string result_str = hashing::sha1::hash(input_str);
    LOG("SHA1 结果: %s", result_str.c_str());
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// 为 calculateSHA256 添加 JNI 实现
static jstring calculateSHA256_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA256_native: 输入字符串为 null");
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA256_native: GetStringUTFChars 失败")
        return nullptr; // GetStringUTFChars 失败可能因为内存不足
    }
    LOG("正在为输入计算 SHA256: %s", nativeString)
    std::string input_str(nativeString);
    SHA256 sha256;
    std::string result_str = sha256(input_str);
    LOG("SHA256 结果: %s", result_str.c_str());
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// JNINativeMethod 数组，用于动态注册
static const JNINativeMethod gMethods[] = {
    {
        "setJniLoggingEnabled",
        "(Z)V",
        (void*)setJniLoggingEnabled_native
    },
    {
        "calculateMD5",
        "(Ljava/lang/String;)Ljava/lang/String;",
        (void*)calculateMD5_native
    },
    {
        "calculateSHA1",
        "(Ljava/lang/String;)Ljava/lang/String;",
        (void*)calculateSHA1_native
    },
    {
        "calculateSHA256",
        "(Ljava/lang/String;)Ljava/lang/String;",
        (void*)calculateSHA256_native
    }
};

// JNI_OnLoad 函数，在库加载时调用
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        LOG("JNI_OnLoad: GetEnv 失败")
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/ghostxx/algotools/utils/CryptoUtils");
    if (clazz == nullptr) {
        LOG("JNI_OnLoad: 找不到类 com/ghostxx/algotools/utils/CryptoUtils");
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

// JNIEXPORT jstring JNICALL
// Java_com_ghostxx_algotools_utils_CryptoUtils_crackMD5(...)
// (如果需要动态注册crackMD5，也需要按类似方式添加)

