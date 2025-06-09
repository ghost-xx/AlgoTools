#pragma once
#include <vector>
#include <string>
#include <cstdint>

// 从内存数据中提取可打印字符串
std::vector<std::string> extractPrintableStrings(const uint8_t* data, size_t length);

// 在内存数据中查找哈希值对应的原文
std::string findHashOriginalInMemory(const uint8_t* data, size_t dataLength, 
                                    const std::string& hashValue, 
                                    const std::string& hashType); 