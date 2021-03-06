package drewcarlson.walletkit.common

import com.breadwallet.corenative.cleaner.ReferenceCleaner
import com.breadwallet.corenative.crypto.BRCryptoHasher
import drewcarlson.walletkit.Closeable

public actual class Hasher internal constructor(
        core: BRCryptoHasher?
) : Closeable {

    internal val core: BRCryptoHasher = checkNotNull(core)

    init {
        ReferenceCleaner.register(core, ::close)
    }

    public actual fun hash(data: ByteArray): ByteArray? =
            core.hash(data).orNull()

    actual override fun close() {
        core.give()
    }

    public actual companion object {
        public actual fun createForAlgorithm(algorithm: HashAlgorithm): Hasher =
                when (algorithm) {
                    HashAlgorithm.SHA1 -> BRCryptoHasher.createSha1()
                    HashAlgorithm.SHA224 -> BRCryptoHasher.createSha224()
                    HashAlgorithm.SHA256 -> BRCryptoHasher.createSha256()
                    HashAlgorithm.SHA256_2 -> BRCryptoHasher.createSha256_2()
                    HashAlgorithm.SHA384 -> BRCryptoHasher.createSha384()
                    HashAlgorithm.SHA512 -> BRCryptoHasher.createSha512()
                    HashAlgorithm.SHA3 -> BRCryptoHasher.createSha3()
                    HashAlgorithm.RMD160 -> BRCryptoHasher.createRmd160()
                    HashAlgorithm.HASH160 -> BRCryptoHasher.createHash160()
                    HashAlgorithm.KECCAK256 -> BRCryptoHasher.createKeccak256()
                    HashAlgorithm.MD5 -> BRCryptoHasher.createMd5()
                }.orNull().run(::Hasher)
    }
}
