#pragma once
#include <vector>
#include <sstream>
#include <iomanip>
#include <string>
#include <cstring>
#include <cstdint>

// SHA-512 constants K
const uint64_t K512[80] = {
    0x428a2f98d728ae22ULL, 0x7137449123ef65cdULL, 0xb5c0fbcfec4d3b2fULL, 0xe9b5dba58189dbbcULL,
    0x3956c25bf348b538ULL, 0x59f111f1b605d019ULL, 0x923f82a4af194f9bULL, 0xab1c5ed5da6d8118ULL,
    0xd807aa98a3030242ULL, 0x12835b0145706fbeULL, 0x243185be4ee4b28cULL, 0x550c7dc3d5ffb4e2ULL,
    0x72be5d74f27b896fULL, 0x80deb1fe3b1696b1ULL, 0x9bdc06a725c71235ULL, 0xc19bf174cf692694ULL,
    0xe49b69c19ef14ad2ULL, 0xefbe4786384f25e3ULL, 0x0fc19dc68b8cd5b5ULL, 0x240ca1cc77ac9c65ULL,
    0x2de92c6f592b0275ULL, 0x4a7484aa6ea6e483ULL, 0x5cb0a9dcbd41fbd4ULL, 0x76f988da831153b5ULL,
    0x983e5152ee66dfabULL, 0xa831c66d2db43210ULL, 0xb00327c898fb213fULL, 0xbf597fc7beef0ee4ULL,
    0xc6e00bf33da88fc2ULL, 0xd5a79147930aa725ULL, 0x06ca6351e003826fULL, 0x142929670a0e6e70ULL,
    0x27b70a8546d22ffcULL, 0x2e1b21385c26c926ULL, 0x4d2c6dfc5ac42aedULL, 0x53380d139d95b3dfULL,
    0x650a73548baf63deULL, 0x766a0abb3c77b2a8ULL, 0x81c2c92e47edaee6ULL, 0x92722c851482353bULL,
    0xa2bfe8a14cf10364ULL, 0xa81a664bbc423001ULL, 0xc24b8b70d0f89791ULL, 0xc76c51a30654be30ULL,
    0xd192e819d6ef5218ULL, 0xd69906245565a910ULL, 0xf40e35855771202aULL, 0x106aa07032bbd1b8ULL,
    0x19a4c116b8d2d0c8ULL, 0x1e376c085141ab53ULL, 0x2748774cdf8eeb99ULL, 0x34b0bcb5e19b48a8ULL,
    0x391c0cb3c5c95a63ULL, 0x4ed8aa4ae3418acbULL, 0x5b9cca4f7763e373ULL, 0x682e6ff3d6b2b8a3ULL,
    0x748f82ee5defb2fcULL, 0x78a5636f43172f60ULL, 0x84c87814a1f0ab72ULL, 0x8cc702081a6439ecULL,
    0x90befffa23631e28ULL, 0xa4506cebde82bde9ULL, 0xbef9a3f7b2c67915ULL, 0xc67178f2e372532bULL,
    0xca273eceea26619cULL, 0xd186b8c721c0c207ULL, 0xeada7dd6cde0eb1eULL, 0xf57d4f7fee6ed178ULL,
    0x06f067aa72176fbaULL, 0x0a637dc5a2c898a6ULL, 0x113f9804bef90daeULL, 0x1b710b35131c471bULL,
    0x28db77f523047d84ULL, 0x32caab7b40c72493ULL, 0x3c9ebe0a15c9bebcULL, 0x431d67c49c100d4cULL,
    0x4cc5d4becb3e42b6ULL, 0x597f299cfc657e2aULL, 0x5fcb6fab3ad6faecULL, 0x6c44198c4a475817ULL
};

// Initial hash values H0-H7 for SHA-512
const uint64_t INITIAL_H512_VALUES[8] = {
    0x6a09e667f3bcc908ULL, 0xbb67ae8584caa73bULL,
    0x3c6ef372fe94f82bULL, 0xa54ff53a5f1d36f1ULL,
    0x510e527fade682d1ULL, 0x9b05688c2b3e6c1fULL,
    0x1f83d9abfb41bd6bULL, 0x5be0cd19137e2179ULL
};

// Helper functions for SHA-512
#define ROR64(x, n) (((x) >> (n)) | ((x) << (64 - (n))))
#define CH(x, y, z) (((x) & (y)) ^ (~(x) & (z)))
#define MAJ(x, y, z) (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
#define SIGMA0(x) (ROR64(x, 28) ^ ROR64(x, 34) ^ ROR64(x, 39))
#define SIGMA1(x) (ROR64(x, 14) ^ ROR64(x, 18) ^ ROR64(x, 41))
#define sigma0(x) (ROR64(x, 1) ^ ROR64(x, 8) ^ ((x) >> 7))
#define sigma1(x) (ROR64(x, 19) ^ ROR64(x, 61) ^ ((x) >> 6))

// Fast byte-swap for big-endian conversion
inline uint64_t bswap64(uint64_t x) {
    x = ((x << 8) & 0xFF00FF00FF00FF00ULL) | ((x >> 8) & 0x00FF00FF00FF00FFULL);
    x = ((x << 16) & 0xFFFF0000FFFF0000ULL) | ((x >> 16) & 0x0000FFFF0000FFFFULL);
    return (x << 32) | (x >> 32);
}

inline void compress_block_sha512(uint64_t H_state[8], const uint8_t* block) {
    alignas(16) uint64_t W[80];
    uint64_t a = H_state[0];
    uint64_t b = H_state[1];
    uint64_t c = H_state[2];
    uint64_t d = H_state[3];
    uint64_t e = H_state[4];
    uint64_t f = H_state[5];
    uint64_t g = H_state[6];
    uint64_t h = H_state[7];
    
    // Message schedule with unrolled endian conversion
    const uint64_t* block64 = reinterpret_cast<const uint64_t*>(block);
    #pragma unroll
    for (int i = 0; i < 16; i++) {
        W[i] = bswap64(block64[i]);
    }
    
    #pragma unroll
    for (int i = 16; i < 80; i++) {
        W[i] = sigma1(W[i-2]) + W[i-7] + sigma0(W[i-15]) + W[i-16];
    }
    
    // Main loop with manual unrolling for better instruction pipelining
    #pragma unroll
    for (int i = 0; i < 80; i += 8) {
        for (int j = 0; j < 8; j++) {
            uint64_t T1 = h + SIGMA1(e) + CH(e, f, g) + K512[i+j] + W[i+j];
            uint64_t T2 = SIGMA0(a) + MAJ(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }
    }
    
    H_state[0] += a;
    H_state[1] += b;
    H_state[2] += c;
    H_state[3] += d;
    H_state[4] += e;
    H_state[5] += f;
    H_state[6] += g;
    H_state[7] += h;
}

// Optimized SHA512 hashing function
inline std::string sha512(const std::string& input_string) {
    alignas(16) uint64_t current_H[8];
    std::memcpy(current_H, INITIAL_H512_VALUES, sizeof(current_H));
    
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
    
    // Format output
    char hex[129];
    char* ptr = hex;
    for (int i = 0; i < 8; ++i) {
        uint64_t h = current_H[i];
        for (int j = 60; j >= 0; j -= 4) {
            *ptr++ = "0123456789abcdef"[(h >> j) & 0xF];
        }
    }
    *ptr = '\0';
    
    return std::string(hex);
} 