/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package one.mixin.android.crypto.ec;

import java.math.BigInteger;

/** An ECDSA Signature. */
public class ECDSASignature {
    public final BigInteger r;
    public final BigInteger s;

    public ECDSASignature(BigInteger r, BigInteger s) {
        this.r = r;
        this.s = s;
    }

    /**
     * @return true if the S component is "low", that means it is below {@link
     *     Sign#HALF_CURVE_ORDER}. See <a
     *     href="https://github.com/bitcoin/bips/blob/master/bip-0062.mediawiki#Low_S_values_in_signatures">
     *     BIP62</a>.
     */
    public boolean isCanonical() {
        return s.compareTo(Sign.HALF_CURVE_ORDER) <= 0;
    }

    /**
     * Will automatically adjust the S component to be less than or equal to half the curve order,
     * if necessary. This is reimport org.bouncycastle.util.encoders.Hex;
     * import org.web3j.crypto.*;
     * import java.math.BigInteger;
     *
     * public class ECCExample {
     *
     *     public static String compressPubKey(BigInteger pubKey) {
     *         String pubKeyYPrefix = pubKey.testBit(0) ? "03" : "02";
     *         String pubKeyHex = pubKey.toString(16);
     *         String pubKeyX = pubKeyHex.substring(0, 64);
     *         return pubKeyYPrefix + pubKeyX;
     *     }
     *
     *     public static void main(String[] args) throws Exception {
     *         //BigInteger privKey = Keys.createEcKeyPair().getPrivateKey();
     *         BigInteger privKey = new BigInteger("97ddae0f3a25b92268175400149d65d6887b9cefaf28ea2c078e05cdc15a3c0a", 16);
     *         BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
     *         ECKeyPair keyPair = new ECKeyPair(privKey, pubKey);
     *         System.out.println("Private key: " + privKey.toString(16));
     *         System.out.println("Public key: " + pubKey.toString(16));
     *         System.out.println("Public key (compressed): " + compressPubKey(pubKey));
     *
     *         String msg = "Message for signing";
     *         byte[] msgHash = Hash.sha3(msg.getBytes());
     *         Sign.SignatureData signature = Sign.signMessage(msgHash, keyPair, false);
     *         System.out.println("Msg: " + msg);
     *         System.out.println("Msg hash: " + Hex.toHexString(msgHash));
     *         System.out.printf("Signature: [v = %d, r = %s, s = %s]\n",
     *                 signature.getV() - 27,
     *                 Hex.toHexString(signature.getR()),
     *                 Hex.toHexString(signature.getS()));
     *
     *         System.out.println();
     *
     *         BigInteger pubKeyRecovered = Sign.signedMessageToKey(msg.getBytes(), signature);
     *         System.out.println("Recovered public key: " + pubKeyRecovered.toString(16));
     *
     *         boolean validSig = pubKey.equals(pubKeyRecovered);
     *         System.out.println("Signature valid? " + validSig);
     *     }
     * }quired because for every signature (r,s) the signature (r, -s (mod
     * N)) is a valid signature of the same message. However, we dislike the ability to modify the
     * bits of a Bitcoin transaction after it's been signed, as that violates various assumed
     * invariants. Thus in future only one of those forms will be considered legal and the other
     * will be banned.
     *
     * @return the signature in a canonicalised form.
     */
    public ECDSASignature toCanonicalised() {
        if (!isCanonical()) {
            // The order of the curve is the number of valid points that exist on that curve.
            // If S is in the upper half of the number of valid points, then bring it back to
            // the lower half. Otherwise, imagine that
            //    N = 10
            //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
            //    10 - 8 == 2, giving us always the latter solution, which is canonical.
            return new ECDSASignature(r, Sign.CURVE.getN().subtract(s));
        } else {
            return this;
        }
    }
}