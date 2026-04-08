#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aicall_companion_assistant_NativeLocalLlmBridge_getNativeEngineStatus(
    JNIEnv *env,
    jobject /* this */
) {
    std::string message = "llama.cpp native bridge scaffold loaded";
    return env->NewStringUTF(message.c_str());
}
