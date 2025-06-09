cmake_minimum_required(VERSION 3.22.1)


project("algotools")

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)


add_library(
        algotools
        SHARED
        native-lib.cpp
        hash_calculator.cpp
        hash_calculator.h
        string_search.cpp
        string_search.h
        memory_utils.cpp
        memory_utils.h
        md5.cpp
        md5.h
        sha1.cpp
        sha1.h
        sha256.h
        sha384.h
        sha512.h
)


find_library(
        log-lib
        log
)
target_link_libraries(
        algotools
        ${log-lib}
        android
)