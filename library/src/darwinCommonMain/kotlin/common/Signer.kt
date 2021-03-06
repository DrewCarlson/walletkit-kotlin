package drewcarlson.walletkit.common

import brcrypto.*
import brcrypto.BRCryptoSignerType.*
import drewcarlson.walletkit.Closeable
import drewcarlson.walletkit.common.SignerAlgorithm.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.usePinned
import kotlin.native.concurrent.*

public actual class Signer internal constructor(
        core: BRCryptoSigner?
) : Closeable {

    internal val core: BRCryptoSigner = requireNotNull(core)

    init {
        freeze()
    }

    public actual fun sign(digest: ByteArray, key: Key): ByteArray? {
        val privKey = key.core
        val digestBytes = digest.asUByteArray().toCValues()
        val digestLength = digestBytes.size.toULong()
        require(digestLength == 32uL)

        val targetLength = cryptoSignerSignLength(core, privKey, digestBytes, digestLength)
        if (targetLength == 0uL) return null
        val target = UByteArray(targetLength.toInt())

        val result = target.usePinned {
            cryptoSignerSign(core, privKey, it.addressOf(0), targetLength, digestBytes, digestLength)
        }
        return if (result == CRYPTO_TRUE) {
            target.asByteArray()
        } else null
    }

    public actual fun recover(digest: ByteArray, signature: ByteArray): Key? {
        val digestBytes = digest.asUByteArray().toCValues()
        val digestLength = digest.size.toULong()
        require(digestBytes.size == 32)

        val signatureBytes = signature.asUByteArray().toCValues()
        val signatureLength = signatureBytes.size.toULong()
        val coreKey = cryptoSignerRecover(core, digestBytes, digestLength, signatureBytes, signatureLength)
        return Key(coreKey ?: return null, false)
    }

    actual override fun close() {
        cryptoSignerGive(core)
    }

    public actual companion object {
        public actual fun createForAlgorithm(algorithm: SignerAlgorithm): Signer =
                when (algorithm) {
                    BASIC_DER -> CRYPTO_SIGNER_BASIC_DER
                    BASIC_JOSE -> CRYPTO_SIGNER_BASIC_JOSE
                    COMPACT -> CRYPTO_SIGNER_COMPACT
                }.run(::cryptoSignerCreate)
                        .run(::Signer)
    }
}
