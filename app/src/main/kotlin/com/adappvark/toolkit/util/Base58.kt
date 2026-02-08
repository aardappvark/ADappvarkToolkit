package com.adappvark.toolkit.util

import java.math.BigInteger

/**
 * Base58 encoding/decoding utility for Solana addresses and signatures
 */
object Base58 {
    
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128) { -1 }
    
    init {
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].code] = i
        }
    }
    
    /**
     * Encode bytes to Base58 string
     */
    fun encode(input: ByteArray): String {
        if (input.isEmpty()) {
            return ""
        }
        
        // Count leading zeros
        var leadingZeros = 0
        while (leadingZeros < input.size && input[leadingZeros] == 0.toByte()) {
            leadingZeros++
        }
        
        // Convert to base58
        var num = BigInteger(1, input)
        val base = BigInteger.valueOf(58)
        val sb = StringBuilder()
        
        while (num > BigInteger.ZERO) {
            val remainder = num.mod(base).toInt()
            sb.append(ALPHABET[remainder])
            num = num.divide(base)
        }
        
        // Add leading zeros
        repeat(leadingZeros) {
            sb.append(ALPHABET[0])
        }
        
        return sb.reverse().toString()
    }
    
    /**
     * Decode Base58 string to bytes
     */
    fun decode(input: String): ByteArray {
        if (input.isEmpty()) {
            return ByteArray(0)
        }
        
        // Count leading zeros
        var leadingZeros = 0
        while (leadingZeros < input.length && input[leadingZeros] == ALPHABET[0]) {
            leadingZeros++
        }
        
        // Convert from base58
        var num = BigInteger.ZERO
        val base = BigInteger.valueOf(58)
        
        for (char in input) {
            val digit = INDEXES[char.code]
            if (digit < 0) {
                throw IllegalArgumentException("Invalid Base58 character: $char")
            }
            num = num.multiply(base).add(BigInteger.valueOf(digit.toLong()))
        }
        
        val bytes = num.toByteArray()
        
        // Remove leading zero from BigInteger's two's complement representation
        val stripSignByte = bytes.size > 1 && bytes[0] == 0.toByte() && bytes[1] < 0
        val leadingZeroBytes = leadingZeros
        
        val result = ByteArray(leadingZeroBytes + bytes.size - if (stripSignByte) 1 else 0)
        System.arraycopy(bytes, if (stripSignByte) 1 else 0, result, leadingZeroBytes, result.size - leadingZeroBytes)
        
        return result
    }
    
    /**
     * Validate if string is valid Base58
     */
    fun isValid(input: String): Boolean {
        return try {
            decode(input)
            true
        } catch (e: Exception) {
            false
        }
    }
}
