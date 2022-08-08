cmake_minimum_required(VERSION 3.18.1)

project("study")

include_directories(include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
message(-------------)
message(${CMAKE_SOURCE_DIR})
aux_source_directory(. SOURCE)
#add_library(
#        native-lib
#        SHARED
#        native-lib.cpp )
add_library(
        native-lib
        SHARED
        ${SOURCE}
)
find_library(
        log-lib
        log)
target_link_libraries(
        native-lib
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        ${log-lib}
        OpenSLES
        android)


add_library(
        study
        SHARED
        ${SOURCE})


find_library(
        log-lib
        log)


target_link_libraries(
        study
        avcodec
        avdevice
        avfilter
        avformat
        avutil
        swresample
        swscale
        OpenSLES
        android
        ${log-lib})