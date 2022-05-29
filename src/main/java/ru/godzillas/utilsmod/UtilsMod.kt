package ru.godzillas.utilsmod

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.Listener
import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.network.ServerConnect
import dev.xdark.clientapi.text.*
import ru.godzillas.utilsmod.database.Database
import ru.godzillas.utilsmod.utils.PlayerUtils
import java.text.SimpleDateFormat
import java.util.*

internal var clientNick: String = ""


class UtilsMod : ModMain, Listener {

    private var enabledRpc = false
    private var enabledTimeInChat = true
    private var rpcText = "ЫаЫаЫаЫаЫ"
    private var colorOfTime = "FF55FF"

    override fun load(api: ClientApi) {

        ServerConnect.BUS.register(this, {
            if (clientNick == "") { clientNick = PlayerUtils.getClientName(api) }
            if (clientNick == "GodzillaS") { enabledRpc = true }

            // Считываю статку, сколько людей зашло с модом, так-же через это работает конфиг со временем в чате
            api.threadManagement().newSingleThreadedExecutor().execute {
                val user = Database.createUser(api)
                colorOfTime = user.chat_color
                enabledRpc = user.enabled_rpc
                rpcText = user.text_rpc
                Database.loginUser(api)
            }
        }, 1)

        ChatSend.BUS.register(this, { a: ChatSend ->

            if (a.message.equals("/glist", ignoreCase = true)) {
                val connections = api.clientConnection().playerInfos.sortedBy { it.gameProfile.name }
                val (_, text) = PlayerUtils.getListOfUsers(connections)
                api.chat().printChatMessage(Text.of("§bИгроков на сервере: §c${connections.size}§b:§r\n").append(text))
            }

            if (a.message.equals("/gtime", ignoreCase = true)) {
                enabledTimeInChat = !enabledTimeInChat
                val action: String = if (enabledTimeInChat) { "показываете" } else { "скрываете" }
                api.chat().printChatMessage(Text.of("§bВы §c$action§b время в чате."))
            }

            if (a.message.startsWith("/gsetrpc")) {
                val data = a.message.split(" ").toTypedArray()

                var newRpcText: String = try {
                    data[1]
                } catch (e: IndexOutOfBoundsException) {
                    api.chat().printChatMessage(Text.of("§bВы не указали §cтекст своего статуса§b."))
                    return@register
                }

                for (i in 1 until data.size) {
                    if (i != 1) {
                        newRpcText += " " + data[i]
                    }
                }

                if (newRpcText.length <= 27) {
                    rpcText = newRpcText
                    enabledRpc = true
                    api.chat().printChatMessage(Text.of("§bВы установили §с'$newRpcText'§b, как ваш статус в rpc."))
                    Database.updateUser(api, colorOfTime, rpcText, enabledRpc)
                } else {
                    api.chat().printChatMessage(Text.of("§bВаше сообщение должно быть §cменьше§b 27-ми символов."))
                }
            }

            if (a.message.startsWith("/gtimecolor")) {
                val data = a.message.split(" ")

                if (data.size != 2) {
                    api.chat().printChatMessage(Text.of("§bИспользование: §c/gtimecolor #hex"))
                    return@register
                }
                var hex = data[1].lowercase()

                if (hex.length != 7) {
                    api.chat().printChatMessage(Text.of("§b$clientNick, §c$hex§b не является hex'ом"))
                    return@register
                }

                val isHex = hex.matches(Regex("#[\\d\\[a-fA-F\\]]{6}"))
                if (isHex) {
                    hex = hex.replace("#", "")
                    if (colorOfTime != hex) {
                        colorOfTime = hex
                        api.chat().printChatMessage(Text.of("§bВы установили ¨$hex#$hex§b цвет для вашего времени."))
                        Database.updateUser(api, colorOfTime, rpcText, enabledRpc)
                    } else {
                        api.chat().printChatMessage(Text.of("§bУ вас уже стоит этот hex"))
                    }
                } else {
                    api.chat().printChatMessage(Text.of("§b$clientNick, §c$hex§b не является hex'ом"))
                }
            }

        }, 1)

        ChatReceive.BUS.register(this, { a: ChatReceive ->
            val senderData = PlayerUtils.getUserUuidAndName(a.text.formattedText, api)

            if (senderData.isNotEmpty()) {
                val senderNick = senderData[1].toString()
                if (senderNick != clientNick) {
                    val newText = Text.of("")
                    for (str in a.text.parts) {
                        if (str.unformattedText.contains(senderNick)) {
                            str.style = str.style
                                .setClickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, "/mute $senderNick "))
                                .setHoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, Text.of("§bНажмите для выдачи мута")))
                        }
                        newText.append(str)
                    }
                    a.text = newText
                }
            }

            if (enabledTimeInChat) {
                val date = Text.of("§8[¨${colorOfTime.lowercase()}" + SimpleDateFormat("HH:mm:ss").format(Date()) + "§8]§r ")
                a.text = Text.of("").append(date).append(a.text)
            }
        }, 3)

        GameLoop.BUS.register(this, {
            if (clientNick == "") {
                try {
                    clientNick = PlayerUtils.getClientName(api)
                } catch (_: java.lang.NullPointerException) { }
            }

            if (enabledRpc) {
                api.discordRpc().updateState(rpcText)
            }
        }, 2)
    }

    override fun unload() {
        ServerConnect.BUS.unregisterAll(this)
        ChatSend.BUS.unregisterAll(this)
        ChatReceive.BUS.unregisterAll(this)
        GameLoop.BUS.unregisterAll(this)
    }
}