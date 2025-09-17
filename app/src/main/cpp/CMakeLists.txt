# CMakeLists.txt for Shaderlay native library

cmake_minimum_required(VERSION 3.22.1)

project("shaderlaynative")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add compile flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -fno-exceptions -fno-rtti")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -g")

# Find packages
find_library(log-lib log)
find_library(android-lib android)
find_library(gles2-lib GLESv2)
find_library(egl-lib EGL)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/glslang
    ${CMAKE_CURRENT_SOURCE_DIR}/spirv-cross
)

# Source files
set(NATIVE_SOURCES
    shader_compiler.cpp
    spirv_handler.cpp
    slang_parser.cpp
    jni_interface.cpp
)

# Create the native library
add_library(shaderlaynative SHARED ${NATIVE_SOURCES})

# Link libraries
target_link_libraries(shaderlaynative
    ${log-lib}
    ${android-lib}
    ${gles2-lib}
    ${egl-lib}
)

# Compiler-specific options
if(CMAKE_COMPILER_IS_GNUCXX OR CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    target_compile_options(shaderlaynative PRIVATE
        -Wall
        -Wextra
        -Wpedantic
        -Wno-unused-parameter
    )
endif()

# Add preprocessor definitions
target_compile_definitions(shaderlaynative PRIVATE
    ANDROID
    GL_GLEXT_PROTOTYPES
    EGL_EGLEXT_PROTOTYPES
)