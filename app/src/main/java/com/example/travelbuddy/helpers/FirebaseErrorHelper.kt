package com.example.travelbuddy.helpers

import com.example.travelbuddy.R
import com.google.firebase.auth.FirebaseAuthException

object FirebaseErrorHelper {
    private fun getCommonError(errorCode: String?, context: android.content.Context): String? {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.error_invalid_email)
            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.error_user_not_found)
            "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.error_network)
            else -> null
        }
    }
    
    fun getLoginErrorMessage(errorCode: String?, context: android.content.Context): String {
        return getCommonError(errorCode, context) ?: when (errorCode) {
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> context.getString(R.string.error_wrong_password)
            "ERROR_USER_DISABLED" -> context.getString(R.string.error_user_disabled)
            "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.error_too_many_requests)
            else -> context.getString(R.string.error_login_failed)
        }
    }
    
    fun getRegistrationErrorMessage(errorCode: String?, context: android.content.Context): String {
        return getCommonError(errorCode, context) ?: when (errorCode) {
            "ERROR_WEAK_PASSWORD" -> context.getString(R.string.error_weak_password)
            "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.error_email_already_in_use)
            else -> context.getString(R.string.error_registration_failed)
        }
    }
    
    fun getPasswordResetErrorMessage(errorCode: String?, context: android.content.Context, defaultMessage: String): String {
        return getCommonError(errorCode, context) ?: defaultMessage
    }
    
    fun getErrorCode(exception: Exception?): String? = (exception as? FirebaseAuthException)?.errorCode
}

