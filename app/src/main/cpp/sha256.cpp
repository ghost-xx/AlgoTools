// //////////////////////////////////////////////////////////
// sha256.cpp
// Copyright (c) 2014 Stephan Brumme. All rights reserved.
// see http://create.stephan-brumme.com/disclaimer.html
//

#include "sha256.h"

// define fixed size integer types
#ifdef _MSC_VER
// Windows
#include <Windows.h>
#else
// GCC
#include <endian.h>
#endif

#include <iostream> // for std::cout, std::cerr, std::hex, std::endl
#include <iomanip>  // for std::setw, std::setfill
#include <sstream>  // for std::ostringstream

/// same as reset()
SHA256::SHA256()
{
  reset();
}


/// restart
void SHA256::reset()
{
  m_numBytes   = 0;
  m_bufferSize = 0;

  // according to RFC 1321
  m_hash[0] = 0x6a09e667;
  m_hash[1] = 0xbb67ae85;
  m_hash[2] = 0x3c6ef372;
  m_hash[3] = 0xa54ff53a;
  m_hash[4] = 0x510e527f;
  m_hash[5] = 0x9b05688c;
  m_hash[6] = 0x1f83d9ab;
  m_hash[7] = 0x5be0cd19;
}


namespace
{
  // mix functions for processBlock()
  inline uint32_t rotateRight(uint32_t a, uint32_t c)
  {
    return (a >> c) | (a << (32 - c));
  }

  inline uint32_t choice(uint32_t a, uint32_t b, uint32_t c)
  {
    return (a & b) ^ (~a & c);
  }

  inline uint32_t majority(uint32_t a, uint32_t b, uint32_t c)
  {
    return (a & b) ^ (a & c) ^ (b & c);
  }

  inline uint32_t sig0(uint32_t a)
  {
    return rotateRight(a,  2) ^ rotateRight(a, 13) ^ rotateRight(a, 22);
  }

  inline uint32_t sig1(uint32_t a)
  {
    return rotateRight(a,  6) ^ rotateRight(a, 11) ^ rotateRight(a, 25);
  }

  // convert big endian to little endian and vice versa
  inline uint32_t swap(uint32_t x)
  {
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_bswap32(x);
#endif
#ifdef _MSC_VER
    return _byteswap_ulong(x);
#endif

    return (x >> 24) |
          ((x >>  8) & 0x0000FF00) |
          ((x <<  8) & 0x00FF0000) |
           (x << 24);
  }
}


/// process 64 bytes
void SHA256::processBlock(const void* data)
{
  // get last hash
  uint32_t a = m_hash[0];
  uint32_t b = m_hash[1];
  uint32_t c = m_hash[2];
  uint32_t d = m_hash[3];
  uint32_t e = m_hash[4];
  uint32_t f = m_hash[5];
  uint32_t g = m_hash[6];
  uint32_t h = m_hash[7];

  // data represented as 16x 32-bit words
  const uint32_t* input = (const uint32_t*) data;
  // convert to big endian
  uint32_t words[64];
  int i;
  for (i = 0; i < 16; i++)
#if defined(__BYTE_ORDER) && (__BYTE_ORDER != 0) && (__BYTE_ORDER == __BIG_ENDIAN)
    words[i] =      input[i];
#else
    words[i] = swap(input[i]);
#endif

  // extend to 64 words
  for (i = 16; i < 64; i++)
  {
    uint32_t s0 = rotateRight(words[i-15],  7) ^ rotateRight(words[i-15], 18) ^ (words[i-15] >>  3);
    uint32_t s1 = rotateRight(words[i- 2], 17) ^ rotateRight(words[i- 2], 19) ^ (words[i- 2] >> 10);
    words[i] = words[i-16] + s0 + words[i-7] + s1;
  }

  // Mush s0-s1 etc together
  static const uint32_t k[] =
  {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
  };

  // applies the SHA-256 algorithm for (const uint32_t k[64])
  for (i = 0; i < 64; i++)
  {
    uint32_t s1 = sig1(e);
    uint32_t ch = choice(e, f, g);
    uint32_t temp1 = h + s1 + ch + k[i] + words[i];
    uint32_t s0 = sig0(a);
    uint32_t maj = majority(a, b, c);
    uint32_t temp2 = s0 + maj;

    h = g;
    g = f;
    f = e;
    e = d + temp1;
    d = c;
    c = b;
    b = a;
    a = temp1 + temp2;
  }

  // add to hash
  m_hash[0] += a;
  m_hash[1] += b;
  m_hash[2] += c;
  m_hash[3] += d;
  m_hash[4] += e;
  m_hash[5] += f;
  m_hash[6] += g;
  m_hash[7] += h;
}


