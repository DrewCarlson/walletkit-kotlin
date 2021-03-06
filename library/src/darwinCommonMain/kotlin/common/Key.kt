package drewcarlson.walletkit.common

import brcrypto.*
import drewcarlson.walletkit.Secret
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import drewcarlson.walletkit.Closeable
import kotlin.native.concurrent.*

public actual class Key internal constructor(
        core: BRCryptoKey,
        take: Boolean
) : Closeable {

    internal val core: BRCryptoKey =
            if (take) checkNotNull(cryptoKeyTake(core))
            else core

    internal actual constructor(
            secret: Secret
    ) : this(
            checkNotNull(cryptoKeyCreateFromSecret(secret.readValue())),
            false
    )

    init {
        freeze()
    }

    public actual val hasSecret: Boolean
        get() = CRYPTO_TRUE == cryptoKeyHasSecret(core).toUInt()

    // TODO: Clean this up
    public actual val encodeAsPrivate: ByteArray
        get() = memScoped {
            checkNotNull(cryptoKeyEncodePrivate(core)).let { coreBytes ->
                var count = 0
                while (true) {
                    if (coreBytes[count] != 0.toByte()) {
                        count++
                    } else break
                }
                ByteArray(count) { i -> coreBytes[i] }
            }
        }

    public actual val encodeAsPublic: ByteArray
        get() = memScoped {
            checkNotNull(cryptoKeyEncodePublic(core)).let { coreBytes ->
                var count = 0
                while (true) {
                    if (coreBytes[count] != 0.toByte()) {
                        count++
                    } else break
                }
                ByteArray(count) { i -> coreBytes[i] }
            }
        }

    public actual val secret: Secret
        get() = memScoped {
            cryptoKeyGetSecret(core).getPointer(this).pointed
        }

    public actual fun publicKeyMatch(that: Key): Boolean =
            CRYPTO_TRUE == cryptoKeyPublicMatch(core, that.core).toUInt()

    internal actual fun privateKeyMatch(that: Key): Boolean =
            CRYPTO_TRUE == cryptoKeySecretMatch(core, that.core).toUInt()

    override fun close() {
        cryptoKeyGive(core)
    }

    public actual companion object {
        private val atomicWordList = atomic<List<String>?>(null)

        public actual var wordList: List<String>?
            get() = atomicWordList.value
            set(value) {
                atomicWordList.value = value
            }

        public actual fun isProtectedPrivateKey(privateKey: String): Boolean =
                CRYPTO_TRUE == cryptoKeyIsProtectedPrivate(privateKey)

        public actual fun createFromPhrase(
                phrase: String,
                words: List<String>?
        ): Key? = memScoped {
            words ?: return null
            val wordsArray = words.toCStringArray(this)
            val coreKey = cryptoKeyCreateFromPhraseWithWords(phrase, wordsArray) ?: return null
            Key(coreKey, false)
        }

        public actual fun createFromProtectedPrivateKey(privateKey: String, passphrase: String): Key? =
                cryptoKeyCreateFromStringProtectedPrivate(privateKey, passphrase)
                        ?.let { coreKey -> Key(coreKey, false) }

        public actual fun createFromPrivateKey(privateKey: String): Key? =
                cryptoKeyCreateFromStringPrivate(privateKey)
                        ?.let { coreKey -> Key(coreKey, false) }

        public actual fun createFromPublicKey(string: String): Key? =
                cryptoKeyCreateFromStringPublic(string)
                        ?.let { coreKey -> Key(coreKey, false) }

        public actual fun createForPigeonFromKey(key: Key, nonce: ByteArray): Key? {
            val nonceValue = nonce.asUByteArray().toCValues()
            val coreKey = cryptoKeyCreateForPigeon(key.core, nonceValue, nonce.size.toULong())
            return Key(coreKey ?: return null, false)
        }

        public actual fun createForBIP32ApiAuth(phrase: String, words: List<String>?): Key? = memScoped {
            words ?: return null
            val wordsArray = words.toCStringArray(this)
            val coreKey = cryptoKeyCreateForBIP32ApiAuth(phrase, wordsArray) ?: return null
            Key(coreKey, false)
        }

        public actual fun createForBIP32BitID(
                phrase: String,
                index: Int,
                uri: String,
                words: List<String>?
        ): Key? = memScoped {
            words ?: return null
            val wordsArray = words.toCStringArray(this)
            val coreKey = cryptoKeyCreateForBIP32BitID(phrase, index, uri, wordsArray) ?: return null
            Key(coreKey, false)
        }
    }
}
