#include "memory_utils.h"
#include <android/log.h>
#include <functional>
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
        uint8_t c = data[i];
        // 优化处理逻辑：直接使用ASCII范围检查
        // 1. 所有ASCII可打印字符 (32-126)
        // 2. 制表符、换行符、回车符 (9,10,13)
        // 3. 所有可能的UTF-8多字节字符 (>=128)
        if ((c >= 32 && c <= 126) || // 所有ASCII可打印字符(包括所有特殊字符)
            c == 9 || c == 10 || c == 13 || // 制表符、换行符、回车符
            c >= 128) { // 可能的UTF-8多字节字符
            // 特别关注一些常用的特殊字符
            if (c == ',' || c == '?' || c == '.') {
                LOG("处理特殊字符: %c (ASCII: %d)", (char)c, c);
            }
            
            current += (char)c;
        } else if (!current.empty()) {
            // 当遇到非可打印字符时，结束当前字符串
            if (current.length() >= 2) {
                // 记录包含特殊字符的字符串
                if (current.find(',') != std::string::npos || current.find('?') != std::string::npos) {
                    LOG("提取包含特殊字符的字符串: '%s'", current.c_str());
                }
                result.push_back(current);
            }
            current.clear();
        }
    }
    
    // 处理最后一个字符串
    if (!current.empty() && current.length() >= 2) {
        if (current.find(',') != std::string::npos || current.find('?') != std::string::npos) {
            LOG("提取包含特殊字符的字符串: '%s'", current.c_str());
        }
        result.push_back(current);
    }
    
    LOG("提取到 %d 个可能的字符串", result.size());
    return result;
}

// 在内存数据中查找哈希值对应的原文
std::string findHashOriginalInMemory(const uint8_t* data, size_t dataLength, 
                                    const std::string& hashValue, 
                                    const std::string& hashType) {
    // 从数据中提取可能的文本
    std::vector<std::string> possibleTexts = extractPrintableStrings(data, dataLength);
    
    LOG("开始比较哈希值，目标类型: %s, 值: %s, 提取到 %zu 个可能的文本", 
        hashType.c_str(), hashValue.c_str(), possibleTexts.size());
    
    // 创建哈希计算函数指针，提高效率
    std::function<std::string(const std::string&)> hashFunc;
    
    // 根据哈希类型选择合适的哈希函数
    if (hashType == "MD5") {
        hashFunc = [](const std::string& text) {
            MD5 md5;
            return md5.calculate(text.c_str());
        };
    } else if (hashType == "SHA-1") {
        hashFunc = [](const std::string& text) {
            return hashing::sha1::hash(text);
        };
    } else if (hashType == "SHA-256") {
        hashFunc = [](const std::string& text) {
            return sha256(text);
        };
    } else if (hashType == "SHA-384") {
        hashFunc = [](const std::string& text) {
            return sha384(text);
        };
    } else if (hashType == "SHA-512") {
        hashFunc = [](const std::string& text) {
            return sha512(text);
        };
    } else {
        LOG("不支持的哈希类型: %s", hashType.c_str());
        return "";
    }
    
    // 对每个可能的文本计算哈希值并比较
    int processedCount = 0;
    for (const auto& text : possibleTexts) {
        // 每处理1000个文本记录一次进度
        if (++processedCount % 1000 == 0) {
            LOG("已处理 %d/%zu 个可能的文本", processedCount, possibleTexts.size());
        }
        
        // 计算哈希值
        std::string calculatedHash = hashFunc(text);
        
        // 将计算出的哈希转换为小写以便比较
        std::transform(calculatedHash.begin(), calculatedHash.end(), calculatedHash.begin(), 
                     [](unsigned char c){ return std::tolower(c); });
        
        // 比较哈希值
        if (calculatedHash == hashValue) {
            LOG("找到匹配的原文: '%s', 哈希值: %s", text.c_str(), calculatedHash.c_str());
            
            // 记录特殊字符
            if (text.find(',') != std::string::npos || text.find('?') != std::string::npos) {
                LOG("匹配的原文包含特殊字符");
                for (size_t i = 0; i < text.length(); i++) {
                    if (text[i] == ',' || text[i] == '?' || text[i] == '.') {
                        LOG("特殊字符位置[%zu]: '%c' (ASCII: %d)", i, text[i], (int)(unsigned char)text[i]);
                    }
                }
            }
            
            return text;
        }
    }
    
    LOG("处理完成，共检查了 %d 个可能的文本，未找到匹配", processedCount);
    return "";
} 