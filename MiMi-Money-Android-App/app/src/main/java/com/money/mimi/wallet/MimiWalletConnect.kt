package com.money.mimi.wallet

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.money.mimi.BuildConfig
import com.money.mimi.R
import com.money.mimi.helpers.PreferenceManager
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.android.relay.ConnectionType
import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import org.json.JSONArray
import org.json.JSONObject
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.rlp.RlpEncoder
import org.web3j.rlp.RlpList
import org.web3j.rlp.RlpString
import org.web3j.rlp.RlpType
import org.web3j.utils.Numeric
import java.lang.ref.WeakReference
import java.io.ByteArrayOutputStream
import java.math.BigInteger

object MimiWalletConnect {
    private const val WALLETCONNECT_NAMESPACE = "eip155"
    private const val WALLETCONNECT_ICON_URL = "https://mimi.money/favicon.ico"
    private val supportedMethods = listOf(
        "eth_accounts",
        "eth_requestAccounts",
        "eth_chainId",
        "net_version",
        "personal_sign",
        "eth_sign",
        "eth_signTypedData",
        "eth_signTypedData_v3",
        "eth_signTypedData_v4",
        "eth_sendTransaction",
        "eth_signTransaction"
    )
    private val supportedEvents = listOf("accountsChanged", "chainChanged")
    @Volatile private var initialized = false
    private var activityRef: WeakReference<Activity>? = null

    @JvmStatic
    fun initialize(application: Application) {
        if (initialized) return
        val projectId = BuildConfig.WALLETCONNECT_PROJECT_ID.trim()
        if (projectId.isEmpty()) return

        val metadata = Core.Model.AppMetaData(
            application.getString(R.string.app_name),
            "MiMi Money wallet",
            "https://mimimoney.app",
            walletIconUrls(application),
            null,
            null,
            false,
            null
        )

        CoreClient.initialize(
            application,
            projectId,
            metadata,
            ConnectionType.AUTOMATIC,
            null,
            null,
            null,
            false
        ) { error -> toast(application, error.throwable.message ?: error.toString()) }

        WalletKit.initialize(
            Wallet.Params.Init(CoreClient),
            {
                initialized = true
                WalletKit.setWalletDelegate(delegate(application.applicationContext))
            },
            { error -> toast(application, error.throwable.message ?: error.toString()) }
        )
    }

    private fun walletIconUrls(context: Context): List<String> {
        val bundledIcon = bundledWalletIconDataUri(context)
        return if (bundledIcon.isNotEmpty()) {
            listOf(bundledIcon, WALLETCONNECT_ICON_URL)
        } else {
            listOf(WALLETCONNECT_ICON_URL)
        }
    }

    @SuppressLint("ResourceType")
    private fun bundledWalletIconDataUri(context: Context): String {
        return try {
            val output = ByteArrayOutputStream()
            context.resources.openRawResource(R.drawable.mimi_wallet_provider_icon_128).use { input ->
                val buffer = ByteArray(4096)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
            }
            "data:image/png;base64,${Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)}"
        } catch (_: Exception) {
            ""
        }
    }

