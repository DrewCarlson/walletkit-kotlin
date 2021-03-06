package drewcarlson.walletkit

import brcrypto.*
import brcrypto.BRCryptoWalletState.CRYPTO_WALLET_STATE_CREATED
import brcrypto.BRCryptoWalletState.CRYPTO_WALLET_STATE_DELETED
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlin.native.concurrent.*

public actual class Wallet internal constructor(
        core: BRCryptoWallet,
        public actual val manager: WalletManager,
        public actual val scope: CoroutineScope,
        take: Boolean
) {

    internal val core: BRCryptoWallet =
            if (take) checkNotNull(cryptoWalletTake(core))
            else core

    init {
        freeze()
    }

    public actual val system: System
        get() = manager.system

    public actual val unit: WKUnit
        get() = WKUnit(checkNotNull(cryptoWalletGetUnit(core)), false)

    public actual val unitForFee: WKUnit
        get() = WKUnit(checkNotNull(cryptoWalletGetUnitForFee(core)), false)

    public actual val balance: Amount
        get() = Amount(checkNotNull(cryptoWalletGetBalance(core)), false)

    public actual val transfers: List<Transfer>
        get() = memScoped {
            val count = alloc<ULongVar>()
            val coreTransfers = cryptoWalletGetTransfers(core, count.ptr)
            if (coreTransfers == null) {
                emptyList()
            } else {
                defer { cryptoMemoryFree(coreTransfers) }

                List(count.value.toInt()) { i ->
                    Transfer(checkNotNull(coreTransfers[i]), this@Wallet, false)
                }
            }
        }

    public actual fun getTransferByHash(hash: TransferHash?): Transfer? =
            transfers.singleOrNull { it.hash == hash }

    public actual val target: Address
        get() = getTargetForScheme(manager.addressScheme)

    public actual fun getTargetForScheme(scheme: AddressScheme): Address {
        val coreAddress = checkNotNull(cryptoWalletGetAddress(core, scheme.toCore()))
        return Address(coreAddress, false)
    }

    public actual val currency: Currency
        get() = Currency(checkNotNull(cryptoWalletGetCurrency(core)), false)

    public actual val name: String
        get() = unit.currency.name

    public actual val state: WalletState
        get() = when (cryptoWalletGetState(core)) {
            CRYPTO_WALLET_STATE_CREATED -> WalletState.CREATED
            CRYPTO_WALLET_STATE_DELETED -> WalletState.DELETED
        }

    public actual fun hasAddress(address: Address): Boolean {
        return cryptoWalletHasAddress(core, address.core)
    }

    /*internal actual fun createTransferFeeBasis(
            pricePerCostFactor: Amount,
            costFactor: Double
    ): TransferFeeBasis? {
        val coreFeeBasis = cryptoWalletCreateFeeBasis(core, pricePerCostFactor.core, costFactor)
        return TransferFeeBasis(coreFeeBasis ?: return null, false)
    }*/

    public actual fun createTransfer(
            target: Address,
            amount: Amount,
            estimatedFeeBasis: TransferFeeBasis,
            transferAttributes: Set<TransferAttribute>
    ): Transfer? = memScoped {
        val attrs = transferAttributes.map(TransferAttribute::core).toCValues()
        val count = attrs.size.toULong()
        val coreTransfer = cryptoWalletCreateTransfer(core, target.core, amount.core, estimatedFeeBasis.core, count, attrs)
        Transfer(coreTransfer ?: return null, this@Wallet, false)
    }

    internal fun transferBy(core: BRCryptoTransfer): Transfer? {
        return if (CRYPTO_TRUE == cryptoWalletHasTransfer(this.core, core)) {
            Transfer(core, this, true)
        } else null
    }

    internal fun transferByCoreOrCreate(core: BRCryptoTransfer): Transfer? {
        return transferBy(core) ?: Transfer(core, this, true)
    }

    public actual suspend fun estimateFee(
            target: Address,
            amount: Amount,
            fee: NetworkFee,
            attributes: Set<TransferAttribute>
    ): TransferFeeBasis {
        val attrsLength = 0uL
        val attrs = attributes.map { it.core }.toCValues()
        cryptoWalletManagerEstimateFeeBasis(
            manager.core, core, null, target.core, amount.core, fee.core, attrsLength, attrs)

        TODO("Not implemented")
    }

    public actual suspend fun estimateLimitMaximum(target: Address, fee: NetworkFee): Amount {
        TODO()
    }

    public actual suspend fun estimateLimitMinimum(target: Address, fee: NetworkFee): Amount {
        TODO()
    }

    actual override fun equals(other: Any?): Boolean =
            other is Wallet && CRYPTO_TRUE == cryptoWalletEqual(core, other.core)

    actual override fun hashCode(): Int = core.hashCode()

    internal fun getTransfer(coreTransfer: BRCryptoTransfer): Transfer? {
        return if (cryptoWalletHasTransfer(core, coreTransfer) == CRYPTO_TRUE) {
            Transfer(coreTransfer, this, true)
        } else null
    }
}
