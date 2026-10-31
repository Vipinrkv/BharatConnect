package com.bharatconnect.app.core.payments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * =========================================================================================
 * BHARATCONNECT 1-TAP BHARAT UPI PAYMENT ENGINE (INDIA SUPERPOWER)
 * =========================================================================================
 *
 * Provides instant National Payments Corporation of India (NPCI) UPI integration:
 * 1. 1-Tap deep link generation for all Indian UPI apps (Google Pay, PhonePe, Paytm, BHIM, CRED).
 * 2. In-Chat interactive payment request card formatting and parsing.
 * 3. Direct app chooser dispatch without complex bank-account linking friction.
 */
data class UpiPaymentData(
    val payeeVpa: String,
    val payeeName: String,
    val amount: String,
    val transactionNote: String = "BharatConnect Payment",
    val currency: String = "INR"
)

object UpiPaymentManager {

    private const val UPI_PREFIX = "UPI_REQ::"

    /**
     * Builds standard NPCI-compliant upi://pay URI string.
     */
    fun buildUpiUriString(data: UpiPaymentData): String {
        val cleanAmount = data.amount.replace("₹", "").trim()
        val params = mutableListOf<String>()
        params.add("pa=${java.net.URLEncoder.encode(data.payeeVpa.trim(), "UTF-8")}")
        params.add("pn=${java.net.URLEncoder.encode(data.payeeName.trim(), "UTF-8")}")
        params.add("cu=${data.currency}")
        if (cleanAmount.isNotBlank() && cleanAmount != "0") {
            params.add("am=$cleanAmount")
        }
        if (data.transactionNote.isNotBlank()) {
            params.add("tn=${java.net.URLEncoder.encode(data.transactionNote.trim(), "UTF-8")}")
        }
        return "upi://pay?" + params.joinToString("&")
    }

    /**
     * Builds standard NPCI-compliant upi://pay URI.
     */
    fun buildUpiUri(data: UpiPaymentData): Uri {
        return Uri.parse(buildUpiUriString(data))
    }

    /**
     * Launches the native Android UPI chooser.
     */
    fun launchUpiPayment(context: Context, data: UpiPaymentData): Boolean {
        val uri = buildUpiUri(data)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            val chooser = Intent.createChooser(intent, "Pay ₹${data.amount} via UPI (PhonePe, GPay, Paytm, BHIM)")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI app (PhonePe/GPay/Paytm) found on this device", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Encodes a payment request into a structured message payload.
     */
    fun encodePaymentMessage(data: UpiPaymentData): String {
        val cleanAmount = data.amount.replace("₹", "").trim()
        val pa = java.net.URLEncoder.encode(data.payeeVpa.trim(), "UTF-8")
        val pn = java.net.URLEncoder.encode(data.payeeName.trim(), "UTF-8")
        val am = java.net.URLEncoder.encode(cleanAmount, "UTF-8")
        val tn = java.net.URLEncoder.encode(data.transactionNote.trim(), "UTF-8")
        return "${UPI_PREFIX}pa=$pa&pn=$pn&am=$am&tn=$tn"
    }

    /**
     * Parses a chat message to check if it represents a 1-tap UPI payment request.
     */
    fun parsePaymentMessage(content: String): UpiPaymentData? {
        if (!content.startsWith(UPI_PREFIX)) return null
        val payload = content.removePrefix(UPI_PREFIX)
        val params = payload.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0].trim() to java.net.URLDecoder.decode(parts[1].trim(), "UTF-8")
            } else "" to ""
        }

        val pa = params["pa"] ?: return null
        val pn = params["pn"] ?: "BharatConnect User"
        val am = params["am"] ?: "0"
        val tn = params["tn"] ?: "BharatConnect Transfer"

        if (pa.isBlank()) return null
        return UpiPaymentData(
            payeeVpa = pa,
            payeeName = pn,
            amount = am,
            transactionNote = tn
        )
    }

    /**
     * Basic validation for Indian UPI Virtual Payment Address (VPA).
     */
    fun isValidVpa(vpa: String): Boolean {
        val trimmed = vpa.trim()
        if (!trimmed.contains("@") || trimmed.contains(" ")) return false
        val parts = trimmed.split("@")
        return parts.size == 2 && parts[0].isNotBlank() && parts[1].length >= 2
    }
}
