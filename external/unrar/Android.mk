LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := src/rar.cpp src/strlist.cpp src/strfn.cpp src/pathfn.cpp src/savepos.cpp \
                   src/smallfn.cpp src/global.cpp src/file.cpp src/filefn.cpp src/filcreat.cpp \
                   src/archive.cpp src/arcread.cpp src/system.cpp src/isnt.cpp src/crypt.cpp \
                   src/crc.cpp src/rawread.cpp src/encname.cpp src/resource.cpp src/match.cpp \
                   src/timefn.cpp src/rdwrfn.cpp src/consio.cpp src/options.cpp src/ulinks.cpp \
                   src/errhnd.cpp src/rarvm.cpp src/secpassword.cpp src/rijndael.cpp \
                   src/getbits.cpp src/sha1.cpp src/extinfo.cpp src/volume.cpp src/recvol.cpp \
                   src/rs.cpp src/list.cpp src/find.cpp src/unpack.cpp src/cmddata.cpp \
                   src/filestr.cpp src/scantree.cpp src/extract.cpp

LOCAL_C_INCLUDES := src

LOCAL_CFLAGS := -O2 -Wall -DUNRAR -D__BIONIC__ \
                -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -DNOVOLUME -DRAR_NOCRYPT

LOCAL_ARM_MODE := arm

LOCAL_MODULE := unrar
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

include $(BUILD_EXECUTABLE)

