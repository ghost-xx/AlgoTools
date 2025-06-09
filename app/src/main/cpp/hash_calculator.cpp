#include <jni.h>
#include <string>
#include <android/log.h>
#include "md5.h"
#include "sha1.h"
#include "sha256.h"
#include "sha384.h"
#include "sha512.h"

// 定义日志标签
#define LOG_TAG "HashCalculator"

// 外部声明日志开关变量
extern bool gEnableJniLog;

// 日志宏
#define LOG(...)\
    if (gEnableJniLog) { \
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); \
    }

// MD5哈希计算
jstring calculateMD5_native(JNIEnv* env, jclass clazz, jstring input) {
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    LOG("正在为输入计算 MD5: %s", nativeString)
    MD5 md5;
    std::string result_str = md5.calculate(nativeString);
    LOG("MD5 结果: %s", result_str.c_str())
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// SHA-1哈希计算
jstring calculateSHA1_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA1_native: 输入字符串为 null")
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA1_native: GetStringUTFChars 失败")
        return nullptr; // GetStringUTFChars 失败可能因为内存不足
    }
    LOG("正在为输入计算 SHA1: %s", nativeString)
    std::string input_str(nativeString);
    std::string result_str = hashing::sha1::hash(input_str);
    LOG("SHA1 结果: %s", result_str.c_str())
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// SHA-256哈希计算
jstring calculateSHA256_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA256_native: 输入字符串为 null")
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA256_native: GetStringUTFChars 失败")
        return nullptr; // GetStringUTFChars 失败可能因为内存不足
    }
    LOG("正在为输入计算 SHA256: %s", nativeString)
    std::string input_str(nativeString);
    std::string result_str = sha256(input_str);
    LOG("SHA256 结果: %s", result_str.c_str())
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// SHA-384哈希计算
jstring calculateSHA384_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA384_native: 输入字符串为 null")
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA384_native: GetStringUTFChars 失败")
        return nullptr;
    }
    LOG("正在为输入计算 SHA384: %s", nativeString)
    std::string input_str(nativeString);
    std::string result_str = sha384(input_str);
    LOG("SHA384 结果: %s", result_str.c_str())
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
}

// SHA-512哈希计算
jstring calculateSHA512_native(JNIEnv* env, jclass clazz, jstring input) {
    if (input == nullptr) {
        LOG("calculateSHA512_native: 输入字符串为 null")
        return nullptr;
    }
    const char* nativeString = env->GetStringUTFChars(input, nullptr);
    if (nativeString == nullptr) {
        LOG("calculateSHA512_native: GetStringUTFChars 失败")
        return nullptr;
    }
    LOG("正在为输入计算 SHA512: %s", nativeString)
    std::string input_str(nativeString);
    std::string result_str = sha512(input_str);
    LOG("SHA512 结果: %s", result_str.c_str())
    env->ReleaseStringUTFChars(input, nativeString);
    return env->NewStringUTF(result_str.c_str());
} 