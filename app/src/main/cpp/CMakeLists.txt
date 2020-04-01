cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_STANDARD 14)

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
            audio.c
        )

target_include_directories(mixin PRIVATE
                           ${distribution_OPUS_DIR}/include)

target_link_libraries(mixin
                      android
                      lib_opusenc
                      lib_opus
                      lib_ogg
                      log)

#add_library(lz4 STATIC ${CMAKE_SOURCE_DIR}/lz4/lz4.c)

include_directories(
    ${CMAKE_SOURCE_DIR}/lz4/
    ${CMAKE_SOURCE_DIR}/rlottie/inc
    ${CMAKE_SOURCE_DIR}/rlottie/src/vector/
    ${CMAKE_SOURCE_DIR}/rlottie/src/vector/pixman
    ${CMAKE_SOURCE_DIR}/rlottie/src/vector/freetype
    ${CMAKE_SOURCE_DIR}/rlottie/src/vector/stb
)

add_library(rlottie
        SHARED
        ${CMAKE_SOURCE_DIR}/lottie.cpp
        ${CMAKE_SOURCE_DIR}/lz4/lz4.c
        ${CMAKE_SOURCE_DIR}/lz4/lz4frame.c
        ${CMAKE_SOURCE_DIR}/lz4/lz4hc.c
        ${CMAKE_SOURCE_DIR}/lz4/xxhash.c
        ${CMAKE_SOURCE_DIR}/rlottie/inc
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottieanimation.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottieitem.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottiekeypath.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottieloader.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottiemodel.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/lottie/lottieparser.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/freetype/v_ft_math.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/freetype/v_ft_raster.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/freetype/v_ft_stroker.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/pixman/vregion.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/stb/stb_image.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vbezier.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vbitmap.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vbrush.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vcompositionfunctions.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vdasher.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vdebug.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vdrawable.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vdrawhelper.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vdrawhelper_neon.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/velapsedtimer.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vimageloader.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vinterpolator.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vmatrix.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vpainter.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vpath.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vpathmesure.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vraster.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vrect.cpp
        ${CMAKE_SOURCE_DIR}/rlottie/src/vector/vrle.cpp
        )
target_link_libraries(
        rlottie
        jnigraphics
        ${log-lib})
