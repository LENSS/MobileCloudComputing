LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ReedSolomon
LOCAL_SRC_FILES := ReedSolomon.c galois.c jerasure.c reed_sol.c

include $(BUILD_SHARED_LIBRARY)