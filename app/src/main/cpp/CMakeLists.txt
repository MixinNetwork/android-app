cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_STANDARD 14)

set(curve25519_DIR ${CMAKE_SOURCE_DIR}/curve25519)

file(GLOB curve25519_ED25519_SOURCES
        ${curve25519_DIR}/ed25519/*.c
        ${curve25519_DIR}/ed25519/additions/*.c
        ${curve25519_DIR}/ed25519/additions/generalized/*.c
        ${curve25519_DIR}/ed25519/nacl_sha512/*.c)

add_library(curve25519
        SHARED
        ${curve25519_DIR}/curve25519-jni.c
        ${curve25519_DIR}/curve25519-donna.c
        ${curve25519_ED25519_SOURCES}
        ${curve25519_DIR}/ed25519/tests/internal_fast_tests.c)

set_target_properties(curve25519 PROPERTIES
        C_STANDARD 99
        C_STANDARD_REQUIRED ON)

target_compile_options(curve25519 PRIVATE
        -Wno-pointer-sign)

target_include_directories(curve25519 PRIVATE
        ${curve25519_DIR}
        ${curve25519_DIR}/ed25519
        ${curve25519_DIR}/ed25519/additions
        ${curve25519_DIR}/ed25519/additions/generalized
        ${curve25519_DIR}/ed25519/nacl_includes
        ${curve25519_DIR}/ed25519/nacl_sha512
        ${curve25519_DIR}/ed25519/tests)

target_link_options(curve25519 PRIVATE
        -Wl,-z,max-page-size=16384
        -Wl,-z,common-page-size=16384)

set(distribution_OPUS_DIR ${CMAKE_SOURCE_DIR}/opus)

add_library(lib_opus STATIC IMPORTED)
set_target_properties(lib_opus PROPERTIES IMPORTED_LOCATION
        ${distribution_OPUS_DIR}/lib/${ANDROID_ABI}/libopus.a)

add_library(lib_ogg STATIC IMPORTED)
set_target_properties(lib_ogg PROPERTIES IMPORTED_LOCATION
        ${distribution_OPUS_DIR}/lib/${ANDROID_ABI}/libogg.a)

add_library(lib_opusenc STATIC IMPORTED)
set_target_properties(lib_opusenc PROPERTIES IMPORTED_LOCATION
        ${distribution_OPUS_DIR}/lib/${ANDROID_ABI}/libopusenc.a)

add_library(mixin SHARED
            audio.c)
target_link_options(mixin PRIVATE
        -Wl,-z,max-page-size=16384
        -Wl,-z,common-page-size=16384
)

target_include_directories(mixin PRIVATE
        ${distribution_OPUS_DIR}/include)

target_link_libraries(mixin
                      android
                      lib_opusenc
                      lib_opus
                      lib_ogg
                      log)


add_library(lib_lz4 STATIC IMPORTED)
set_target_properties(lib_lz4 PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/lz4/lib/${ANDROID_ABI}/liblz4.a)

add_library(lib_rlottie_internal STATIC IMPORTED)
set_target_properties(lib_rlottie_internal PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/rlottie/lib/${ANDROID_ABI}/librlottie_internal.a)

add_library(rlottie
        SHARED
        ${CMAKE_SOURCE_DIR}/lottie.cpp
        )
target_link_options(rlottie PRIVATE
        -Wl,-z,max-page-size=16384
        -Wl,-z,common-page-size=16384
)

target_include_directories(rlottie PRIVATE
        ${CMAKE_SOURCE_DIR}/rlottie/inc
        ${CMAKE_SOURCE_DIR}/lz4/inc)

target_link_libraries(
        rlottie
        lib_lz4
        lib_rlottie_internal
        jnigraphics
        ${log-lib})
