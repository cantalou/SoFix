#include <jni.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_wy_sofix_app_NativeLib_getString(JNIEnv *env, jclass type) {
    return env->NewStringUTF("test");
}