/// add arbitrary number of bytes
void SHA256::add(const void* data, size_t numBytes)
{
  const uint8_t* current = (const uint8_t*) data;

  // copy data to buffer
  if (m_bufferSize > 0)
  {
    while (numBytes > 0 && m_bufferSize < BlockSize)
    {
      m_buffer[m_bufferSize++] = *current++;
      numBytes--;
    }
  }

  // process new blocks
  while (numBytes >= BlockSize)
  {
    processBlock(current);
    current    += BlockSize;
    numBytes   -= BlockSize;
    m_numBytes += BlockSize;
  }

  // keep remaining bytes in buffer
  if (numBytes > 0)
  {
    while (numBytes > 0)
    {
      m_buffer[m_bufferSize++] = *current++;
      numBytes--;
    }
  }
}


/// process final block, less than 64 bytes
void SHA256::processBuffer()
{
  // the final block is processed differently compared to full blocks
  // be careful with copying data from m_buffer to block - m_buffer contains the padding bits too!
  size_t paddedLength = m_bufferSize * 8;

  // append padding bits
  m_buffer[m_bufferSize++] = 0x80;
  while (m_bufferSize < BlockSize)
    m_buffer[m_bufferSize++] = 0x00;

  // if anoter block is needed for length (big endian)
  if (m_bufferSize > BlockSize - 8) // 56 B
  {
    processBlock(m_buffer);
    // reset buffer
    for (int j = 0; j < (int)BlockSize; j++)
      m_buffer[j] = 0;
  }

  // append total length
  m_numBytes += paddedLength / 8; // total number of message bits
  // add stored numBytes
  uint64_t msgBits = (m_numBytes * 8) + (paddedLength % 8); // Total bits in message

  // append length
  // split msgBits into two 32-bit words and add them to the end of the buffer (big endian)
  // store total length in big endian format
  m_buffer[BlockSize - 8] = (uint8_t)(msgBits >> 56);
  m_buffer[BlockSize - 7] = (uint8_t)(msgBits >> 48);
  m_buffer[BlockSize - 6] = (uint8_t)(msgBits >> 40);
  m_buffer[BlockSize - 5] = (uint8_t)(msgBits >> 32);
  m_buffer[BlockSize - 4] = (uint8_t)(msgBits >> 24);
  m_buffer[BlockSize - 3] = (uint8_t)(msgBits >> 16);
  m_buffer[BlockSize - 2] = (uint8_t)(msgBits >>  8);
  m_buffer[BlockSize - 1] = (uint8_t)(msgBits       );

  processBlock(m_buffer);
}


/// return latest hash as 64 hex characters
std::string SHA256::getHash()
{
  // process remaining bytes
  processBuffer();

  // convert hash to string
  std::ostringstream result;
  for (int i = 0; i < 8; i++)
    result << std::hex << std::setw(8) << std::setfill('0') << m_hash[i];

  // restart for next use
  reset();

  return result.str();
}

/// return latest hash as bytes
__attribute__((unused)) void SHA256::getHash(unsigned char buffer[32])
{
    // process remaining bytes
    processBuffer();

    // copy hash to buffer (big-endian)
    for (int i = 0; i < 8; i++)
    {
        buffer[i*4 + 0] = (m_hash[i] >> 24) & 0xFF;
        buffer[i*4 + 1] = (m_hash[i] >> 16) & 0xFF;
        buffer[i*4 + 2] = (m_hash[i] >>  8) & 0xFF;
        buffer[i*4 + 3] =  m_hash[i]        & 0xFF;
    }

    // restart for next use
    reset();
}


/// compute SHA256 of a memory block
std::string SHA256::operator()(const void* data, size_t numBytes)
{
  reset();
  add(data, numBytes);
  return getHash();
}


/// compute SHA256 of a string, excluding final zero
std::string SHA256::operator()(const std::string& text)
{
  reset();
  add(text.c_str(), text.length());
  return getHash();
}

// std::string sha256(const std::string& text) {
//     SHA256 sha;
//     return sha(text);
// }

// This part will be in native-lib.cpp or a similar JNI wrapper
/*
std::string sha256_hex(const std::string& input) {
    SHA256 sha256;
    return sha256(input);
}
*/ 