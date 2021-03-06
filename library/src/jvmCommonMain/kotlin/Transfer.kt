package drewcarlson.walletkit

import com.breadwallet.corenative.cleaner.*
import com.breadwallet.corenative.crypto.*
import com.breadwallet.corenative.crypto.BRCryptoTransferDirection.*
import com.breadwallet.corenative.crypto.BRCryptoTransferStateType.*
import java.util.*

public actual class Transfer internal constructor(
    internal val core: BRCryptoTransfer,
    public actual val wallet: Wallet
) {

    init {
        ReferenceCleaner.register(core, core::give)
    }

    public actual val source: Address?
        get() = core.sourceAddress.orNull()?.run(::Address)

    public actual val target: Address?
        get() = core.targetAddress.orNull()?.run(::Address)

    public actual val amount: Amount
        get() = Amount(core.amount)

    public actual val amountDirected: Amount
        get() = Amount(core.amountDirected)

    public actual val fee: Amount
        get() = checkNotNull(confirmedFeeBasis?.fee ?: estimatedFeeBasis?.fee) {
            "Missed confirmed+estimated feeBasis"
        }

    public actual val estimatedFeeBasis: TransferFeeBasis?
        get() = core.estimatedFeeBasis.orNull()?.run(::TransferFeeBasis)

    public actual val confirmedFeeBasis: TransferFeeBasis?
        get() = core.confirmedFeeBasis.orNull()?.run(::TransferFeeBasis)

    public actual val direction: TransferDirection by lazy {
        when (core.direction) {
            CRYPTO_TRANSFER_SENT -> TransferDirection.SENT
            CRYPTO_TRANSFER_RECEIVED -> TransferDirection.RECEIVED
            CRYPTO_TRANSFER_RECOVERED -> TransferDirection.RECOVERED
            else -> error("Unknown core transfer direction (${core.direction})")
        }
    }

    public actual val hash: TransferHash?
        get() = core.hash.orNull()?.run(::TransferHash)

    public actual val unit: WKUnit
        get() = WKUnit(core.unitForAmount)

    public actual val unitForFee: WKUnit
        get() = WKUnit(core.unitForFee)

    public actual val confirmation: TransferConfirmation?
        get() = (state as? TransferState.INCLUDED)?.confirmation

    public actual val confirmations: ULong?
        get() = getConfirmationsAt(wallet.manager.network.height)

    public actual val state: TransferState
        get() = when (core.state.type()) {
            CRYPTO_TRANSFER_STATE_CREATED -> TransferState.CREATED
            CRYPTO_TRANSFER_STATE_SIGNED -> TransferState.SIGNED
            CRYPTO_TRANSFER_STATE_SUBMITTED -> TransferState.SUBMITTED
            CRYPTO_TRANSFER_STATE_DELETED -> TransferState.DELETED
            CRYPTO_TRANSFER_STATE_INCLUDED ->
                core.state.included().let { included ->
                    TransferState.INCLUDED(
                        TransferConfirmation(
                            blockNumber = included.blockNumber.toLong().toULong(),
                            transactionIndex = included.transactionIndex.toLong().toULong(),
                            timestamp = included.blockTimestamp.toLong().toULong(),
                            fee = Amount(checkNotNull(included.feeBasis.fee.orNull())),
                            success = included.success,
                            error = included.error.orNull()
                        )
                    )
                }
            CRYPTO_TRANSFER_STATE_ERRORED ->
                core.state.errored().let { coreError ->
                    TransferState.FAILED(
                        error = when (coreError.type()) {
                            BRCryptoTransferSubmitErrorType.CRYPTO_TRANSFER_SUBMIT_ERROR_UNKNOWN ->
                                TransferSubmitError.UNKNOWN
                            BRCryptoTransferSubmitErrorType.CRYPTO_TRANSFER_SUBMIT_ERROR_POSIX ->
                                TransferSubmitError.POSIX(
                                    errNum = coreError.u.posix.errnum,
                                    errMessage = coreError.message.orNull()
                                )
                            else -> error("Unknown core error type (${coreError.type()})")
                        }
                    )
                }
            else -> error("Unknown core transfer state type (${core.state.type()})")
        }

    public actual fun getConfirmationsAt(blockHeight: ULong): ULong? {
        return confirmation?.run {
            if (blockHeight >= blockNumber) {
                1u + blockHeight - blockNumber
            } else null
        }
    }

    actual override fun equals(other: Any?): Boolean =
        other is Transfer && core == other.core

    actual override fun hashCode(): Int = Objects.hash(core)
}
