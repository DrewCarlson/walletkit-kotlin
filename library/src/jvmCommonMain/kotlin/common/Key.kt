package drewcarlson.walletkit.common

import drewcarlson.walletkit.Secret
import com.breadwallet.corenative.cleaner.ReferenceCleaner
import com.breadwallet.corenative.crypto.BRCryptoKey
import drewcarlson.walletkit.Closeable

public actual class Key internal constructor(
        internal val core: BRCryptoKey
) : Closeable {

    init {
        ReferenceCleaner.register(core, ::close)
    }

    internal actual constructor(
            secret: Secret
    ) : this(
            checkNotNull(
                    BRCryptoKey.cryptoKeyCreateFromSecret(secret.u8).orNull()
            )
    )

    public actual val hasSecret: Boolean
        get() = core.hasSecret()

    public actual val encodeAsPrivate: ByteArray
        get() = checkNotNull(core.encodeAsPrivate())

    public actual val encodeAsPublic: ByteArray
        get() = checkNotNull(core.encodeAsPublic())

    public actual val secret: Secret
        get() = Secret(core.secret)

    public actual fun publicKeyMatch(that: Key): Boolean =
            core.publicKeyMatch(that.core)

    internal actual fun privateKeyMatch(that: Key): Boolean =
            core.privateKeyMatch(that.core)

    override fun close() {
        core.give()
    }

    public actual companion object {
        public actual var wordList: List<String>? = null
            @Synchronized get
            @Synchronized set

        public actual fun isProtectedPrivateKey(privateKey: String): Boolean =
                BRCryptoKey.isProtectedPrivateKeyString(privateKey.toByteArray())

        public actual fun createFromPhrase(phrase: String, words: List<String>?): Key? =
                if (words == null && wordList == null) null
                else BRCryptoKey.createFromPhrase(phrase.toByteArray(), words)
                        .orNull()
                        ?.run(::Key)

        public actual fun createFromProtectedPrivateKey(privateKey: String, passphrase: String): Key? =
                BRCryptoKey.createFromPrivateKeyString(privateKey.toByteArray(), passphrase.toByteArray())
                        .orNull()
                        ?.run(::Key)

        public actual fun createFromPrivateKey(privateKey: String): Key? =
                BRCryptoKey.createFromPrivateKeyString(privateKey.toByteArray())
                        .orNull()
                        ?.run(::Key)

        public actual fun createFromPublicKey(string: String): Key? =
                BRCryptoKey.createFromPublicKeyString(string.toByteArray())
                        .orNull()
                        ?.run(::Key)

        public actual fun createForPigeonFromKey(key: Key, nonce: ByteArray): Key? =
                BRCryptoKey.createForPigeon(key.core, nonce).orNull()?.run(::Key)

        public actual fun createForBIP32ApiAuth(phrase: String, words: List<String>?): Key? =
                if (words == null && wordList == null) null
                else BRCryptoKey.createForBIP32ApiAuth(phrase.toByteArray(), words)
                        .orNull()
                        ?.run(::Key)

        public actual fun createForBIP32BitID(phrase: String, index: Int, uri: String, words: List<String>?): Key? =
                if (words == null && wordList == null) null
                else BRCryptoKey.createForBIP32BitID(phrase.toByteArray(), index, uri, words)
                        .orNull()
                        ?.run(::Key)
    }
}
