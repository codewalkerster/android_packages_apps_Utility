#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <linux/ioctl.h>
#include <dirent.h>

#define LOG_TAG "fancontrol"
#define LOGE(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define AUTO        1
#define MANUAL      0
#define ENABLE      1
#define DIASABLE    0

char fan_node[32];

jstring Java_com_hardkernel_odroid_MainActivity_readFanMode(JNIEnv* env, jobject obj) {
    DIR *dp = NULL;
    FILE *fp = NULL;
    char buf[32] = {'\0',};
    struct dirent *ep = NULL;
    char fullpath[64] = {'\0',};
    dp = opendir("/sys/devices/");
    if (dp != NULL) {
        while (ep = readdir(dp)) {
            if (strncmp(ep->d_name, "odroid_fan.", 11) == 0) {
                strcpy(fan_node, ep->d_name);
                sprintf(fullpath, "/sys/devices/%s/fan_mode", fan_node);
                if ((fp = fopen(fullpath, "r")) == NULL) {
                    LOGE("%s not found", fullpath);
                    return NULL;
                }
            }
        }
    }

    fread(buf, 1, 32, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setFanMode(JNIEnv* env, jobject obj, jint auto_manual) {
    FILE *fp = NULL;
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/fan_mode", fan_node);

    if ((fp = fopen(fullpath, "w")) == NULL) {
        LOGE("%s not found", fullpath);
        return;
    }

    if (auto_manual == AUTO) {
        fwrite("1", 1, 1, fp);
        LOGE("AUTO");
    } else {
        fwrite("0", 1, 1, fp);
        LOGE("MANUAL");
    }

    fclose(fp);
}

jstring Java_com_hardkernel_odroid_MainActivity_readFanSpeeds(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[16] = {'\0',};
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/fan_speeds", fan_node);

    if ((fp = fopen(fullpath, "r")) == NULL) {
        LOGE("%s not found", fullpath);
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setFanSpeeds(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp = NULL;
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/fan_speeds", fan_node);

    if ((fp = fopen(fullpath, "w")) == NULL) {
        LOGE("%s not found", fullpath);
        return;
    }

    const char *speeds = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", speeds);

    fwrite(speeds, 1, strlen(speeds), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, speeds);
}

jstring Java_com_hardkernel_odroid_MainActivity_readPWMDuty(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[16] = {'\0',};
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/pwm_duty", fan_node);

    if ((fp = fopen(fullpath, "r")) == NULL) {
        LOGE("%s not found", fullpath);
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setPWMDuty(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp = NULL;
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/pwm_duty", fan_node);

    if ((fp = fopen(fullpath, "w")) == NULL) {
        LOGE("%s not found", fullpath);
        return;
    }

    const char *duty = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", duty);

    fwrite(duty, 1, strlen(duty), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, duty);
}

jstring Java_com_hardkernel_odroid_MainActivity_readPWMEnable(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[16] = {'\0',};
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/pwm_enable", fan_node);

    if ((fp = fopen(fullpath, "r")) == NULL) {
        LOGE("%s not found", fullpath);
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setPWMEnable(JNIEnv* env, jobject obj, jint enable) {
    FILE *fp = NULL;
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/pwm_enable", fan_node);

    if ((fp = fopen(fullpath, "w")) == NULL) {
        LOGE("%s not found", fullpath);
        return;
    }

    if (enable == ENABLE) {
        fwrite("1", 1, 1, fp);
        LOGE("Enable");
    } else {
        fwrite("0", 1, 1, fp);
        LOGE("Disable");
    }

    fclose(fp);
}

jstring Java_com_hardkernel_odroid_MainActivity_readTempLevels(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[16] = {'\0',};
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/temp_levels", fan_node);

    if ((fp = fopen(fullpath, "r")) == NULL) {
        LOGE("%s not found", fullpath);
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setTempLevels(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp = NULL;
    char fullpath[64] = {'\0',};
    sprintf(fullpath, "/sys/devices/%s/temp_levels", fan_node);

    if ((fp = fopen(fullpath, "w")) == NULL) {
        LOGE("%s not found", fullpath);
        return;
    }

    const char *temps = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", temps);

    fwrite(temps, 1, strlen(temps), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, temps);
}
