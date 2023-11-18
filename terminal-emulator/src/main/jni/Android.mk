LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE:= libtermux
LOCAL_SRC_FILES:= termux.c
LOCAL_LDLIBS :=-llog
include $(BUILD_SHARED_LIBRARY)
