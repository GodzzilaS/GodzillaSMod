package ru.godzillas.utilsmod.utils

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.network.NetworkPlayerInfo
import dev.xdark.clientapi.text.ClickEvent
import dev.xdark.clientapi.text.HoverEvent
import dev.xdark.clientapi.text.Text
import dev.xdark.clientapi.text.TextFormatting

object PlayerUtils {

    fun getUserUuidAndName(text: String, api: ClientApi): List<Any> {
        val unformattedText: CharSequence = text
        println(unformattedText)
        val a1 = unformattedText
            .replace(Regex("§."), "")
            .replace(Regex("¨......"), "")
            .replace(Regex("».+"), "")
            .replace(Regex(":.+"), "")
            .replace(Regex("[\\[\\]]"), "")
            .split(' ')

        for (str in a1) {
            try {
                val playerInfo: NetworkPlayerInfo? = api.clientConnection().getPlayerInfo(str)
                if (playerInfo != null) {
                    return listOf(playerInfo.gameProfile.id, playerInfo.gameProfile.name)
                }
            } catch (ex: Exception) {
                continue
            }
        }
        return listOf()
    }

    fun getListOfUsers(connections: List<NetworkPlayerInfo>): Array<Text> {
        val text: Text = Text.of("")
        var name: Text = Text.of("")

        for ((i, element) in connections.withIndex()) {
            name = Text.of("${element.gameProfile.name}${ if (i + 1 < connections.size) ", " else ""}", TextFormatting.GRAY)
            text.append(name.setStyle(name.style
                .setClickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, "/msg ${element.gameProfile.name} "))
                .setHoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, Text.of("Написать игроку")))
            ))
        }

        return arrayOf(name, text)
    }

    fun getUserStaffGroup(user: NetworkPlayerInfo): String {
        val group: String = try {
            user.gameProfile.properties.get("group.staff").elementAt(0).value
        } catch (ex: IndexOutOfBoundsException) {
            "PLAYER"
        }

        return group
    }

    fun getClientName(api: ClientApi): String {
        val player = api.minecraft().player

        val data = player.toString().split(", ").toTypedArray()
        val index1 = data[0].indexOf("'")
        val index2 = data[0].lastIndexOf("'")
        data[0] = data[0].substring(index1 + 1, index2)

        return data[0]
    }
}
