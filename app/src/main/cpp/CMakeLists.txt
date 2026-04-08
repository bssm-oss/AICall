cmake_minimum_required(VERSION 3.22.1)

project(aicall_local_engine)

add_library(aicall_local_engine SHARED local_engine_bridge.cpp)

find_library(log-lib log)

target_link_libraries(aicall_local_engine ${log-lib})
