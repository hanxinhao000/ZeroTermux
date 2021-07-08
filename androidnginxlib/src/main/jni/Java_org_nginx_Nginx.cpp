#include <android/log.h>
#include <jni.h>
#include <ngx_config.h>
#include <nginx.h>
#include <stdlib.h>


#ifdef __cplusplus
extern "C" {
#endif

#ifndef NULL
#define NULL 0
#endif

static char *config = NULL;
static char *prefix = NULL;
static char *host = NULL;
static int port = 8080;

static char * jstrdup(JNIEnv* env, jstring str);

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_startNative(JNIEnv* env, jobject thiz)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_start");

	int result = 0;

	if (prefix && config) {

		char *ngx_cmd[5] = {"nginx", "-p", prefix, "-c", config};
		//char *ngx_cmd[5] = {"nginx","-p", "/sdcard/nginx/", "-c", "conf/nginx.conf"};
		result = jni_main(5, ngx_cmd);

	} else if (prefix) {
		char *ngx_cmd[3] = {"nginx", "-p", prefix};
		result = jni_main(3, ngx_cmd);
	} else if (config) {


		char *ngx_cmd[3] = {"nginx", "-c", config};
		//char *ngx_cmd[3] = {"nginx", "-c", "/sdcard/nginx/conf/nginx.conf"/*config*/};
		result = jni_main(3, ngx_cmd);

	} else {

		char *ngx_cmd[1] = {"nginx"};
		result = jni_main(1, ngx_cmd);
	}

	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "result = %d", result);
}

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_Nginx_stopNative(JNIEnv* env, jobject thiz)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_stop");

	int result = jni_stop();

	char *ngx_cmd[3] = {"nginx", "-s", "stop"};
	result = jni_main(3, ngx_cmd);

	if (prefix) {
		free(prefix);
	}
	if (host) {
		free(host);
	}

	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "result = %d", result);
}

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_setPort(JNIEnv* env, jobject thiz, jint port)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_setPort");
}

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_setHost(JNIEnv* env, jobject thiz, jstring host)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_setHost");
}

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_setConfigPath(JNIEnv* env, jobject thiz, jstring jstr_config)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_setConfigPath");
	config = jstrdup(env, jstr_config);
}

JNIEXPORT jstring JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_getPrefixPath(JNIEnv* env, jobject thiz)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_getPrefixPath");
	if (prefix != NULL) {
		return env->NewStringUTF(prefix);
	} else {
		return env->NewStringUTF(NGX_PREFIX);
	}
}

JNIEXPORT void JNICALL
Java_org_screenshare_rtmp_nginx_Nginx_setPrefixPath(JNIEnv* env, jobject thiz, jstring jstr_prefix)
{
	__android_log_print(ANDROID_LOG_ERROR, "jni_nginx", "Java_org_screenshare_rtmp_nginx_Nginx_setPrefix");
	prefix = jstrdup(env, jstr_prefix);
}

static char *jstrdup(JNIEnv* env, jstring str)
{
	char *cchar = NULL;

	const char *cchar_str = env->GetStringUTFChars(str, NULL);

	const int size = sizeof(char) * (strlen(cchar_str) + 1);
	cchar = (char *) malloc(size);
	if (cchar == NULL) {
		goto finally;
	}

	memset(cchar, 0, size);
	strcpy(cchar, cchar_str);

	finally:
	env->ReleaseStringUTFChars(str, cchar_str);

	return cchar;
}

#ifdef __cplusplus
}
#endif
