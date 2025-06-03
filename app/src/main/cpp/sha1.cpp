// From https://thealgorithms.github.io/C-Plus-Plus/d8/d7a/sha1_8cpp.html
// Simplified and extracted for use
#include <string>
#include <vector>
#include <array>
#include <cstdint>
#include <algorithm> // For std::copy
#include <iomanip>   // For std::set, std::set fill, std::hex
#include <sstream>   // For std::upstreaming

namespace hashing {
namespace sha1 {

// Rotates the bits of a 32-bit unsigned integer.
static uint32_t leftRotate32bits(uint32_t n, std::size_t rotate) {
    return (n << rotate) | (n >> (32 - rotate));
}

// Transforms the 160-bit SHA-1 signature into a 40 char hex string.
static std::string sig2hex(const uint8_t *sig) {
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (int i = 0; i < 20; ++i) {
        oss << std::setw(2) << static_cast<unsigned int>(sig[i]);
    }
    return oss.str();
}

// The SHA-1 algorithm itself, taking in a byte string.
// Returns a pointer to a 20-byte array (raw hash). Caller must delete.
static uint8_t* hash_bs(const void *input_bs, uint64_t input_size) {
    auto* input = static_cast<const uint8_t*>(input_bs);

    // Step 0: The initial 160-bit state
    uint32_t h0 = 0x67452301;
    uint32_t h1 = 0xEFCDAB89;
    uint32_t h2 = 0x98BADCFE;
    uint32_t h3 = 0x10325476;
    uint32_t h4 = 0xC3D2E1F0;

    // Step 1: Processing the byte string
    uint64_t padded_message_size = 0;
    if (input_size % 64 < 56) {
        padded_message_size = input_size + 64 - (input_size % 64);
    } else {
        padded_message_size = input_size + 128 - (input_size % 64);
    }

    std::vector<uint8_t> padded_message(padded_message_size);
    if (input_size > 0) { // Add this check
        std::copy(input, input + input_size, padded_message.begin());
    }
    padded_message[input_size] = 1 << 7; // 10000000
    // Zero padding (already handled by vector initialization for parts > input_size, except for the 1 bit)
    for (uint64_t i = input_size + 1; i < padded_message_size - 8; ++i) {
         padded_message[i] = 0;
    }


    uint64_t input_bitsize = input_size * 8;
    for (uint8_t i = 0; i < 8; i++) {
        padded_message[padded_message_size - 8 + i] = (input_bitsize >> (56 - 8 * i)) & 0xFF;
    }

    std::array<uint32_t, 80> W{}; // Word sequence

    // Rounds
    for (uint64_t chunk_idx = 0; chunk_idx * 64 < padded_message_size; chunk_idx++) {
        const uint8_t* chunk = padded_message.data() + (chunk_idx * 64);

        for (uint8_t i = 0; i < 16; i++) {
            W[i] = (static_cast<uint32_t>(chunk[i*4 + 0]) << 24) |
                   (static_cast<uint32_t>(chunk[i*4 + 1]) << 16) |
                   (static_cast<uint32_t>(chunk[i*4 + 2]) << 8)  |
                   (static_cast<uint32_t>(chunk[i*4 + 3]) << 0);
        }

        for (uint8_t i = 16; i < 80; i++) {
            W[i] = leftRotate32bits((W[i-3] ^ W[i-8] ^ W[i-14] ^ W[i-16]), 1);
        }

        uint32_t a = h0;
        uint32_t b = h1;
        uint32_t c = h2;
        uint32_t d = h3;
        uint32_t e = h4;
        uint32_t F = 0, k = 0;

        for (uint8_t i = 0; i < 80; i++) {
            if (i < 20) {
                F = (b & c) | ((~b) & d);
                k = 0x5A827999;
            } else if (i < 40) {
                F = b ^ c ^ d;
                k = 0x6ED9EBA1;
            } else if (i < 60) {
                F = (b & c) | (b & d) | (c & d);
                k = 0x8F1BBCDC;
            } else {
                F = b ^ c ^ d;
                k = 0xCA62C1D6;
            }

            uint32_t temp = leftRotate32bits(a, 5) + F + e + k + W[i];
            e = d;
            d = c;
            c = leftRotate32bits(b, 30);
            b = a;
            a = temp;
        }

        h0 += a;
        h1 += b;
        h2 += c;
        h3 += d;
        h4 += e;
    }

    auto* sig = new uint8_t[20];
    for (uint8_t i = 0; i < 4; i++) {
        sig[i]    = (h0 >> (24 - 8 * i)) & 0xFF;
        sig[i+4]  = (h1 >> (24 - 8 * i)) & 0xFF;
        sig[i+8]  = (h2 >> (24 - 8 * i)) & 0xFF;
        sig[i+12] = (h3 >> (24 - 8 * i)) & 0xFF;
        sig[i+16] = (h4 >> (24 - 8 * i)) & 0xFF;
    }
    return sig;
}

// Converts the string to byte string and calls the main algorithm.
// Returns a 40-char hex string.
std::string hash(const std::string &message) {
    if (message.empty()) { // Handle empty string case explicitly for safety with .data()
        uint8_t* raw_hash = hash_bs(nullptr, 0);
        std::string hex_str = sig2hex(raw_hash);
        delete[] raw_hash;
        return hex_str;
    }
    uint8_t* raw_hash = hash_bs(message.data(), message.size());
    std::string hex_str = sig2hex(raw_hash);
    delete[] raw_hash;
    return hex_str;
}

} // namespace sha1
} // namespace hashing 