#ifndef SHA1_H
#define SHA1_H

#include <string>

namespace hashing {
namespace sha1 {

// Calculates the SHA-1 hash of a string and returns it as a 40-character hex string.
std::string hash(const std::string &message);

} // namespace sha1
} // namespace hashing

#endif // SHA1_H 