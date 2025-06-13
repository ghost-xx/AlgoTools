#ifndef ALGOTOOLS_MD5_H
#define ALGOTOOLS_MD5_H

#include <string>
#include <cstring>
#include <cstdint>

class MD5 {
public:
    MD5();
    std::string calculate(const std::string& input);

    void update(const uint8_t* input, size_t length);

    void final(uint8_t digest[16]);

private:
    // MD5的四个寄存器
    uint32_t state[4]{};
    // 输入bits数
    uint64_t count;
    // 输入缓冲区
    uint8_t buffer[64]{};
    // 填充用数据
    uint8_t padding[64]{};

    void transform(const uint8_t block[64]);

    // 辅助函数
    static uint32_t F(uint32_t x, uint32_t y, uint32_t z) { return (x & y) | (~x & z); }
    static uint32_t G(uint32_t x, uint32_t y, uint32_t z) { return (x & z) | (y & ~z); }
    static uint32_t H(uint32_t x, uint32_t y, uint32_t z) { return x ^ y ^ z; }
    static uint32_t I(uint32_t x, uint32_t y, uint32_t z) { return y ^ (x | ~z); }
    static uint32_t rotate_left(uint32_t x, int n) { return (x << n) | (x >> (32 - n)); }
};

#endif