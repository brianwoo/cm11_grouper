
LOCAL_PATH := $(call my-dir)

C_SRC_FILES := \
    src/google/protobuf-c/protobuf-c-rpc.c              \
    src/google/protobuf-c/protobuf-c-dispatch.c         \
    src/google/protobuf-c/protobuf-c-data-buffer.c      \
    src/google/protobuf-c/protobuf-c.c

# C library
# =======================================================
include $(CLEAR_VARS)

LOCAL_MODULE := libprotobuf-c
LOCAL_MODULE_TAGS := debug

LOCAL_SRC_FILES := $(C_SRC_FILES)

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/src

LOCAL_COPY_HEADERS_TO := protobuf-c/include/google/protobuf-c/
LOCAL_COPY_HEADERS := ./src/google/protobuf-c/protobuf-c.h
LOCAL_COPY_HEADERS += ./src/google/protobuf-c/protobuf-c-private.h

LOCAL_SHARED_LIBRARIES := \
    libcutils libutils

LOCAL_CFLAGS := -DGOOGLE_PROTOBUF_NO_RTTI -DHAVE_SYS_POLL_H -DHAVE_ALLOCA_H -DHAVE_UNISTD_H

include $(BUILD_STATIC_LIBRARY)

