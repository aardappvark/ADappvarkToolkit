package com.adappvark.toolkit.service

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Solana transaction builder
 * Constructs raw Solana transactions for SOL and SPL token payments
 */
class SolanaTransactionBuilder {

    companion object {
        // Solana system program ID (all zeros)
        private val SYSTEM_PROGRAM_ID = ByteArray(32)

        // SPL Token Program ID: TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA
        val TOKEN_PROGRAM_ID = PaymentService.decodeBase58("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")

        // Associated Token Program ID: ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL
        val ASSOCIATED_TOKEN_PROGRAM_ID = PaymentService.decodeBase58("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")

        // Sysvar Rent: SysvarRent111111111111111111111111111111111
        val SYSVAR_RENT_ID = PaymentService.decodeBase58("SysvarRent111111111111111111111111111111111")

        // Transfer instruction discriminator (2 for SystemProgram::Transfer)
        private const val TRANSFER_INSTRUCTION: Byte = 2

        // TransferChecked instruction discriminator for SPL Token Program
        private const val TRANSFER_CHECKED_INSTRUCTION: Byte = 12

        /**
         * Derive the Associated Token Address (ATA) for a wallet and mint.
         * PDA = findProgramAddress([wallet, TOKEN_PROGRAM_ID, mint], ASSOCIATED_TOKEN_PROGRAM_ID)
         */
        fun deriveAssociatedTokenAddress(wallet: ByteArray, mint: ByteArray): ByteArray {
            require(wallet.size == 32) { "Wallet must be 32 bytes" }
            require(mint.size == 32) { "Mint must be 32 bytes" }

            // Try bump seeds from 255 down to 0
            for (bump in 255 downTo 0) {
                val hash = MessageDigest.getInstance("SHA-256")
                hash.update(wallet)
                hash.update(TOKEN_PROGRAM_ID)
                hash.update(mint)
                hash.update(byteArrayOf(bump.toByte()))
                hash.update(ASSOCIATED_TOKEN_PROGRAM_ID)
                hash.update("ProgramDerivedAddress".toByteArray())

                val candidate = hash.digest()

                // A valid PDA must NOT be on the ed25519 curve.
                // We check this by attempting to decompress the point.
                // If it fails, the candidate is a valid PDA.
                if (!isOnCurve(candidate)) {
                    return candidate
                }
            }
            throw Exception("Could not derive ATA — no valid bump seed found")
        }

        /**
         * Check if a 32-byte key is on the ed25519 curve.
         * Uses the property that a valid ed25519 point y-coordinate satisfies:
         * x^2 = (y^2 - 1) / (d * y^2 + 1) mod p, where p = 2^255 - 19
         * If a square root exists for x^2, the point is on the curve.
         */
        private fun isOnCurve(point: ByteArray): Boolean {
            if (point.size != 32) return false

            // ed25519 prime: p = 2^255 - 19
            val p = java.math.BigInteger.TWO.pow(255).subtract(java.math.BigInteger.valueOf(19))

            // ed25519 constant d = -121665/121666 mod p
            val d = java.math.BigInteger("-121665")
                .multiply(java.math.BigInteger("121666").modInverse(p))
                .mod(p)

            // Decode y from little-endian bytes (clear top bit which is sign of x)
            val yCopy = point.copyOf()
            yCopy[31] = (yCopy[31].toInt() and 0x7F).toByte()
            val yBytes = yCopy.reversedArray() // Convert LE to BE for BigInteger
            val y = java.math.BigInteger(1, yBytes).mod(p)

            // Check if y >= p (invalid)
            if (y >= p) return false

            // Compute x^2 = (y^2 - 1) / (d * y^2 + 1) mod p
            val y2 = y.multiply(y).mod(p)
            val numerator = y2.subtract(java.math.BigInteger.ONE).mod(p)
            val denominator = d.multiply(y2).add(java.math.BigInteger.ONE).mod(p)

            val denominatorInv = denominator.modInverse(p)
            val x2 = numerator.multiply(denominatorInv).mod(p)

            if (x2 == java.math.BigInteger.ZERO) return true

            // Check if x^2 is a quadratic residue mod p using Euler's criterion
            // x^2 is a QR if x^2^((p-1)/2) == 1 mod p
            val exp = p.subtract(java.math.BigInteger.ONE).divide(java.math.BigInteger.TWO)
            val result = x2.modPow(exp, p)
            return result == java.math.BigInteger.ONE
        }
    }
    
