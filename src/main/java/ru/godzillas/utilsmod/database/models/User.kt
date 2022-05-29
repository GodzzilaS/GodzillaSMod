package ru.godzillas.utilsmod.database.models

data class User(val uuid: String,
                val name: String,
                val chat_color: String,
                val enabled_rpc: Boolean,
                val text_rpc: String) {

    override fun toString(): String {
        return "User(uuid=$uuid, name=$name, chat_color=$chat_color, enabled_rpc=$enabled_rpc, text_rpc=$text_rpc)"
    }

}
