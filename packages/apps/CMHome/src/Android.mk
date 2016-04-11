#
# Copyright (C) 2014 The CyanogenMod Project
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
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_STATIC_JAVA_LIBRARIES := org.cyanogenmod.launcher.home \
			dashclockapiv2 \
            android-support-v13

library_src_files += ../../../../external/cardslib/library/src/main/java
LOCAL_SRC_FILES += $(call all-java-files-under, $(library_src_files))
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res \
	$(LOCAL_PATH)/../../../../external/cardslib/library/src/main/res

LOCAL_PACKAGE_NAME := CMHome
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_AAPT_FLAGS := \
	--auto-add-overlay \
	--extra-packages it.gmariotti.cardslib.library \

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	dashclockapiv2:libs/dashclock-api-r2.0.jar

include $(BUILD_MULTI_PREBUILT)
