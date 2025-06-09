#include "string_search.h"
#include <algorithm>
#include <android/log.h>

// 外部声明日志开关变量
extern bool gEnableJniLog;

// 定义日志标签
#define LOG_TAG "StringSearch"

// 日志宏
#define LOG(...)\
    if (gEnableJniLog) { \
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); \
    }

// 使用Boyer-Moore算法在内存块中搜索特征字符串
bool searchInMemory(const uint8_t* data, int dataLength, const uint8_t* pattern, int patternLength) {
    return simdEnabledSearch(data, dataLength, pattern, patternLength);
}

// 预处理坏字符表
void precomputeBadCharTable(const uint8_t* pattern, int patternLength, int badCharTable[256]) {
    // 初始化坏字符表，默认跳过整个模式长度
    for (int i = 0; i < 256; i++) {
        badCharTable[i] = patternLength;
    }
    
    // 填充模式中每个字符的最右出现位置
    for (int i = 0; i < patternLength - 1; i++) {
        badCharTable[pattern[i]] = patternLength - 1 - i;
    }
}

// 计算后缀数组
void computeSuffixes(const uint8_t* pattern, int patternLength, int* suffixes) {
    suffixes[patternLength - 1] = patternLength;
    int f = 0, g = patternLength - 1;
    
    for (int i = patternLength - 2; i >= 0; --i) {
        if (i > g && suffixes[i + patternLength - 1 - f] < i - g) {
            suffixes[i] = suffixes[i + patternLength - 1 - f];
        } else {
            if (i < g) {
                g = i;
            }
            f = i;
            while (g >= 0 && pattern[g] == pattern[g + patternLength - 1 - f]) {
                --g;
            }
            suffixes[i] = f - g;
        }
    }
}

// 计算好后缀表
void precomputeGoodSuffixTable(const uint8_t* pattern, int patternLength, int* goodSuffixTable) {
    int* suffixes = new int[patternLength];
    
    // 计算后缀数组
    computeSuffixes(pattern, patternLength, suffixes);
    
    // 初始化好后缀表
    for (int i = 0; i < patternLength; ++i) {
        goodSuffixTable[i] = patternLength;
    }
    
    // 计算好后缀表的第一部分
    int j = 0;
    for (int i = patternLength - 1; i >= 0; --i) {
        if (suffixes[i] == i + 1) {
            for (; j < patternLength - 1 - i; ++j) {
                if (goodSuffixTable[j] == patternLength) {
                    goodSuffixTable[j] = patternLength - 1 - i;
                }
            }
        }
    }
    
    // 计算好后缀表的第二部分
    for (int i = 0; i < patternLength - 1; ++i) {
        goodSuffixTable[patternLength - 1 - suffixes[i]] = patternLength - 1 - i;
    }
    
    delete[] suffixes;
}

// 增强版Boyer-Moore搜索算法（使用坏字符规则和好后缀规则）
bool enhancedBoyerMooreSearch(const uint8_t* text, int textLength, 
                             const uint8_t* pattern, int patternLength) {
    if (patternLength == 0) return true;
    if (patternLength > textLength) return false;
    
    // 如果模式串很短（小于4字节），使用简单的暴力搜索，避免预处理开销
    if (patternLength < 4) {
        for (int i = 0; i <= textLength - patternLength; ++i) {
            bool found = true;
            for (int j = 0; j < patternLength; ++j) {
                if (text[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }
    
    // 预处理坏字符表和好后缀表
    int badCharTable[256];
    int* goodSuffixTable = new int[patternLength];
    
    precomputeBadCharTable(pattern, patternLength, badCharTable);
    precomputeGoodSuffixTable(pattern, patternLength, goodSuffixTable);
    int i = patternLength - 1; // 从文本的模式长度-1位置开始
    while (i < textLength) {
        int j = patternLength - 1;
        
        // 从右向左比较
        while (j >= 0 && pattern[j] == text[i - patternLength + 1 + j]) {
            --j;
        }
        // 如果j变为-1，说明找到了匹配
        if (j < 0) {
            delete[] goodSuffixTable;
            return true;
        }
        
        // 使用坏字符规则和好后缀规则中的最大值来跳过
        i += std::max(badCharTable[text[i - patternLength + 1 + j]] - patternLength + 1 + j, 
                     goodSuffixTable[j]);
    }
    
    delete[] goodSuffixTable;
    return false;
}

// 使用SIMD指令加速字符串搜索（如果可用）
bool simdEnabledSearch(const uint8_t* text, int textLength, 
                      const uint8_t* pattern, int patternLength) {
    // 这里可以添加使用SIMD指令的实现
    // 由于需要特定的CPU指令集支持，这里先使用增强版Boyer-Moore算法
    return enhancedBoyerMooreSearch(text, textLength, pattern, patternLength);
} 