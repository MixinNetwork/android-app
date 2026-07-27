/**
 * Copyright (C) 2014-2016 Open Whisper Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */


#include <string.h>
#include <stdint.h>

#include <jni.h>
#include "curve25519-donna.h"
#include "curve_sigs.h"
#include "xeddsa.h"
#include "internal_fast_tests.h"
#include "gen_x.h"

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_generatePrivateKey
  (JNIEnv *env, jobject obj, jbyteArray random)
{
  uint8_t* privateKey = (uint8_t*)(*env)->GetByteArrayElements(env, random, 0);

  privateKey[0] &= 248;
  privateKey[31] &= 127;
  privateKey[31] |= 64;

  (*env)->ReleaseByteArrayElements(env, random, privateKey, 0);

  return random;
}

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_generatePublicKey
  (JNIEnv *env, jobject obj, jbyteArray privateKey)
{
    static const uint8_t  basepoint[32] = {9};

    jbyteArray publicKey       = (*env)->NewByteArray(env, 32);
    uint8_t*   publicKeyBytes  = (uint8_t*)(*env)->GetByteArrayElements(env, publicKey, 0);
    uint8_t*   privateKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, privateKey, 0);

    curve25519_donna(publicKeyBytes, privateKeyBytes, basepoint);

    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, privateKey, privateKeyBytes, 0);

    return publicKey;
}

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_calculateAgreement
  (JNIEnv *env, jobject obj, jbyteArray privateKey, jbyteArray publicKey)
{
    jbyteArray sharedKey       = (*env)->NewByteArray(env, 32);
    uint8_t*   sharedKeyBytes  = (uint8_t*)(*env)->GetByteArrayElements(env, sharedKey, 0);
    uint8_t*   privateKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, privateKey, 0);
    uint8_t*   publicKeyBytes  = (uint8_t*)(*env)->GetByteArrayElements(env, publicKey, 0);

    curve25519_donna(sharedKeyBytes, privateKeyBytes, publicKeyBytes);

    (*env)->ReleaseByteArrayElements(env, sharedKey, sharedKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, privateKey, privateKeyBytes, 0);

    return sharedKey;
}

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_calculateSignature
  (JNIEnv *env, jobject obj, jbyteArray random, jbyteArray privateKey, jbyteArray message)
{
    jbyteArray signature       = (*env)->NewByteArray(env, 64);
    uint8_t*   signatureBytes  = (uint8_t*)(*env)->GetByteArrayElements(env, signature, 0);
    uint8_t*   randomBytes     = (uint8_t*)(*env)->GetByteArrayElements(env, random, 0);
    uint8_t*   privateKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, privateKey, 0);
    uint8_t*   messageBytes    = (uint8_t*)(*env)->GetByteArrayElements(env, message, 0);
    jsize      messageLength   = (*env)->GetArrayLength(env, message);

    int result = xed25519_sign(signatureBytes, privateKeyBytes, messageBytes, messageLength, randomBytes);

    (*env)->ReleaseByteArrayElements(env, signature, signatureBytes, 0);
    (*env)->ReleaseByteArrayElements(env, random, randomBytes, 0);
    (*env)->ReleaseByteArrayElements(env, privateKey, privateKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, message, messageBytes, 0);

    if (result == 0) return signature;

    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/AssertionError"), "Signature failed!");
    return NULL;
}

JNIEXPORT jboolean JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_verifySignature
  (JNIEnv *env, jobject obj, jbyteArray publicKey, jbyteArray message, jbyteArray signature)
{
    uint8_t*   signatureBytes = (uint8_t*)(*env)->GetByteArrayElements(env, signature, 0);
    uint8_t*   publicKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, publicKey, 0);
    uint8_t*   messageBytes   = (uint8_t*)(*env)->GetByteArrayElements(env, message, 0);
    jsize      messageLength  = (*env)->GetArrayLength(env, message);

    jboolean result = (curve25519_verify(signatureBytes, publicKeyBytes, messageBytes, messageLength) == 0);

    (*env)->ReleaseByteArrayElements(env, signature, signatureBytes, 0);
    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, message, messageBytes, 0);

    return result;
}

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_calculateVrfSignature
  (JNIEnv *env, jobject obj, jbyteArray random, jbyteArray privateKey, jbyteArray message)
{
    jbyteArray signature       = (*env)->NewByteArray(env, 96);
    uint8_t*   signatureBytes  = (uint8_t*)(*env)->GetByteArrayElements(env, signature, 0);
    uint8_t*   randomBytes     = (uint8_t*)(*env)->GetByteArrayElements(env, random, 0);
    uint8_t*   privateKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, privateKey, 0);
    uint8_t*   messageBytes    = (uint8_t*)(*env)->GetByteArrayElements(env, message, 0);
    jsize      messageLength   = (*env)->GetArrayLength(env, message);

    int result = generalized_xveddsa_25519_sign(signatureBytes, privateKeyBytes, messageBytes, messageLength, randomBytes, NULL, 0);

    (*env)->ReleaseByteArrayElements(env, signature, signatureBytes, 0);
    (*env)->ReleaseByteArrayElements(env, random, randomBytes, 0);
    (*env)->ReleaseByteArrayElements(env, privateKey, privateKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, message, messageBytes, 0);

    if (result == 0) return signature;

    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/AssertionError"), "Signature failed!");
    return NULL;
}

JNIEXPORT jbyteArray JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_verifyVrfSignature
  (JNIEnv *env, jobject obj, jbyteArray publicKey, jbyteArray message, jbyteArray signature)
{
    uint8_t*   signatureBytes = (uint8_t*)(*env)->GetByteArrayElements(env, signature, 0);
    uint8_t*   publicKeyBytes = (uint8_t*)(*env)->GetByteArrayElements(env, publicKey, 0);
    uint8_t*   messageBytes   = (uint8_t*)(*env)->GetByteArrayElements(env, message, 0);
    jsize      messageLength  = (*env)->GetArrayLength(env, message);

    jbyteArray vrf      = (*env)->NewByteArray(env, 32);
    uint8_t*   vrfBytes = (uint8_t*)(*env)->GetByteArrayElements(env, vrf, 0);

    int result = generalized_xveddsa_25519_verify(vrfBytes, signatureBytes, publicKeyBytes, messageBytes, messageLength, NULL, 0);

    (*env)->ReleaseByteArrayElements(env, signature, signatureBytes, 0);
    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
    (*env)->ReleaseByteArrayElements(env, message, messageBytes, 0);
    (*env)->ReleaseByteArrayElements(env, vrf, vrfBytes, 0);

    if (result == 0) return vrf;

    (*env)->ThrowNew(env, (*env)->FindClass(env, "org/whispersystems/curve25519/VrfSignatureVerificationFailedException"), "Invalid signature");
    return NULL;
}


JNIEXPORT jboolean JNICALL Java_org_whispersystems_curve25519_NativeCurve25519Provider_smokeCheck
  (JNIEnv *env, jobject obj, jint dummy)
{
    return 1;
}

JNIEXPORT jboolean JNICALL Java_org_whispersystems_curve25519_NativeCurve25519ProviderTest_internalTest
  (JNIEnv *env)
{
    jboolean result = (all_fast_tests(1) == 0);
    return result;
}