    /**
     * Wrap a Solana message into a full serialized transaction.
     * MWA signAndSendTransactions expects the full transaction format:
     *   [compact-u16 num_signatures] [64-byte empty signature per signer] [message bytes]
     *
     * @param messageBytes The serialized Solana message
     * @param numSigners Number of required signers (from message header byte 0)
     * @return Full serialized transaction with empty signature slots
     */
    private fun wrapMessageAsTransaction(messageBytes: ByteArray, numSigners: Int = 1): ByteArray {
        val buffer = ByteBuffer.allocate(1 + numSigners * 64 + messageBytes.size)
        // Compact-u16 for numSigners (always < 128, so single byte)
        buffer.put(numSigners.toByte())
        // Empty signature slots (64 zero bytes each) — wallet fills these in
        for (i in 0 until numSigners) {
            buffer.put(ByteArray(64))
        }
        // Message bytes
        buffer.put(messageBytes)
        return buffer.array()
    }

    /**
     * Build a SOL transfer transaction
     * @param fromPubkey Sender's public key (32 bytes)
     * @param toPubkey Recipient's public key (32 bytes)
     * @param lamports Amount to transfer in lamports
     * @param recentBlockhash Recent blockhash (32 bytes)
     * @return Serialized transaction ready for signing
     */
    fun buildTransferTransaction(
        fromPubkey: ByteArray,
        toPubkey: ByteArray,
        lamports: Long,
        recentBlockhash: ByteArray
    ): ByteArray {
        require(fromPubkey.size == 32) { "From pubkey must be 32 bytes" }
        require(toPubkey.size == 32) { "To pubkey must be 32 bytes" }
        require(recentBlockhash.size == 32) { "Blockhash must be 32 bytes" }

        // Create transfer instruction data
        val instructionData = createTransferInstructionData(lamports)

        // Build message
        val message = buildMessage(
            fromPubkey = fromPubkey,
            toPubkey = toPubkey,
            recentBlockhash = recentBlockhash,
            instructionData = instructionData
        )

        // Wrap message as full transaction (1 signer = fee payer)
        return wrapMessageAsTransaction(message, numSigners = 1)
    }
    
    /**
     * Build an SPL token TransferChecked transaction.
     * Includes CreateAssociatedTokenAccount for the recipient if their ATA doesn't exist.
     *
     * @param fromWallet Sender's wallet public key (32 bytes)
     * @param toWallet Recipient's wallet public key (32 bytes)
     * @param mint Token mint public key (32 bytes)
     * @param amount Token amount in smallest units (e.g. for 1 token with 6 decimals = 1_000_000)
     * @param decimals Token decimals
     * @param recentBlockhash Recent blockhash (32 bytes)
     * @param createRecipientAta Whether to include CreateAssociatedTokenAccount for recipient
     */
    fun buildSplTransferCheckedTransaction(
        fromWallet: ByteArray,
        toWallet: ByteArray,
        mint: ByteArray,
        amount: Long,
        decimals: Int,
        recentBlockhash: ByteArray,
        createRecipientAta: Boolean = true
    ): ByteArray {
        require(fromWallet.size == 32) { "From wallet must be 32 bytes" }
        require(toWallet.size == 32) { "To wallet must be 32 bytes" }
        require(mint.size == 32) { "Mint must be 32 bytes" }
        require(recentBlockhash.size == 32) { "Blockhash must be 32 bytes" }

        val senderAta = deriveAssociatedTokenAddress(fromWallet, mint)
        val recipientAta = deriveAssociatedTokenAddress(toWallet, mint)

        val message = if (createRecipientAta) {
            buildCreateAtaAndTransferMessage(
                feePayer = fromWallet,
                senderAta = senderAta,
                recipientAta = recipientAta,
                recipientWallet = toWallet,
                mint = mint,
                amount = amount,
                decimals = decimals,
                recentBlockhash = recentBlockhash
            )
        } else {
            buildTransferCheckedMessage(
                feePayer = fromWallet,
                senderAta = senderAta,
                recipientAta = recipientAta,
                mint = mint,
                amount = amount,
                decimals = decimals,
                recentBlockhash = recentBlockhash
            )
        }

        // Wrap message as full transaction (1 signer = fee payer)
        return wrapMessageAsTransaction(message, numSigners = 1)
    }

