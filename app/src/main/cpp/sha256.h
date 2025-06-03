#pragma once
#include <vector>
#include <sstream>
#include <iomanip>
#include <string> // For std::string
#include <cstring> // For std::memcpy
#include <cstdint> // For uint32_t, uint64_t

// SHA-256 constants K
const uint32_t K_CONST[64] = { // Renamed to avoid conflict if K is used elsewhere
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
};
// Initial hash values H0-H7 for SHA-256
const uint32_t INITIAL_H_VALUES[8] = {
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
};

// Helper functions for SHA-256 (inline for header-only)
inline uint32_t right_rotate(uint32_t x, uint32_t n) {
    return (x >> n) | (x << (32 - n));
}

inline uint32_t little_sigma_0(uint32_t x) {
    return right_rotate(x, 7) ^ right_rotate(x, 18) ^ (x >> 3);
}

inline uint32_t little_sigma_1(uint32_t x) {
    return right_rotate(x, 17) ^ right_rotate(x, 19) ^ (x >> 10);
}

inline uint32_t big_sigma_0(uint32_t x) {
    return right_rotate(x, 2) ^ right_rotate(x, 13) ^ right_rotate(x, 22);
}

inline uint32_t big_sigma_1(uint32_t x) {
    return right_rotate(x, 6) ^ right_rotate(x, 11) ^ right_rotate(x, 25);
}

inline void message_schedule(uint32_t W[64], const uint8_t block[64]) {
    for (int i = 0; i < 16; i++) {
        W[i] =  ( (uint32_t)block [i * 4    ] << 24) |
                ( (uint32_t)block [i * 4 + 1] << 16) |
                ( (uint32_t)block [i * 4 + 2] <<  8) |
                ( (uint32_t)block [i * 4 + 3]);
    }
    for (int i = 16; i < 64; i++) {
        W[i] =  little_sigma_1(W[i - 2])  + W[i - 7] +
                little_sigma_0(W[i - 15]) + W[i - 16];
    }
}

inline uint32_t choice(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (~x & z);
}

inline uint32_t majority(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

inline void compress_block_sha256(uint32_t H_state[8], const uint8_t block[64]) {
    uint32_t W[64];
    message_schedule(W, block);

    uint32_t temp_h[8];
    for(int i=0; i<8; ++i) temp_h[i] = H_state[i];

    for (int i = 0; i < 64; i++) {
        uint32_t S1 = big_sigma_1(temp_h[4]);
        uint32_t ch = choice(temp_h[4], temp_h[5], temp_h[6]);
        uint32_t temp1 = temp_h[7] + S1 + ch + K_CONST[i] + W[i];
        uint32_t S0 = big_sigma_0(temp_h[0]);
        uint32_t maj = majority(temp_h[0], temp_h[1], temp_h[2]);
        uint32_t temp2 = S0 + maj;

        temp_h[7] = temp_h[6];
        temp_h[6] = temp_h[5];
        temp_h[5] = temp_h[4];
        temp_h[4] = temp_h[3] + temp1;
        temp_h[3] = temp_h[2];
        temp_h[2] = temp_h[1];
        temp_h[1] = temp_h[0];
        temp_h[0] = temp1 + temp2;
    }

    for (int i = 0; i < 8; i++) {
        H_state[i] += temp_h[i];
    }
}

// Main SHA256 hashing function
inline std::string sha256(const std::string& input_string) {
    uint32_t current_H[8];
    for(int i=0; i<8; ++i) {
        current_H[i] = INITIAL_H_VALUES[i];
    }

    std::vector<uint8_t> message_bytes;
    message_bytes.reserve(input_string.length() + 73); // Original + 0x80 + padding + length_bytes

    for (char ch : input_string) {
        message_bytes.push_back(static_cast<uint8_t>(ch));
    }

    uint64_t original_length_bits = (uint64_t)input_string.length() * 8;

    message_bytes.push_back(0x80); // Append bit '1' (and 7 zero bits)

    // Append 0 <= k < 512 bits '0', such that the resulting message length in bits
    // is congruent to 448 (mod 512)
    while (message_bytes.size() % 64 != 56) {
        message_bytes.push_back(0x00);
    }

    // Append original length in bits as a 64-bit big-endian integer
    for (int i = 7; i >= 0; --i) {
        message_bytes.push_back(static_cast<uint8_t>((original_length_bits >> (i * 8)) & 0xFF));
    }
    uint8_t block[64];
    for (size_t i = 0; i < message_bytes.size() / 64; ++i) {
        std::memcpy(block, &message_bytes[i * 64], 64);
        compress_block_sha256(current_H, block);
    }
    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    for (size_t i = 0; i < 8; ++i) {
        ss << std::setw(8) << current_H[i];
    }
    return ss.str();
}
