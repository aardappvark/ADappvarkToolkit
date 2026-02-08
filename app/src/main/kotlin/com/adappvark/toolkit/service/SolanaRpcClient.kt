package com.adappvark.toolkit.service

import com.adappvark.toolkit.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Solana RPC client for blockchain operations
 */
class SolanaRpcClient {

    private val rpcUrl = AppConfig.Payment.RPC_ENDPOINT
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Get recent blockhash
     */
    suspend fun getRecentBlockhash(): Result<Blockhash> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getLatestBlockhash")
                putJsonArray("params") {
                    addJsonObject {
                        put("commitment", "finalized")
                    }
                }
            }

            val response = makeRpcCall(requestBody.toString())

            val blockhashValue = response["result"]?.jsonObject?.get("value")?.jsonObject
                ?: return@withContext Result.failure(Exception("Invalid blockhash response"))

            val blockhashStr = blockhashValue["blockhash"]?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("No blockhash in response"))

            val lastValidBlockHeight = blockhashValue["lastValidBlockHeight"]?.jsonPrimitive?.long
                ?: 0L

            Result.success(
                Blockhash(
                    blockhash = blockhashStr,
                    lastValidBlockHeight = lastValidBlockHeight
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send transaction to network
     */
    suspend fun sendTransaction(
        serializedTransaction: String,
        encoding: String = "base64"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "sendTransaction")
                putJsonArray("params") {
                    add(serializedTransaction)
                    addJsonObject {
                        put("encoding", encoding)
                        put("skipPreflight", false)
                        put("preflightCommitment", "finalized")
                    }
                }
            }

            val response = makeRpcCall(requestBody.toString())

            if (response.containsKey("error")) {
                val errorMsg = response["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    ?: "Unknown error"
                return@withContext Result.failure(Exception("RPC Error: $errorMsg"))
            }

            val signature = response["result"]?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("No signature in response"))

            Result.success(signature)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Confirm transaction status
     */
    suspend fun confirmTransaction(
        signature: String,
        commitment: String = "finalized"
    ): Result<TransactionStatus> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getSignatureStatuses")
                putJsonArray("params") {
                    addJsonArray {
                        add(signature)
                    }
                    addJsonObject {
                        put("searchTransactionHistory", true)
                    }
                }
            }

            val response = makeRpcCall(requestBody.toString())

            val statuses = response["result"]?.jsonObject?.get("value")?.jsonArray
                ?: return@withContext Result.failure(Exception("No status array"))

            if (statuses.isEmpty()) {
                return@withContext Result.success(TransactionStatus.NotFound)
            }

            val firstStatus = statuses[0]
            if (firstStatus is JsonNull) {
                return@withContext Result.success(TransactionStatus.NotFound)
            }

            val status = firstStatus.jsonObject
            val confirmationStatus = status["confirmationStatus"]?.jsonPrimitive?.content
            val err = status["err"]

            when {
                err != null && err !is JsonNull -> Result.success(TransactionStatus.Failed)
                confirmationStatus == "finalized" -> Result.success(TransactionStatus.Finalized)
                confirmationStatus == "confirmed" -> Result.success(TransactionStatus.Confirmed)
                else -> Result.success(TransactionStatus.Processing)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get account balance in lamports
     */
    suspend fun getBalance(publicKey: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getBalance")
                putJsonArray("params") {
                    add(publicKey)
                    addJsonObject {
                        put("commitment", "finalized")
                    }
                }
            }

            val response = makeRpcCall(requestBody.toString())

            val balance = response["result"]?.jsonObject?.get("value")?.jsonPrimitive?.long
                ?: return@withContext Result.failure(Exception("No balance in response"))

            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get minimum balance for rent exemption
     */
    suspend fun getMinimumBalanceForRentExemption(dataLength: Int = 0): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getMinimumBalanceForRentExemption")
                putJsonArray("params") {
                    add(dataLength)
                }
            }

            val response = makeRpcCall(requestBody.toString())

            val minBalance = response["result"]?.jsonPrimitive?.long
                ?: return@withContext Result.failure(Exception("No result"))

            Result.success(minBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Make RPC call to Solana network
     */
    private suspend fun makeRpcCall(requestJson: String): JsonObject = withContext(Dispatchers.IO) {
        val url = URL(rpcUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Write request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson)
                writer.flush()
            }

            // Read response
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                throw Exception("HTTP error: $responseCode")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

            json.parseToJsonElement(response).jsonObject
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Blockhash response
 */
data class Blockhash(
    val blockhash: String,
    val lastValidBlockHeight: Long
)

/**
 * Transaction status
 */
sealed class TransactionStatus {
    data object NotFound : TransactionStatus()
    data object Processing : TransactionStatus()
    data object Confirmed : TransactionStatus()
    data object Finalized : TransactionStatus()
    data object Failed : TransactionStatus()
}
