cmake_minimum_required(VERSION 3.22.1)


project("algotools")


add_library(
        algotools
        SHARED
        native-lib.cpp
        md5.cpp
        md5.h
        sha1.cpp
        sha1.h
        sha256.h
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