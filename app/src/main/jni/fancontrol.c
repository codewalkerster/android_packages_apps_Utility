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

#define LOG_TAG "fancontrol"
#define LOGE(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

#define FAN_NODE    "/sys/devices/odroid_fan.14/"

#define AUTO        1
#define MANUAL      0
#define ENABLE      1
#define DIASABLE    0

jstring Java_com_hardkernel_odroid_MainActivity_readFanMode(JNIEnv* env, jobject obj) {
    FILE *fp;
    char buf[32] = {'\0',};
    if ((fp = fopen(FAN_NODE "fan_mode", "r")) == NULL) {
        LOGE(FAN_NODE "fan_mode not found");
        return NULL;
    }

    fread(buf, 1, 32, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setFanMode(JNIEnv* env, jobject obj, jint auto_manual) {
    FILE *fp;

    if ((fp = fopen(FAN_NODE "fan_mode", "w")) == NULL) {
        LOGE(FAN_NODE "fan_mode not found");
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
    FILE *fp;
    char buf[16] = {'\0',};
    if ((fp = fopen(FAN_NODE "fan_speeds", "r")) == NULL) {
        LOGE(FAN_NODE "fan_speeds not found");
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setFanSpeeds(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp;

    if ((fp = fopen(FAN_NODE "fan_speeds", "w")) == NULL) {
        LOGE(FAN_NODE "fan_speeds not found");
        return;
    }

    const char *speeds = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", speeds);

    fwrite(speeds, 1, strlen(speeds), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, speeds);
}

jstring Java_com_hardkernel_odroid_MainActivity_readPWMDuty(JNIEnv* env, jobject obj) {
    FILE *fp;
    char buf[16] = {'\0',};
    if ((fp = fopen(FAN_NODE "pwm_duty", "r")) == NULL) {
        LOGE(FAN_NODE "pwm_duty not found");
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setPWMDuty(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp;

    if ((fp = fopen(FAN_NODE "pwm_duty", "w")) == NULL) {
        LOGE(FAN_NODE "pwm_duty not found");
        return;
    }

    const char *duty = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", duty);

    fwrite(duty, 1, strlen(duty), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, duty);
}

jstring Java_com_hardkernel_odroid_MainActivity_readPWMEnable(JNIEnv* env, jobject obj) {
    FILE *fp;
    char buf[16] = {'\0',};
    if ((fp = fopen(FAN_NODE "pwm_enable", "r")) == NULL) {
        LOGE(FAN_NODE "pwm_enable not found");
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}

void Java_com_hardkernel_odroid_MainActivity_setPWMEnable(JNIEnv* env, jobject obj, jint enable) {
    FILE *fp;

    if ((fp = fopen(FAN_NODE "pwm_enable", "w")) == NULL) {
        LOGE(FAN_NODE "pwm_enable not found");
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
    FILE *fp;
    char buf[16] = {'\0',};
    if ((fp = fopen(FAN_NODE "temp_levels", "r")) == NULL) {
        LOGE(FAN_NODE "temp_levels not found");
        return NULL;
    }

    fread(buf, 1, 16, fp);
    fclose(fp);

    return (*env)->NewStringUTF(env, buf);
}


void Java_com_hardkernel_odroid_MainActivity_setTempLevels(JNIEnv* env, jobject obj, jstring values) {
    FILE *fp;

    if ((fp = fopen(FAN_NODE "temp_levels", "w")) == NULL) {
        LOGE(FAN_NODE "temp_levels not found");
        return;
    }

    const char *temps = (*env)->GetStringUTFChars(env, values, NULL);

    LOGE("%s", temps);

    fwrite(temps, 1, strlen(temps), fp);

    fclose(fp);

    (*env)->ReleaseStringUTFChars(env, values, temps);
}
