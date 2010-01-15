#include "net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer.h"

#import <CoreVideo/CVPixelBuffer.h>

JNIEXPORT jbyteArray JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getBytes
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    CVPixelBufferRef pixelBuffer;
    size_t byteCount;
    jbyteArray bytes;

    pixelBuffer = (CVPixelBufferRef) ptr;

    byteCount
        = CVPixelBufferGetBytesPerRow(pixelBuffer)
            * CVPixelBufferGetHeight(pixelBuffer);
    bytes = (*jniEnv)->NewByteArray(jniEnv, byteCount);
    if (!bytes)
        return NULL;

    if (kCVReturnSuccess == CVPixelBufferLockBaseAddress(pixelBuffer, 0))
    {
        jbyte *cBytes;

        cBytes = CVPixelBufferGetBaseAddress(pixelBuffer);
        (*jniEnv)->SetByteArrayRegion(jniEnv, bytes, 0, byteCount, cBytes);
        CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    }
    return bytes;
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getHeight
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    return (jint) CVPixelBufferGetHeight((CVPixelBufferRef) ptr);
}

JNIEXPORT jint JNICALL
Java_net_java_sip_communicator_impl_neomedia_quicktime_CVPixelBuffer_getWidth
    (JNIEnv *jniEnv, jclass clazz, jlong ptr)
{
    return (jint) CVPixelBufferGetWidth((CVPixelBufferRef) ptr);
}