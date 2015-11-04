LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

CSSTOH_SOURCE := $(LOCAL_PATH)/csstoh.c
POWERTOP_CSS_SOURCE := $(LOCAL_PATH)/powertop.css
GEN_CSSTOH := $(LOCAL_PATH)/csstoh
GEN_CSS_H := $(LOCAL_PATH)/css.h
$(GEN_CSS_H):
	$(CC) -o $(GEN_CSSTOH) $(CSSTOH_SOURCE)
	./$(GEN_CSSTOH) $(POWERTOP_CSS_SOURCE) $@

LOCAL_GENERATED_SOURCES += $(GEN_CSS_H)

LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libstlport

LOCAL_SHARED_LIBRARIES += libncurses
LOCAL_STATIC_LIBRARIES += libnl_2

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)

LOCAL_MODULE := powertop

#LOCAL_CFLAGS += -Wall -O2 -g -fno-omit-frame-pointer -fstack-protector -Wshadow -Wformat -D_FORTIFY_SOURCE=2
#LOCAL_CPPFLAGS += -Wall -O2 -g -fno-omit-frame-pointer
LOCAL_CPPFLAGS += \
		-DDISABLE_I18N \
		-DDISABLE_TRYCATCH \
		-DNCURSES_NOMACROS \
		-DDISABLE_WSTRING \
		-DDEFAULT_TERM=\"xterm\" \
		-DTERMINFO_PATH=\"/system/etc/terminfo\" \
		-DDEFINE_ETHTOOL_CMD \

LOCAL_C_INCLUDES += external/stlport/stlport/ \
					external/stlport/stlport/stl \
					external/stlport/stlport/using/h/ \
					bionic \
					external/libnl-headers/ \
					external/libncurses \
					external/libncurses/lib \
					external/libncurses/include \
					external/libncurses/include/ncurses

LOCAL_CFLAGS += -DCONFIG_LIBNL20
ifeq ($(TARGET_ARCH),x86)
#TARGET_ARCH == x86
LOCAL_SHARED_LIBRARIES += libpci
LOCAL_C_INCLUDES += external/pciutils/include
else 
#TARGET_ARCH != x86
LOCAL_CPPFLAGS += -DDISABLE_PCI
endif 

LOCAL_SRC_FILES += \
	parameters/parameters.cpp \
	parameters/persistent.cpp \
	parameters/learn.cpp \
	process/powerconsumer.cpp \
	process/work.cpp \
	process/process.cpp \
	process/timer.cpp \
	process/device.cpp \
	process/interrupt.cpp \
	process/do_process.cpp \
	cpu/intel_cpus.cpp \
	cpu/cpu.cpp \
	cpu/cpu_linux.cpp \
	cpu/cpudevice.cpp \
	cpu/cpu_core.cpp \
	cpu/cpu_package.cpp \
	cpu/abstract_cpu.cpp \
	measurement/measurement.cpp \
	measurement/acpi.cpp \
	measurement/extech.cpp \
	measurement/power_supply.cpp \
	display.cpp \
	html.cpp \
	main.cpp \
	tuning/tuning.cpp \
	tuning/usb.cpp \
	tuning/bluetooth.cpp \
	tuning/ethernet.cpp \
	tuning/runtime.cpp \
	tuning/iw.c \
	tuning/iw.h \
	tuning/tunable.cpp \
	tuning/sysfs.cpp \
	tuning/cpufreq.cpp \
	tuning/wifi.cpp \
	perf/perf_bundle.cpp \
	perf/perf.cpp \
	devices/thinkpad-fan.cpp \
	devices/alsa.cpp \
	devices/runtime_pm.cpp \
	devices/usb.cpp \
	devices/ahci.cpp \
	devices/rfkill.cpp \
	devices/thinkpad-light.cpp \
	devices/i915-gpu.cpp \
	devices/backlight.cpp \
	devices/network.cpp \
	devices/device.cpp \
	devlist.cpp \
	calibrate/calibrate.cpp \
	lib.cpp \

include $(BUILD_EXECUTABLE)

