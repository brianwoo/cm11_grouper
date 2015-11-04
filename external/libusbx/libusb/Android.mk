LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
 core.c \
 descriptor.c \
 io.c \
 sync.c \
 os/linux_usbfs.c \
 os/threads_posix.c \
 os/poll_posix.c \
 hotplug.c

LOCAL_C_INCLUDES += \
 external/libusbx/ \
 external/libusbx/libusb/ \
 external/libusbx/libusb/os

LOCAL_CFLAGS := -D_SHARED_LIBRARY_
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= libusbx
#include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
 core.c \
 descriptor.c \
 io.c \
 sync.c \
 hotplug.c

LOCAL_C_INCLUDES += \
 external/libusbx/ \
 external/libusbx/libusb/ \
 external/libusbx/libusb/os

ifeq ($(HOST_OS),linux)
  LOCAL_SRC_FILES += os/linux_usbfs.c os/threads_posix.c os/poll_posix.c
endif

ifeq ($(HOST_OS),darwin)
  LOCAL_SRC_FILES += os/darwin_usb.c os/threads_posix.c os/poll_posix.c
endif

ifeq ($(HOST_OS),windows)
  LOCAL_SRC_FILES += os/windows_usb.c os/threads_windows.c os/poll_windows.c
endif


LOCAL_CFLAGS := -D_SHARED_LIBRARY_
LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE:= libusbx
include $(BUILD_HOST_STATIC_LIBRARY)
