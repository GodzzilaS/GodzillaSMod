package ru.godzillas.utilsmod

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.Listener
import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.network.ServerConnect
import dev.xdark.clientapi.network.NetworkPlayerInfo
import dev.xdark.clientapi.text.ClickEvent
import dev.xdark.clientapi.text.HoverEvent
import dev.xdark.clientapi.text.Text
import dev.xdark.clientapi.text.TextFormatting
import java.text.SimpleDateFormat
import java.util.*

class UtilsMod : ModMain, Listener {

    private val themeColor = TextFormatting.LIGHT_PURPLE // Сделать возможность смены цвета
    private var customDiscordRpcText = false
    private var discordRpcText = "ЫаЫаЫаЫаЫ"
    private var timeInChat = true

    override fun load(api: ClientApi) {

        ServerConnect.BUS.register(this, {
            val player = api.minecraft().player

            val data = player.toString().split(", ").toTypedArray()
            val index1 = data[0].indexOf("'")
            val index2 = data[0].lastIndexOf("'")
            data[0] = data[0].substring(index1 + 1, index2)
            if (data[0] == "GodzillaS") { customDiscordRpcText = true }

        }, 1)

        ChatSend.BUS.register(this, { a: ChatSend ->

            if (a.message.equals("/glist", ignoreCase = true)) {
                val connections = api.clientConnection().playerInfos.sortedBy { it.gameProfile.name }
                val (_, text) = returnListOfMembers(connections)

                api.chat().printChatMessage(Text.of("Игроков на сервере: ${connections.size}:\n", TextFormatting.GOLD).append(text))
            }

            if (a.message.equals("/gtime", ignoreCase = true)) {
                timeInChat = !timeInChat

                val action: String = if (timeInChat) { "открыли" } else { "скрыли" }

                api.chat().printChatMessage(Text.of("Вы $action время в чате.", TextFormatting.GOLD))
            }

            if (a.message.startsWith("/gsetrpc")) {
                val data = a.message.split(" ").toTypedArray()

                var newRpcText: String = try {
                    data[1]
                } catch (e: IndexOutOfBoundsException) {
                    api.chat().printChatMessage(Text.of("Вы не указали текст."))
                    return@register
                }

                for (i in 1 until data.size) {
                    if (i != 1) {
                        newRpcText += " " + data[i]
                    }
                }

                if (newRpcText.length < 27) {
                    discordRpcText = newRpcText
                    customDiscordRpcText = true
                    api.chat().printChatMessage(Text.of("Вы установили '$newRpcText', как ваш статус в rpc.", TextFormatting.GOLD))
                } else {
                    api.chat().printChatMessage(Text.of("Ваше сообщение должно быть меньше 27-ми символов.", TextFormatting.GOLD))
                }
            }
        }, 1)

        ChatReceive.BUS.register(this, { a: ChatReceive ->
            if (timeInChat) {
                val date1 = Text.of("[", TextFormatting.DARK_GRAY)
                val date2 = Text.of(SimpleDateFormat("HH:mm:ss").format(Date()), themeColor)
                val date3 = Text.of("] ", TextFormatting.DARK_GRAY)
                a.text = Text.of("").append(date1).append(date2).append(date3).append(a.text)
            }
        }, 1)
    }

    override fun unload() {}

    private fun returnListOfMembers(connections: List<NetworkPlayerInfo>): Array<Text> {
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
}