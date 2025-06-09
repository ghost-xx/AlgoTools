#pragma once
#include <vector>
#include <sstream>
#include <iomanip>
#include <string>
#include <cstring>
#include <cstdint>
#include "sha512.h" // 使用SHA-512的核心功能

// Initial hash values H0-H7 for SHA-384 (different from SHA-512)
const uint64_t INITIAL_H384_VALUES[8] = {
    0xcbbb9d5dc1059ed8ULL, 0x629a292a367cd507ULL,
    0x9159015a3070dd17ULL, 0x152fecd8f70e5939ULL,
    0x67332667ffc00b31ULL, 0x8eb44a8768581511ULL,
    0xdb0c2e0d64f98fa7ULL, 0x47b5481dbefa4fa4ULL
};

// SHA-384 hashing function
// Uses the same compression function as SHA-512 but with different initial values
// and truncates the output to 384 bits (48 bytes, 96 hex chars)
inline std::string sha384(const std::string& input_string) {
    alignas(16) uint64_t current_H[8];
    std::memcpy(current_H, INITIAL_H384_VALUES, sizeof(current_H));
    
    const size_t input_length = input_string.length();
    const size_t total_blocks = (input_length + 17 + 127) / 128; // Calculate total blocks needed
    alignas(16) uint8_t final_blocks[256] = {0}; // Max 2 blocks needed for padding
    
    // Process full blocks directly from input
    const size_t full_blocks = input_length / 128;
    for (size_t i = 0; i < full_blocks; ++i) {
        compress_block_sha512(current_H, reinterpret_cast<const uint8_t*>(input_string.data() + i * 128));
    }
    
    // Handle remaining data and padding in final block(s)
    const size_t remaining = input_length % 128;
    std::memcpy(final_blocks, input_string.data() + full_blocks * 128, remaining);
    final_blocks[remaining] = 0x80;
    
    // Append length in bits as big-endian 128-bit integer
    const size_t length_pos = (remaining + 1 <= 112) ? 112 : 240;
    uint64_t length_high = 0;
    uint64_t length_low = input_length << 3;
    if (input_length > (0xFFFFFFFFFFFFFFFFULL / 8)) {
        length_high = input_length >> 61;
        length_low = input_length << 3;
    }
    
    uint64_t* length_ptr = reinterpret_cast<uint64_t*>(final_blocks + length_pos);
    length_ptr[0] = bswap64(length_high);
    length_ptr[1] = bswap64(length_low);
    
    // Process final block(s)
    compress_block_sha512(current_H, final_blocks);
    if (length_pos == 240) {
        compress_block_sha512(current_H, final_blocks + 128);
    }
    
    // Format output - SHA-384 only uses the first 6 of the 8 hash values (48 bytes, 96 hex chars)
    char hex[97]; // 96 characters + null terminator
    char* ptr = hex;
    for (int i = 0; i < 6; ++i) {
        uint64_t h = current_H[i];
        for (int j = 60; j >= 0; j -= 4) {
            *ptr++ = "0123456789abcdef"[(h >> j) & 0xF];
        }
    }
    *ptr = '\0';
    
    return std::string(hex);
} 