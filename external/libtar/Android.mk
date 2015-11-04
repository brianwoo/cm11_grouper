LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libtar
LOCAL_MODULE_TAGS = optional
LOCAL_SRC_FILES := lib/append.c \
			lib/block.c \
			lib/decode.c \
			lib/encode.c \
			lib/extract.c \
			lib/handle.c \
			listhash/libtar_hash.c \
			listhash/libtar_list.c \
			lib/output.c \
			lib/util.c \
			lib/wrapper.c \
			compat/strlcpy.c \
			compat/basename.c \
			compat/dirname.c \
			compat/strmode.c


LOCAL_C_INCLUDES := $(LOCAL_PATH)/lib $(LOCAL_PATH)/compat $(LOCAL_PATH)/listhash
LOCAL_CFLAGS += -DHAVE_SELINUX

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := minitar
LOCAL_MODULE_TAGS = optional
LOCAL_SRC_FILES := libtar/libtar.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/lib $(LOCAL_PATH)/compat $(LOCAL_PATH)/listhash $(LOCAL_PATH)/../zlib
LOCAL_STATIC_LIBRARIES := libc libtar libz libselinux
LOCAL_MODULE_PATH := $(TARGET_RECOVERY_ROOT_OUT)/sbin

LOCAL_FORCE_STATIC_EXECUTABLE := true

include $(BUILD_EXECUTABLE)
