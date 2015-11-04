LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := kiss_fft.c

LOCAL_CFLAGS := -O3 -fvisibility=hidden

ifeq ($(ARCH_ARM_HAVE_NEON),true)
LOCAL_CFLAGS += -D__ARM_HAVE_NEON
LOCAL_SRC_FILES += kiss_fft_bfly2_neon.S
LOCAL_SRC_FILES += kiss_fft_bfly4_neon.S
endif

LOCAL_MODULE := libkissfft

include $(BUILD_STATIC_LIBRARY)
