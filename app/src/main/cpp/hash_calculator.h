#pragma once
#include <jni.h>

// 声明全局日志开关变量，在native-lib.cpp中定义
extern bool gEnableJniLog;

// MD5哈希计算
jstring calculateMD5_native(JNIEnv* env, jclass clazz, jstring input);

// SHA-1哈希计算
jstring calculateSHA1_native(JNIEnv* env, jclass clazz, jstring input);

// SHA-256哈希计算
jstring calculateSHA256_native(JNIEnv* env, jclass clazz, jstring input);

// SHA-384哈希计算
jstring calculateSHA384_native(JNIEnv* env, jclass clazz, jstring input);

// SHA-512哈希计算
jstring calculateSHA512_native(JNIEnv* env, jclass clazz, jstring input); 