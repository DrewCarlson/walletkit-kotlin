package drewcarlson.walletkit

import com.breadwallet.corenative.cleaner.ReferenceCleaner
import com.breadwallet.corenative.crypto.BRCryptoAccount
import com.google.common.primitives.UnsignedLong


public actual class Account(
        internal val core: BRCryptoAccount
) : Closeable {

    init {
        ReferenceCleaner.register(core, ::close)
    }

    public actual val uids: String get() = core.uids

    // NOTE: java.util.Date.getTime(): Long is in milliseconds
    public actual val timestamp: Long get() = (core.timestamp.time / 1000)
    public actual val serialize: ByteArray get() = core.serialize()
    public actual val filesystemIdentifier: String get() = core.filesystemIdentifier

    public actual fun validate(serialization: ByteArray): Boolean =
            core.validate(serialization)

    public actual fun isInitialized(network: Network): Boolean {
        return core.isInitialized(network.core)
    }

    public actual fun getInitializationData(network: Network): ByteArray {
        return core.getInitializationData(network.core)
    }

    public actual fun initialize(network: Network, data: ByteArray) {
        core.initialize(network.core, data)
    }

    actual override fun close() {
        core.give()
    }

    public actual companion object {
        public actual fun createFromPhrase(
                phrase: ByteArray,
                timestamp: Long,
                uids: String
        ): Account? = BRCryptoAccount.createFromPhrase(
                phrase,
                UnsignedLong.valueOf(timestamp),
                uids
        ).orNull()?.run(::Account)

        public actual fun createFromSerialization(serialization: ByteArray, uids: String): Account? =
                BRCryptoAccount.createFromSerialization(serialization, uids).orNull()?.run(::Account)

        public actual fun generatePhrase(words: List<String>): ByteArray? =
                runCatching { BRCryptoAccount.generatePhrase(words) }.getOrNull()

        public actual fun validatePhrase(phrase: ByteArray, words: List<String>): Boolean =
                BRCryptoAccount.validatePhrase(phrase, words)
    }
}
