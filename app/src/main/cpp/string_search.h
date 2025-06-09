#pragma once
#include <cstdint>
#include <string>

// 使用Boyer-Moore算法在内存块中搜索特征字符串
bool searchInMemory(const uint8_t* data, int dataLength, const uint8_t* pattern, int patternLength);

// 预处理坏字符表
void precomputeBadCharTable(const uint8_t* pattern, int patternLength, int badCharTable[256]);

// 计算后缀数组
void computeSuffixes(const uint8_t* pattern, int patternLength, int* suffixes);

// 计算好后缀表
void precomputeGoodSuffixTable(const uint8_t* pattern, int patternLength, int* goodSuffixTable);

// 增强版Boyer-Moore搜索算法（使用坏字符规则和好后缀规则）
bool enhancedBoyerMooreSearch(const uint8_t* text, int textLength, 
                             const uint8_t* pattern, int patternLength);

// 使用SIMD指令加速字符串搜索（如果可用）
bool simdEnabledSearch(const uint8_t* text, int textLength, 
                      const uint8_t* pattern, int patternLength); 