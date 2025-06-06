#include "md5.h"
#include <iomanip>
#include <sstream>

// MD5转换要使用的常量表
const uint32_t k[64] = {
    0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee,
    0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
    0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
    0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
    0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
    0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
    0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
    0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
    0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
};

// 每轮循环要移位的位数
const int s[64] = {
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
    5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
    4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
};

MD5::MD5() {
    // 初始化状态
    state[0] = 0x67452301;
    state[1] = 0xefcdab89;
    state[2] = 0x98badcfe;
    state[3] = 0x10325476;
    
    count = 0;
    
    // 初始化填充数组
    padding[0] = 0x80;
    memset(padding + 1, 0, 63);
}

void MD5::transform(const uint8_t block[64]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
    uint32_t x[16];
    
    // 将64字节的块转换为16个32位的字
    for(int i = 0, j = 0; i < 16; ++i, j += 4)
        x[i] = ((uint32_t)block[j]) | (((uint32_t)block[j+1]) << 8) |
               (((uint32_t)block[j+2]) << 16) | (((uint32_t)block[j+3]) << 24);

    // 主循环
    for(int i = 0; i < 64; i++) {
        uint32_t f, g;
        
        if (i < 16) {
            f = F(b, c, d);
            g = i;
        } else if (i < 32) {
            f = G(b, c, d);
            g = (5*i + 1) % 16;
        } else if (i < 48) {
            f = H(b, c, d);
            g = (3*i + 5) % 16;
        } else {
            f = I(b, c, d);
            g = (7*i) % 16;
        }

        uint32_t temp = d;
        d = c;
        c = b;
        b = b + rotate_left(a + f + k[i] + x[g], s[i]);
        a = temp;
    }
    
    // 更新状态
    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
}

void MD5::update(const uint8_t* input, size_t length) {
    // 计算已经缓存的字节数
    size_t index = (count >> 3) & 0x3F;
    
    // 更新位数计数器
    count += (length << 3);
    
    // 需要填充的字节数
    size_t first = 64 - index;
    
    size_t i;
    
    // 如果有足够的字节，就转换它们
    if (length >= first) {
        memcpy(&buffer[index], input, first);
        transform(buffer);
        
        for (i = first; i + 63 < length; i += 64)
            transform(&input[i]);
            
        index = 0;
    } else
        i = 0;
    
    // 缓存剩余的输入
    memcpy(&buffer[index], &input[i], length-i);
}

void MD5::final(uint8_t digest[16]) {
    // 保存位数
    uint8_t bits[8];
    for(int i = 0; i < 8; i++)
        bits[i] = (count >> (i * 8)) & 0xFF;
    
    // 填充到56 mod 64
    size_t index = (count >> 3) & 0x3f;
    size_t padLen = (index < 56) ? (56 - index) : (120 - index);
    update(padding, padLen);
    
    // 追加长度
    update(bits, 8);
    
    // 存储状态到digest
    for(int i = 0; i < 4; i++) {
        digest[i*4] = state[i] & 0xFF;
        digest[i*4+1] = (state[i] >> 8) & 0xFF;
        digest[i*4+2] = (state[i] >> 16) & 0xFF;
        digest[i*4+3] = (state[i] >> 24) & 0xFF;
    }
}

std::string MD5::calculate(const std::string& input) {
    update((const uint8_t*)input.c_str(), input.length());
    
    uint8_t digest[16];
    final(digest);
    
    // 将结果转换为十六进制字符串
    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    for(unsigned char i : digest)
        ss << std::setw(2) << (int)i;
        
    return ss.str();
} 