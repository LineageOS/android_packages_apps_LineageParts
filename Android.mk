LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := LineageParts
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_ANDROID_LIBRARIES := \
    androidx.appcompat_appcompat \
    androidx.dynamicanimation_dynamicanimation \
    androidx.palette_palette \
    androidx.preference_preference \
    androidx.recyclerview_recyclerview \
    androidx.legacy_legacy-preference-v14 \
    androidx.legacy_legacy-support-v4 \
    androidx.legacy_legacy-support-v13

LOCAL_STATIC_JAVA_LIBRARIES := \
    jsr305 \
    org.lineageos.platform.internal

LOCAL_RESOURCE_DIR := \
    $(LOCAL_PATH)/res

LOCAL_USE_AAPT2 := true

LOCAL_PRIVATE_PLATFORM_APIS := true

LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

ifneq ($(INCREMENTAL_BUILDS),)
    LOCAL_PROGUARD_ENABLED := disabled
    LOCAL_JACK_ENABLED := incremental
    LOCAL_DX_FLAGS := --multi-dex
    LOCAL_JACK_FLAGS := --multi-dex native
endif

include frameworks/base/packages/SettingsLib/common.mk

include $(BUILD_PACKAGE)
