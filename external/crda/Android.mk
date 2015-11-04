LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	reglib.c \
	crda.c \
	keys-ssl.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH) \
	external/libnl-headers \
	external/openssl/include

LOCAL_CFLAGS := -DUSE_OPENSSL -DPUBKEY_DIR=\"/system/lib/crda\" -DCONFIG_LIBNL20

LOCAL_MODULE_TAGS := eng optional
LOCAL_SHARED_LIBRARIES := libcrypto libnl_2
LOCAL_MODULE := crda

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
	reglib.c \
	keys-ssl.c \
	regdbdump.c \
	print-regdom.c

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH) \
	external/libnl-headers \
	external/openssl/include

LOCAL_CFLAGS := -DUSE_OPENSSL -DPUBKEY_DIR=\"/system/lib/crda\" -DCONFIG_LIBNL20

LOCAL_MODULE_TAGS := eng optional
LOCAL_SHARED_LIBRARIES := libcrypto libnl_2
LOCAL_MODULE := regdbdump
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE       := regulatory.bin
LOCAL_MODULE_TAGS  := eng optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH  := $(TARGET_OUT)/lib/crda
LOCAL_SRC_FILES    := ./$(LOCAL_MODULE)
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE       := linville.key.pub.pem
LOCAL_MODULE_TAGS  := eng optional
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_PATH  := $(TARGET_OUT)/lib/crda
LOCAL_SRC_FILES    := ./pubkeys/$(LOCAL_MODULE)
include $(BUILD_PREBUILT)