    /**
     * Build message with CreateAssociatedTokenAccount + TransferChecked.
     *
     * Account ordering (sorted by signer/writable status per Solana convention):
     * 0: feePayer (signer, writable)
     * 1: senderAta (writable)
     * 2: recipientAta (writable)
     * 3: mint (readonly)
     * 4: recipientWallet (readonly)
     * 5: systemProgram (readonly)
     * 6: tokenProgram (readonly)
     * 7: associatedTokenProgram (readonly)
     */
    private fun buildCreateAtaAndTransferMessage(
        feePayer: ByteArray,
        senderAta: ByteArray,
        recipientAta: ByteArray,
        recipientWallet: ByteArray,
        mint: ByteArray,
        amount: Long,
        decimals: Int,
        recentBlockhash: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(2048)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // Header
        buffer.put(1.toByte())  // 1 required signature (feePayer)
        buffer.put(0.toByte())  // 0 readonly signed accounts
        buffer.put(5.toByte())  // 5 readonly unsigned accounts (mint, recipientWallet, system, token, ata program)

        // Account keys (8 total)
        writeCompactU16(buffer, 8)
        buffer.put(feePayer)              // 0: signer, writable
        buffer.put(senderAta)             // 1: writable
        buffer.put(recipientAta)          // 2: writable
        buffer.put(mint)                  // 3: readonly
        buffer.put(recipientWallet)       // 4: readonly
        buffer.put(SYSTEM_PROGRAM_ID)     // 5: readonly
        buffer.put(TOKEN_PROGRAM_ID)      // 6: readonly
        buffer.put(ASSOCIATED_TOKEN_PROGRAM_ID) // 7: readonly

        // Recent blockhash
        buffer.put(recentBlockhash)

        // 2 instructions
        writeCompactU16(buffer, 2)

        // --- Instruction 1: CreateAssociatedTokenAccount ---
        // Program: Associated Token Program (index 7)
        buffer.put(7.toByte())
        // Accounts: [payer(0), ata(2), wallet_owner(4), mint(3), system(5), token(6)]
        writeCompactU16(buffer, 6)
        buffer.put(0.toByte())   // payer (feePayer)
        buffer.put(2.toByte())   // associated token account to create (recipientAta)
        buffer.put(4.toByte())   // wallet owner (recipientWallet)
        buffer.put(3.toByte())   // mint
        buffer.put(5.toByte())   // system program
        buffer.put(6.toByte())   // token program
        // Instruction data: empty (CreateAssociatedTokenAccount has no data)
        writeCompactU16(buffer, 0)

        // --- Instruction 2: TransferChecked ---
        // Program: Token Program (index 6)
        buffer.put(6.toByte())
        // Accounts: [source(1), mint(3), destination(2), owner/signer(0)]
        writeCompactU16(buffer, 4)
        buffer.put(1.toByte())   // source (senderAta)
        buffer.put(3.toByte())   // mint
        buffer.put(2.toByte())   // destination (recipientAta)
        buffer.put(0.toByte())   // owner/authority (feePayer = sender wallet)
        // Instruction data: discriminator(1) + amount(8) + decimals(1) = 10 bytes
        val transferData = createTransferCheckedInstructionData(amount, decimals)
        writeCompactU16(buffer, transferData.size)
        buffer.put(transferData)

        val messageBytes = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(messageBytes)
        return messageBytes
    }

    /**
     * Build message with just TransferChecked (when recipient ATA already exists).
     *
     * Account ordering:
     * 0: feePayer/owner (signer, writable)
     * 1: senderAta (writable)
     * 2: recipientAta (writable)
     * 3: mint (readonly)
     * 4: tokenProgram (readonly)
     */
    private fun buildTransferCheckedMessage(
        feePayer: ByteArray,
        senderAta: ByteArray,
        recipientAta: ByteArray,
        mint: ByteArray,
        amount: Long,
        decimals: Int,
        recentBlockhash: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(1024)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // Header
        buffer.put(1.toByte())  // 1 required signature
        buffer.put(0.toByte())  // 0 readonly signed
        buffer.put(2.toByte())  // 2 readonly unsigned (mint, tokenProgram)

        // Account keys (5 total)
        writeCompactU16(buffer, 5)
        buffer.put(feePayer)          // 0: signer, writable
        buffer.put(senderAta)         // 1: writable
        buffer.put(recipientAta)      // 2: writable
        buffer.put(mint)              // 3: readonly
        buffer.put(TOKEN_PROGRAM_ID)  // 4: readonly

        // Recent blockhash
        buffer.put(recentBlockhash)

        // 1 instruction
        writeCompactU16(buffer, 1)

        // TransferChecked
        buffer.put(4.toByte())  // Program: Token Program (index 4)
        // Accounts: [source(1), mint(3), destination(2), owner(0)]
        writeCompactU16(buffer, 4)
        buffer.put(1.toByte())  // source (senderAta)
        buffer.put(3.toByte())  // mint
        buffer.put(2.toByte())  // destination (recipientAta)
        buffer.put(0.toByte())  // owner/authority (feePayer)
        // Instruction data
        val transferData = createTransferCheckedInstructionData(amount, decimals)
        writeCompactU16(buffer, transferData.size)
        buffer.put(transferData)

        val messageBytes = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(messageBytes)
        return messageBytes
    }

