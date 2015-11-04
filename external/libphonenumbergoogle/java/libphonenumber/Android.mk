LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libphonenumbergoogle
LOCAL_SDK_VERSION := 8
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_JAVA_RESOURCE_DIRS := src
  
include $(BUILD_STATIC_JAVA_LIBRARY)
