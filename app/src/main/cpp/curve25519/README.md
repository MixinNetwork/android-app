# Curve25519 JNI

The JNI sources are vendored from
https://github.com/signalapp/curve25519-java at revision
`70fae57d6dccff7e78a46203c534314b07dfdd98` (`0.5.0`).

They are compiled by the app CMake build with Android NDK r28c. See `LICENSE`
for the upstream GPLv3 license.

The JNI wrapper adds explicit null returns after Java exceptions for modern
Clang control-flow validation; the cryptographic implementation is unchanged.
