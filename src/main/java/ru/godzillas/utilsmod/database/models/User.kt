package ru.godzillas.utilsmod.database.models

data class User(val uuid: String, val name: String, val chat_color: String ) {

    override fun toString(): String {
        return "User(uuid=$uuid, name=$name, chat_color=$chat_color)"
    }

}
