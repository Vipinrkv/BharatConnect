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
                "Too many attempts. Please wait a few moments and try again."
            }

            // Email not confirmed
            rawMessage.contains("Email not confirmed", ignoreCase = true) -> {
                "Please verify your email address to sign in."
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
                "Unable to complete request. Please check your internet connection and try again."
            }

            // Clean custom messages without internal technical keywords
            rawMessage.isNotBlank() && rawMessage.length < 120 -> rawMessage

            else -> "Unable to complete request. Please check your internet connection and try again."
        }
    }
}