    @JvmStatic
    fun pair(activity: Activity, uri: String) {
        activityRef = WeakReference(activity)
        initialize(activity.application)
        if (!initialized) {
            Toast.makeText(activity, R.string.wallet_dapp_walletconnect_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(activity, R.string.wallet_dapp_walletconnect_pairing, Toast.LENGTH_SHORT).show()
        WalletKit.pair(
            Wallet.Params.Pair(uri),
            {},
            { error ->
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.wallet_dapp_walletconnect_pairing_failed) + ": " + readableError(error), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun delegate(appContext: Context): WalletKit.WalletDelegate {
        return object : WalletKit.WalletDelegate {
            override val onSessionAuthenticate: (Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit = { _, _ -> }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal, verifyContext: Wallet.Model.VerifyContext) {
                val activity = activityRef?.get()
                if (activity == null || activity.isFinishing) {
                    rejectSession(sessionProposal)
                    return
                }
                activity.runOnUiThread {
                    val walletAddress = PreferenceManager.getWalletAddress(activity)?.trim().orEmpty()
                    if (walletAddress.isEmpty()) {
                        Toast.makeText(activity, R.string.wallet_no_wallet_generate_first, Toast.LENGTH_LONG).show()
                        rejectSession(sessionProposal)
                        return@runOnUiThread
                    }
                    val message = buildString {
                        append(sessionProposal.name.ifEmpty { sessionProposal.url.ifEmpty { "dApp" } })
                        append("\n\n")
                        if (sessionProposal.url.isNotEmpty()) append(sessionProposal.url).append("\n\n")
                        append("Wallet: ").append(walletAddress).append("\n")
                        append("Network: eip155:").append(WalletConfig.getChainId(activity))
                    }
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.wallet_dapp_walletconnect_approve_title)
                        .setMessage(message)
                        .setNegativeButton(android.R.string.cancel) { _, _ -> rejectSession(sessionProposal) }
                        .setPositiveButton(android.R.string.ok) { _, _ -> approveSession(activity, sessionProposal, walletAddress) }
                        .show()
                }
            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
                val activity = activityRef?.get()
                if (activity == null || activity.isFinishing) {
                    respondError(sessionRequest, 4001, "No active wallet screen")
                    return
                }
                activity.runOnUiThread {
                    val method = sessionRequest.request.method
                    val message = buildString {
                        append(sessionRequest.peerMetaData?.name?.ifEmpty { "dApp" } ?: "dApp").append("\n\n")
                        append(method).append("\n")
                        append(sessionRequest.request.params.take(500))
                    }
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.wallet_dapp_walletconnect_request_title)
                        .setMessage(message)
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            respondError(sessionRequest, 4001, activity.getString(R.string.wallet_dapp_walletconnect_rejected))
                        }
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            Thread { handleRequest(activity.applicationContext, sessionRequest) }.start()
                        }
                        .show()
                }
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {}
            override fun onSessionExtend(session: Wallet.Model.Session) {}
            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {}
            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {}
            override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {}
            override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {}
            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {}
            override fun onError(error: Wallet.Model.Error) {
                toast(appContext, readableError(error))
            }
        }
    }

    private fun approveSession(context: Context, proposal: Wallet.Model.SessionProposal, walletAddress: String) {
        val chain = "eip155:${WalletConfig.getChainId(context)}"
        val accounts = listOf("$chain:$walletAddress")
        val requestedMethods = collectRequested(proposal) { it.methods }.ifEmpty { supportedMethods }
        val requestedEvents = collectRequested(proposal) { it.events }.ifEmpty { supportedEvents }
        val namespace = Wallet.Model.Namespace.Session(
            listOf(chain),
            accounts,
            requestedMethods.filter { supportedMethods.contains(it) },
            requestedEvents.filter { supportedEvents.contains(it) }
        )
        val namespaces = mapOf(WALLETCONNECT_NAMESPACE to namespace)
        WalletKit.approveSession(
            Wallet.Params.SessionApprove(proposal.proposerPublicKey, namespaces, emptyMap(), emptyMap(), proposal.relayProtocol),
            { toast(context, context.getString(R.string.wallet_dapp_walletconnect_connected)) },
            { error -> toast(context, readableError(error)) }
        )
    }

    private fun collectRequested(
        proposal: Wallet.Model.SessionProposal,
        extractor: (Wallet.Model.Namespace.Proposal) -> List<String>
    ): List<String> {
        return (proposal.requiredNamespaces.values + proposal.optionalNamespaces.values)
            .flatMap(extractor)
            .distinct()
    }

    private fun rejectSession(proposal: Wallet.Model.SessionProposal) {
        WalletKit.rejectSession(Wallet.Params.SessionReject(proposal.proposerPublicKey, "User rejected"), {}, {})
    }

    private fun handleRequest(context: Context, request: Wallet.Model.SessionRequest) {
        try {
            val id = request.request.id
            val method = request.request.method
            val params = parseParams(request.request.params)
            val result = when (method) {
                "eth_accounts", "eth_requestAccounts" -> JSONArray().put(requireWalletAddress(context)).toString()
                "eth_chainId" -> "\"0x${WalletConfig.getChainId(context).toString(16)}\""
                "net_version" -> "\"${WalletConfig.getChainId(context)}\""
                "personal_sign" -> "\"${signPersonal(context, params)}\""
                "eth_sign" -> "\"${signEth(context, params)}\""
                "eth_signTypedData", "eth_signTypedData_v3", "eth_signTypedData_v4" -> "\"${signTypedData(context, params)}\""
                "eth_sendTransaction" -> "\"${sendTransaction(context, params)}\""
                "eth_signTransaction" -> "\"${signTransaction(context, params)}\""
                else -> throw IllegalArgumentException("Unsupported method: $method")
            }
            respondResult(request.topic, id, result)
        } catch (e: Exception) {
            respondError(request, -32603, e.message ?: "WalletConnect request failed")
        }
    }

    private fun parseParams(raw: String): JSONArray {
        if (raw.isBlank()) return JSONArray()
        val trimmed = raw.trim()
        return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONArray().put(JSONObject(trimmed))
    }

    private fun requireWalletAddress(context: Context): String {
        return PreferenceManager.getWalletAddress(context)?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException(context.getString(R.string.wallet_no_wallet_generate_first))
    }

    private fun loadCredentials(context: Context): Credentials {
        val mnemonic = PreferenceManager.getWalletMnemonic(context)
        val password = PreferenceManager.getWalletPassword(context)
        if (mnemonic.isNullOrEmpty() || password.isNullOrEmpty()) {
            throw IllegalStateException(context.getString(R.string.wallet_no_wallet_generate_first))
        }
        return org.web3j.crypto.WalletUtils.loadBip39Credentials(password, mnemonic)
    }

    private fun signPersonal(context: Context, params: JSONArray): String {
        val credentials = loadCredentials(context)
        val (address, message) = parseSignPayload(params)
        requireActiveSigner(credentials, address)
        val data = if (message.startsWith("0x")) Numeric.hexStringToByteArray(message) else message.toByteArray(Charsets.UTF_8)
        val prefix = "\u0019Ethereum Signed Message:\n${data.size}".toByteArray(Charsets.UTF_8)
        return signatureToHex(Sign.signMessage(prefix + data, credentials.ecKeyPair, false))
    }

    private fun signEth(context: Context, params: JSONArray): String {
        val credentials = loadCredentials(context)
        val (address, payload) = parseSignPayload(params)
        requireActiveSigner(credentials, address)
        val data = if (payload.startsWith("0x")) Numeric.hexStringToByteArray(payload) else payload.toByteArray(Charsets.UTF_8)
        return signatureToHex(Sign.signMessage(data, credentials.ecKeyPair, false))
    }

    private fun signTypedData(context: Context, params: JSONArray): String {
        val credentials = loadCredentials(context)
        val (address, payload) = parseTypedDataPayload(params)
        requireActiveSigner(credentials, address)
        val hash = Eip712Json.hash(payload)
        if (hash.domainChainId != null && hash.domainChainId.toLong() != WalletConfig.getChainId(context)) {
            throw IllegalArgumentException("Typed data chainId does not match the active network")
        }
        return signatureToHex(Sign.signMessage(hash.digest, credentials.ecKeyPair, false))
    }

    private fun parseSignPayload(params: JSONArray): Pair<String, String> {
        if (params.length() < 2) throw IllegalArgumentException("Signature request requires an address and payload")
        val first = params.optString(0)
        val second = params.optString(1)
        return when {
            isAddress(first) && !isAddress(second) -> Pair(normalizeAddress(first), second)
            isAddress(second) && !isAddress(first) -> Pair(normalizeAddress(second), first)
            isAddress(first) -> Pair(normalizeAddress(first), second)
            isAddress(second) -> Pair(normalizeAddress(second), first)
            else -> throw IllegalArgumentException("Signature request requires one wallet address")
        }
    }

    private fun parseTypedDataPayload(params: JSONArray): Pair<String, Any> {
        if (params.length() < 2) throw IllegalArgumentException("Typed data request requires an address and payload")
        val first = params.opt(0)
        val second = params.opt(1)
        val firstText = first?.toString().orEmpty()
        val secondText = second?.toString().orEmpty()
        return when {
            isAddress(firstText) && !isAddress(secondText) -> Pair(normalizeAddress(firstText), second)
            isAddress(secondText) && !isAddress(firstText) -> Pair(normalizeAddress(secondText), first)
            isAddress(firstText) -> Pair(normalizeAddress(firstText), second)
            isAddress(secondText) -> Pair(normalizeAddress(secondText), first)
            else -> throw IllegalArgumentException("Typed data request requires one wallet address")
        }
    }

    private fun requireActiveSigner(credentials: Credentials, address: String) {
        if (!normalizeAddress(credentials.address).equals(normalizeAddress(address), ignoreCase = true)) {
            throw IllegalArgumentException("Signature must use the active wallet address")
        }
    }

    private fun normalizeAddress(address: String): String {
        val trimmed = address.trim()
        return if (trimmed.startsWith("0x", ignoreCase = true)) trimmed else "0x$trimmed"
    }

    private fun isAddress(value: String): Boolean {
        return value.matches(Regex("^(0x)?[0-9a-fA-F]{40}$"))
    }

    private fun sendTransaction(context: Context, params: JSONArray): String {
        val signed = signTransaction(context, params)
        val response = Web3Provider.get(context).ethSendRawTransaction(signed).send()
        if (response.hasError()) throw IllegalStateException(response.error.message)
        return response.transactionHash
    }

    private fun signTransaction(context: Context, params: JSONArray): String {
        val tx = params.getJSONObject(0)
        val credentials = loadCredentials(context)
        val from = tx.optString("from", credentials.address)
        if (!from.equals(credentials.address, ignoreCase = true)) throw IllegalArgumentException("Transaction from must match active wallet")
        val to = if (tx.has("to") && !tx.isNull("to")) tx.optString("to") else null
        val value = parseQuantity(tx.optString("value", "0x0"))
        val data = tx.optString("data", "")
        val web3 = Web3Provider.get(context)
        val nonce = tx.optString("nonce", "").takeIf { it.isNotEmpty() }?.let(::parseQuantity)
            ?: web3.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.PENDING).send().transactionCount
        val gasPrice = tx.optString("gasPrice", "").takeIf { it.isNotEmpty() }?.let(::parseQuantity)
            ?: web3.ethGasPrice().send().gasPrice
        val gasLimit = tx.optString("gas", "").takeIf { it.isNotEmpty() }?.let(::parseQuantity)
            ?: web3.ethEstimateGas(Transaction.createFunctionCallTransaction(credentials.address, nonce, gasPrice, null, to, value, data)).send().amountUsed
        val rawTx = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, to, value, data)
        return Numeric.toHexString(TransactionEncoder.signMessage(rawTx, WalletConfig.getChainId(context), credentials))
    }

    private fun parseQuantity(value: String): BigInteger {
        val clean = value.ifBlank { "0x0" }
        return if (clean.startsWith("0x")) BigInteger(clean.removePrefix("0x").ifEmpty { "0" }, 16) else BigInteger(clean)
    }

    private fun signatureToHex(signature: Sign.SignatureData): String {
        val signed = ByteArray(65)
        System.arraycopy(signature.r, 0, signed, 0, 32)
        System.arraycopy(signature.s, 0, signed, 32, 32)
        signed[64] = signature.v[0]
        return Numeric.toHexString(signed)
    }

    private fun respondResult(topic: String, id: Long, result: String) {
        WalletKit.respondSessionRequest(
            Wallet.Params.SessionRequestResponse(topic, Wallet.Model.JsonRpcResponse.JsonRpcResult(id, result)),
            {},
            {}
        )
    }

    private fun respondError(request: Wallet.Model.SessionRequest, code: Int, message: String) {
        WalletKit.respondSessionRequest(
            Wallet.Params.SessionRequestResponse(request.topic, Wallet.Model.JsonRpcResponse.JsonRpcError(request.request.id, code, message)),
            {},
            {}
        )
    }

    private fun readableError(error: Wallet.Model.Error): String {
        return error.throwable.message ?: error.toString()
    }

    private fun toast(context: Context, message: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
