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
        // 改进处理逻辑，包含几乎所有可打印字符，包括特殊字符
        // ASCII 9(Tab), 10(LF), 13(CR) 和 32-126 的可打印字符都保留
        // 同时保留所有可能的UTF-8字符（>= 128）
        if ((data[i] == 9) || (data[i] == 10) || (data[i] == 13) || 
            (data[i] >= 32 && data[i] <= 126) || // ASCII可打印字符，包括 . / 等特殊字符
            (data[i] >= 128)) {                  // 可能是UTF-8多字节字符的一部分
            current += (char)data[i];
        } else if (!current.empty()) {
            if (current.length() >= 2) { // 进一步降低最小长度阈值，甚至包括短密码
                result.push_back(current);
            }
            current.clear();
        }
    }
    if (!current.empty() && current.length() >= 2) {
        result.push_back(current);
    }
    
    // 简化日志
    LOG("提取到 %d 个可能的字符串", result.size());
    
    return result;
}

// 在内存数据中查找哈希值对应的原文
std::string findHashOriginalInMemory(const uint8_t* data, size_t dataLength, 
                                    const std::string& hashValue, 
                                    const std::string& hashType) {
    // 从数据中提取可能的文本
    std::vector<std::string> possibleTexts = extractPrintableStrings(data, dataLength);
    
    LOG("开始比较哈希值，目标类型: %s, 值: %s", hashType.c_str(), hashValue.c_str());
    
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
        
        // 将计算出的哈希转换为小写以便比较
        std::transform(calculatedHash.begin(), calculatedHash.end(), calculatedHash.begin(), 
                     [](unsigned char c){ return std::tolower(c); });
        
        // 比较哈希值
        if (calculatedHash == hashValue) {
            LOG("找到匹配的原文: '%s', 哈希值: %s", text.c_str(), calculatedHash.c_str());
            return text;
        }
    }
    
    LOG("未找到匹配的原文");
    // 如果没有找到匹配的原文，返回空字符串
    return "";
} 