    /**
     * Create instruction data for SPL Token TransferChecked
     * Layout: [discriminator: u8, amount: u64 LE, decimals: u8]
     */
    private fun createTransferCheckedInstructionData(amount: Long, decimals: Int): ByteArray {
        val buffer = ByteBuffer.allocate(10)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(TRANSFER_CHECKED_INSTRUCTION)  // 1 byte discriminator
        buffer.putLong(amount)                      // 8 bytes amount
        buffer.put(decimals.toByte())               // 1 byte decimals
        return buffer.array()
    }

    /**
     * Create instruction data for SystemProgram::Transfer
     */
    private fun createTransferInstructionData(lamports: Long): ByteArray {
        val buffer = ByteBuffer.allocate(12)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Instruction discriminator (4 bytes for transfer = 2)
        buffer.putInt(TRANSFER_INSTRUCTION.toInt())
        
        // Amount in lamports (8 bytes, little-endian)
        buffer.putLong(lamports)
        
        return buffer.array()
    }
    
    /**
     * Build Solana transaction message
     */
    private fun buildMessage(
        fromPubkey: ByteArray,
        toPubkey: ByteArray,
        recentBlockhash: ByteArray,
        instructionData: ByteArray
    ): ByteArray {
        val buffer = ByteBuffer.allocate(1024)  // Generous size
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Header
        // - 1 byte: number of required signatures (1 for simple transfer)
        buffer.put(1.toByte())
        
        // - 1 byte: number of readonly signed accounts (0)
        buffer.put(0.toByte())
        
        // - 1 byte: number of readonly unsigned accounts (1 for system program)
        buffer.put(1.toByte())
        
        // Account keys
        // - Compact array length (3 accounts: from, to, system_program)
        writeCompactU16(buffer, 3)
        
        // - Account 0: From (fee payer, writable, signer)
        buffer.put(fromPubkey)
        
        // - Account 1: To (writable)
        buffer.put(toPubkey)
        
        // - Account 2: System program (readonly)
        buffer.put(SYSTEM_PROGRAM_ID)
        
        // Recent blockhash
        buffer.put(recentBlockhash)
        
        // Instructions
        // - Compact array length (1 instruction)
        writeCompactU16(buffer, 1)
        
        // Instruction
        // - Program ID index (2 = system program)
        buffer.put(2.toByte())
        
        // - Accounts compact array (2 accounts: from=0, to=1)
        writeCompactU16(buffer, 2)
        buffer.put(0.toByte())  // From account index
        buffer.put(1.toByte())  // To account index
        
        // - Instruction data
        writeCompactU16(buffer, instructionData.size)
        buffer.put(instructionData)
        
        // Get actual bytes
        val messageBytes = ByteArray(buffer.position())
        buffer.rewind()
        buffer.get(messageBytes)
        
        return messageBytes
    }
    
    /**
     * Write compact-u16 encoding (Solana's variable-length encoding)
     */
    private fun writeCompactU16(buffer: ByteBuffer, value: Int) {
        when {
            value < 128 -> {
                buffer.put(value.toByte())
            }
            value < 16384 -> {
                buffer.put(((value and 0x7F) or 0x80).toByte())
                buffer.put((value shr 7).toByte())
            }
            else -> {
                buffer.put(((value and 0x7F) or 0x80).toByte())
                buffer.put((((value shr 7) and 0x7F) or 0x80).toByte())
                buffer.put((value shr 14).toByte())
            }
        }
    }
}
