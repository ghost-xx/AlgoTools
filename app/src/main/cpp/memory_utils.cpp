#include "memory_utils.h"
#include <android/log.h>
#include "md5.h"
#include "sha1.h"
#include "sha256.h"
#include "sha384.h"
#include "sha512.h"

// 外部声明日志开关变量
extern bool gEnableJniLog;

// 定义日志标签
#define LOG_TAG "MemoryUtils"

// 日志宏
#define LOG(...)\
    if (gEnableJniLog) { \
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); \
    }

// 从内存数据中提取可打印字符串
std::vector<std::string> extractPrintableStrings(const uint8_t* data, size_t length) {
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

// 在内存数据中查找哈希值对应的原文
std::string findHashOriginalInMemory(const uint8_t* data, size_t dataLength, 
                                    const std::string& hashValue, 
                                    const std::string& hashType) {
    // 从数据中提取可能的文本
    std::vector<std::string> possibleTexts = extractPrintableStrings(data, dataLength);
    
    // 对每个可能的文本计算哈希值并比较
    for (const auto& text : possibleTexts) {
        std::string calculatedHash;
        
        // 根据哈希类型计算哈希值
        if (hashType == "MD5") {
            MD5 md5;
            calculatedHash = md5.calculate(text.c_str());
        } else if (hashType == "SHA-1") {
            calculatedHash = hashing::sha1::hash(text);
        } else if (hashType == "SHA-256") {
            calculatedHash = sha256(text);
        } else if (hashType == "SHA-384") {
            calculatedHash = sha384(text);
        } else if (hashType == "SHA-512") {
            calculatedHash = sha512(text);
        }
        
        // 比较哈希值（不区分大小写）
        if (strcasecmp(calculatedHash.c_str(), hashValue.c_str()) == 0) {
            return text;
        }
    }
    
    // 如果没有找到匹配的原文，返回空字符串
    return "";
} 