package com.bharatconnect.app.core.network

object NetworkErrorSanitizer {
    /**
     * Sanitizes raw exceptions, HTTP error responses, and internal stack trace messages
     * into clean, production-grade, customer-friendly messages with zero internal URL or tool leakages.
     */
    fun sanitize(error: Throwable?): String {
        if (error == null) return "An unexpected error occurred. Please try again."
        val rawMessage = error.message.orEmpty()
        val errorClassName = error::class.java.simpleName

        return when {
            // Timeout errors
            rawMessage.contains("timeout", ignoreCase = true) ||
            rawMessage.contains("timed out", ignoreCase = true) ||
            errorClassName.contains("Timeout", ignoreCase = true) -> {
                "Connection timed out. Please check your internet connection and try again."
            }

            // Connection / Network / DNS errors
            rawMessage.contains("UnknownHostException", ignoreCase = true) ||
            rawMessage.contains("ConnectException", ignoreCase = true) ||
            rawMessage.contains("No address associated with hostname", ignoreCase = true) ||
            rawMessage.contains("Failed to connect", ignoreCase = true) ||
            rawMessage.contains("Network is unreachable", ignoreCase = true) -> {
                "Unable to connect to the network. Please check your internet connection."
            }

            // OTP / Verification Code errors
            rawMessage.contains("Token has expired", ignoreCase = true) ||
            rawMessage.contains("Token is invalid", ignoreCase = true) ||
            rawMessage.contains("otp_expired", ignoreCase = true) ||
            rawMessage.contains("invalid token", ignoreCase = true) ||
            rawMessage.contains("token not found", ignoreCase = true) -> {
                "The 6-digit verification code is invalid or has expired. Please check and try again or click Resend."
            }

            rawMessage.contains("Email change request not found", ignoreCase = true) ||
            rawMessage.contains("signup request not found", ignoreCase = true) -> {
                "No pending verification request found. Please request a new code or try signing in."
            }

            rawMessage.contains("security purposes", ignoreCase = true) -> {
                "For security purposes, please wait 60 seconds before requesting another email."
            }

            // User already registered
            rawMessage.contains("already registered", ignoreCase = true) ||
            rawMessage.contains("already exists", ignoreCase = true) ||
            rawMessage.contains("duplicate key", ignoreCase = true) ||
            rawMessage.contains("23505") -> {
                "An account with this email, username, or phone number already exists."
            }

            // Invalid credentials
            rawMessage.contains("Invalid login credentials", ignoreCase = true) ||
            rawMessage.contains("invalid_grant", ignoreCase = true) ||
            rawMessage.contains("Invalid credentials", ignoreCase = true) -> {
                "Incorrect email, username, mobile number, or password."
            }

            // Password length / format
            rawMessage.contains("Password should be at least", ignoreCase = true) ||
            rawMessage.contains("Password must be at least", ignoreCase = true) -> {
                "Password must be at least 6 characters."
            }

            // Rate limits
            rawMessage.contains("rate limit", ignoreCase = true) ||
            rawMessage.contains("429", ignoreCase = true) ||
            rawMessage.contains("too many requests", ignoreCase = true) ||
            rawMessage.contains("over_email_send_rate_limit", ignoreCase = true) -> {
                "Email send limit reached (Supabase limits signup emails to 3-4 per hour on free tier). Please check your spam folder or wait a few minutes."
            }

            // Email not confirmed
            rawMessage.contains("Email not confirmed", ignoreCase = true) -> {
                "Please verify your email address with the 6-digit code to sign in."
            }

            // Invalid email format
            rawMessage.contains("Unable to validate email", ignoreCase = true) ||
            rawMessage.contains("invalid email", ignoreCase = true) -> {
                "Please enter a valid email address."
            }

            // Default fallback if message leaks internal URLs or tool names
            rawMessage.contains("supabase", ignoreCase = true) ||
            rawMessage.contains("http://", ignoreCase = true) ||
            rawMessage.contains("https://", ignoreCase = true) ||
            rawMessage.contains("postgrest", ignoreCase = true) ||
            rawMessage.contains("gotrue", ignoreCase = true) ||
            rawMessage.contains("cloudinary", ignoreCase = true) ||
            rawMessage.contains("sql", ignoreCase = true) ||
            rawMessage.contains("postgres", ignoreCase = true) ||
            rawMessage.contains("cio", ignoreCase = true) ||
            rawMessage.contains("ktor", ignoreCase = true) -> {
                "Unable to complete authentication request. Please check your details and try again."
            }

            // Clean custom messages without internal technical keywords
            rawMessage.isNotBlank() && rawMessage.length < 120 -> rawMessage

            else -> "Unable to complete request. Please try again."
        }
    }
}
