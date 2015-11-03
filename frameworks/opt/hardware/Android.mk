# Copyright (C) 2013 The CyanogenMod Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifneq ($(BOARD_HARDWARE_CLASS),)
    $(foreach bcp,$(BOARD_HARDWARE_CLASS), \
        $(eval LOCAL_SRC_FILES += $(call all-java-files-under, ../../../$(bcp))))
endif

BASE_SRC_FILES += $(call all-java-files-under, src/)

unique_specific_classes := 
    $(foreach cf,$(LOCAL_SRC_FILES), \
        $(eval unique_specific_classes += $(notdir $(cf))))
  
default_classes :=
$(foreach cf,$(BASE_SRC_FILES), \
    $(if $(filter $(unique_specific_classes),$(notdir $(cf))),,\
        $(eval default_classes += $(cf))))

LOCAL_SRC_FILES += $(default_classes)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := org.cyanogenmod.hardware

include $(BUILD_JAVA_LIBRARY)

include $(call all-makefiles-under,$(LOCAL_PATH))

