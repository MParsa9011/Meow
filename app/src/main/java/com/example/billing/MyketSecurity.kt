package com.example.billing

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * RSA signature verification compatible with Myket In-App Billing v3 (TrivialDrive Security port).
 */
object MyketSecurity {
    private const val TAG = "MyketSecurity"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"
    private const val PLACEHOLDER_KEY = "REPLACE_WITH_MYKET_RSA_PUBLIC_KEY"

    /**
     * Reconstructs the public key at runtime from BuildConfig rather than storing a literal key in code.
     */
    fun resolveRuntimePublicKey(): String {
        val raw = BuildConfig.MYKET_PUBLIC_KEY.trim()
        if (raw.isEmpty() || raw == PLACEHOLDER_KEY) return ""
        return buildString(raw.length) {
            for (ch in raw) {
                if (!ch.isWhitespace()) append(ch)
            }
        }
    }

    fun isPublicKeyConfigured(): Boolean = resolveRuntimePublicKey().isNotEmpty()

    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String?): Boolean {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) || TextUtils.isEmpty(signature)) {
            Log.e(TAG, "Purchase verification failed: missing data.")
            return false
        }
        return try {
            val key = generatePublicKey(base64PublicKey)
            verify(key, signedData, signature!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying purchase signature: ${e.message}")
            false
        }
    }

    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        return try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Invalid key specification.")
            throw IllegalArgumentException(e)
        }
    }

    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed.")
            return false
        }
        return try {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(publicKey)
            sig.update(signedData.toByteArray(Charsets.UTF_8))
            if (!sig.verify(signatureBytes)) {
                Log.e(TAG, "Signature verification failed.")
                false
            } else {
                true
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException.")
            false
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key specification.")
            false
        } catch (e: SignatureException) {
            Log.e(TAG, "Signature exception.")
            false
        }
    }
}
