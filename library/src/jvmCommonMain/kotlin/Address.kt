package drewcarlson.walletkit

import com.breadwallet.corenative.cleaner.ReferenceCleaner
import com.breadwallet.corenative.crypto.BRCryptoAddress

public actual class Address internal constructor(
        internal val core: BRCryptoAddress
) : Closeable {

    init {
        ReferenceCleaner.register(core, ::close)
    }

    actual override fun equals(other: Any?): Boolean =
            other is Address && core.isIdentical(other.core)

    actual override fun hashCode(): Int = toString().hashCode()
    actual override fun toString(): String = core.toString()

    actual override fun close() {
        core.give()
    }

    public actual companion object {
        public actual fun create(string: String, network: Network): Address? {
            return BRCryptoAddress.create(string, network.core)
                    ?.orNull()
                    ?.run(::Address)
        }
    }
}
