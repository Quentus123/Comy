package models.users

import com.beust.klaxon.Json

data class User(val id: String, val password: String, var refreshKey: String? = null) {
    companion object {
        var fakeDB: MutableList<User> = mutableListOf()
    }
}