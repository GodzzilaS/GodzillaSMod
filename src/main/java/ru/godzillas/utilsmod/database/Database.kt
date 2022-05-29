package ru.godzillas.utilsmod.database

import dev.xdark.clientapi.ClientApi
import ru.godzillas.utilsmod.clientNick
import com.google.gson.Gson
import ru.godzillas.utilsmod.database.models.User
import ru.godzillas.utilsmod.utils.PlayerUtils
import java.net.URL
import java.util.*

object Database {

    private const val URL_NAME = "https://cristalix.i-e.space"
    private const val TOKEN = "MWOEMoI1NF)(2r32rIU\$WFJNiNF2=3rUWEJFno)JIWFNEH9832r"
    private var clientUuid: UUID? = null

    private fun updateClientUuid(api: ClientApi) {
        if (clientUuid == null) {
            while (clientUuid == null) {
                clientUuid = PlayerUtils.getClientUuid(api)
            }
            println("[GodzillaSMod] Successfully updated PlayerUUID")
        }
    }

    fun createUser(api: ClientApi): User {
        updateClientUuid(api)
        val user: User = Gson().fromJson(URL("$URL_NAME/create-user\$token=$TOKEN\$uuid=$clientUuid\$name=$clientNick").readText(), User::class.java)
        println("[GodzillaSMod] Successfully create and get user data: $user")
        return user
    }

    fun updateUser(api: ClientApi, chatColor: String): User {
        updateClientUuid(api)
        val user: User = Gson().fromJson(URL("$URL_NAME/update-user\$token=$TOKEN\$uuid=$clientUuid\$name=$clientNick\$chat_color=$chatColor").readText(), User::class.java)
        println("[GodzillaSMod] Successfully update user data: $user")
        return user
    }

    fun getUser(api: ClientApi): User {
        updateClientUuid(api)
        val user: User = Gson().fromJson(URL("$URL_NAME/get-user\$token=$TOKEN\$uuid=$clientUuid\$name=$clientNick").readText(), User::class.java)
        println("[GodzillaSMod] Successfully get user data: $user")
        return user
    }

    fun getUserParam(api: ClientApi, param: String): String {
        val user = getUser(api)
        if (param == "name") { return user.name }
        if (param == "color") { return user.chat_color }
        if (param == "uuid") { return user.uuid }
        return ""
    }
}