
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common framework
LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src

LOCAL_SRC_FILES += $(call all-java-files-under, src/)

LOCAL_MODULE_TAGS := optional debug
LOCAL_MODULE := telephony-msim

include $(BUILD_JAVA_LIBRARY)

# Include subdirectory makefiles
# ============================================================
include $(call all-makefiles-under,$(LOCAL_PATH))

