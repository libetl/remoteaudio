/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder */

#ifndef _Included_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
#define _Included_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    closeHandle
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_closeHandleNative
  (JNIEnv *, jobject, jint);

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    openData
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_openDataNative
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    openFile
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_openFileNative
  (JNIEnv *, jobject, jstring);

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    readSamples
 * Signature: (ILjava/nio/FloatBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_readSamplesNative__ILjava_nio_FloatBuffer_2I
  (JNIEnv *, jobject, jint, jobject, jint);

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    readSamples
 * Signature: (ILjava/nio/ShortBuffer;I)I
 */
JNIEXPORT jint JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_readSamplesNative__ILjava_nio_ShortBuffer_2I
  (JNIEnv *, jobject, jint, jobject, jint);

/*
 * Class:     org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder
 * Method:    readSamples
 * Signature: (II)[S
 */
JNIEXPORT jshortArray JNICALL Java_org_toilelibre_libe_remoteaudio_audio_format_MP3Decoder_readSamplesNative__II
  (JNIEnv *, jobject, jint, jint);


#ifdef __cplusplus
}
#endif
#endif