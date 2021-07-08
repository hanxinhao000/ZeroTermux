
/*
 * Copyright (C) Igor Sysoev
 * Copyright (C) Nginx, Inc.
 */


#ifndef _NGINX_H_INCLUDED_
#define _NGINX_H_INCLUDED_


#define nginx_version      1005010
#define NGINX_VERSION      "1.5.10"
#define NGINX_VER          "nginx/" NGINX_VERSION

#define NGINX_VAR          "NGINX"
#define NGX_OLDPID_EXT     ".oldbin"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef DDEBUG
#ifdef ANDROID
#define jni_ngx_log_print __android_log_print
#else
#define jni_ngx_log_print printf
#endif
#endif

int jni_main(int argc, char *const *argv);

int jni_stop();

#ifdef __cplusplus
}
#endif

#endif /* _NGINX_H_INCLUDED_ */
