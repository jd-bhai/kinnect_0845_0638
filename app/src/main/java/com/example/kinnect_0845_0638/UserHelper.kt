package com.example.kinnect_0845_0638

import android.content.Context

object UserHelper {

    fun getUserInitials(firstName: String, lastName: String): String {
        return if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
            "${firstName[0]}${lastName[0]}".uppercase()
        } else if (firstName.isNotEmpty()) {
            firstName[0].toString().uppercase()
        } else {
            "JW"
        }
    }

    fun fetchUserData(context: Context, uid: String): UserData? {
        return DatabaseHelper.getInstance(context).getUserByUid(uid)
    }

    fun listenToUserData(context: Context, uid: String, onDataChange: (UserData) -> Unit) {
        val user = fetchUserData(context, uid)
        if (user != null) {
            onDataChange(user)
        }
    }
}