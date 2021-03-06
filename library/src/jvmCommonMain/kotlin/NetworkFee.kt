package drewcarlson.walletkit

import com.breadwallet.corenative.cleaner.ReferenceCleaner
import com.breadwallet.corenative.crypto.BRCryptoNetworkFee
import com.google.common.primitives.UnsignedLong
import java.util.*

public actual class NetworkFee(
        internal val core: BRCryptoNetworkFee
) : Closeable {

    init {
        ReferenceCleaner.register(core, ::close)
    }

    internal actual constructor(
            timeIntervalInMilliseconds: ULong,
            pricePerCostFactor: Amount
    ) : this(
            BRCryptoNetworkFee.create(
                    UnsignedLong.valueOf(timeIntervalInMilliseconds.toLong()),
                    pricePerCostFactor.core,
                    pricePerCostFactor.unit.core
            )
    )

    public actual val timeIntervalInMilliseconds: ULong =
            core.confirmationTimeInMilliseconds.toLong().toULong()
    internal actual val pricePerCostFactor: Amount =
            Amount(core.pricePerCostFactor)

    actual override fun hashCode(): Int =
            Objects.hash(core.confirmationTimeInMilliseconds)

    actual override fun equals(other: Any?): Boolean =
            other is NetworkFee && core.isIdentical(other.core)

    actual override fun close() {
        core.give()
    }
}
