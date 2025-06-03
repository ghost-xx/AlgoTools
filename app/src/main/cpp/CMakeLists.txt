cmake_minimum_required(VERSION 3.22.1)


project("algotools")


add_library(
        algotools
        SHARED
        native-lib.cpp
        md5.cpp
        sha1.cpp
        sha256.cpp
)


find_library(
        log-lib
        log
)
target_link_libraries(
        algotools
        ${log-lib}
)