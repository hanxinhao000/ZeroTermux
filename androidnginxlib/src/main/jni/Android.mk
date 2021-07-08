LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := nginx_static
LOCAL_SRC_FILES := nginx/libnginx_static.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := crypto
LOCAL_SRC_FILES := openssl/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := ssl
LOCAL_SRC_FILES := openssl/libssl.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)

##LOCAL_PATH := $(call my-dir)
LOCAL_MODULE    := nginx
LOCAL_CFLAGS  += -Wno-sign-compare -DOPENSSL_NO_ENGINE

LOCAL_SRC_FILES := \
	Java_org_nginx_Nginx.cpp \


NGINX_ROOT:=/Users/lixingkun/red5/crtmpserver/Cross-Compile-Nginx-with-RTMP-Module-for-Android/nginx-1.12.0/
LOCAL_C_INCLUDES:= \
    $(LOCAL_PATH) \
	$(NGINX_ROOT)//src/core \
	$(NGINX_ROOT)//src/event \
	$(NGINX_ROOT)//src/event/modules \
	$(NGINX_ROOT)//src/os/unix \
	$(NGINX_ROOT)//objs \
	$(NGINX_ROOT)//src/http \
	$(NGINX_ROOT)//src/http/modules \
	$(NGINX_ROOT)//src/mail  \
	 /usr/local/ssl/android-21/include/

LOCAL_LDLIBS    := -llog -lz
LOCAL_STATIC_LIBRARIES  :=  nginx_static ssl crypto
include $(BUILD_SHARED_LIBRARY)
