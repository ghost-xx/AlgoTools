// //////////////////////////////////////////////////////////
// sha256.h
// Copyright (c) 2014 Stephan Brumme. All rights reserved.
// see http://create.stephan-brumme.com/disclaimer.html
//
#pragma once

// define fixed size integer types
#ifdef _MSC_VER
// Windows
typedef unsigned __int8  uint8_t;
typedef unsigned __int32 uint32_t;
typedef unsigned __int64 uint64_t;
#else
// GCC
#include <cstdint>
#endif

#include <string>

// SHA256 class
class SHA256
{
public:
  // split into 64 byte blocks (=> 512 bits)
  static const unsigned int BlockSize = 512 / 8;

  // same as reset()
  SHA256();

  // compute hash of a memory block
  std::string operator()(const void* data, size_t numBytes);
  // compute hash of a string, excluding final zero
  std::string operator()(const std::string& text);

  // add arbitrary number of bytes
  void add(const void* data, size_t numBytes);

  // return latest hash as 64 hex characters
  std::string getHash();
  // return latest hash as bytes
  __attribute__((unused)) void        getHash(unsigned char buffer[32]);

  // restart
  void reset();

private:
  // process 64 bytes
  void processBlock(const void* data);
  // process everything left in the internal buffer
  void processBuffer();

  // size of processed data in bytes
  uint64_t m_numBytes;
  // data not processed yet
  unsigned char m_buffer[BlockSize];
  // valid bytes in m_buffer
  size_t m_bufferSize;

  uint32_t m_hash[8];
}; 