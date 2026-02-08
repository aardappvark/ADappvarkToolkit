package com.adappvark.toolkit.service

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Solana transaction builder
 * Constructs raw Solana transactions for payment
 */
class SolanaTransactionBuilder {
    
    companion object {
        // Solana system program ID
        private val SYSTEM_PROGRAM_ID = byteArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )
        
        // Transfer instruction discriminator (2 for SystemProgram::Transfer)
        private const val TRANSFER_INSTRUCTION: Byte = 2
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
        
        return message
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
