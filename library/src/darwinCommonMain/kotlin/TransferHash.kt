package drewcarlson.walletkit

import brcrypto.*
import kotlinx.cinterop.toKStringFromUtf8
import kotlin.native.concurrent.*

public actual class TransferHash(
        core: BRCryptoHash,
        take: Boolean
) : Closeable {

    internal val core: BRCryptoHash =
            if (take) checkNotNull(cryptoHashTake(core))
            else core

    init {
        freeze()
    }

    actual override fun equals(other: Any?): Boolean =
            other is TransferHash && CRYPTO_TRUE == cryptoHashEqual(core, other.core)

    actual override fun hashCode(): Int =
            toString().hashCode()

    actual override fun toString(): String =
            checkNotNull(cryptoHashEncodeString(core)).toKStringFromUtf8()

    actual override fun close() {
        cryptoHashGive(core)
    }
}
