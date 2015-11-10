LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := libfancontrol

LOCAL_SRC_FILES := fancontrol.c

LOCAL_SHARED_LIBRARIES := \
    libandroid_runtime \
    libnativehelper \
    libcutils \
    libutils \
    liblog

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
