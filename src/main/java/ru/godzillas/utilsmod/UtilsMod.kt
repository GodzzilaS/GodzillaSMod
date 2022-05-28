package ru.godzillas.utilsmod

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.Listener
import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.network.ServerConnect
import dev.xdark.clientapi.text.*
import ru.godzillas.utilsmod.utils.PlayerUtils
import java.text.SimpleDateFormat
import java.util.*

class UtilsMod : ModMain, Listener {

    private var enabledRpc = false
    private var enabledTimeInChat = true
    private var rpcText = "ЫаЫаЫаЫаЫ"
    private var clientNick = ""
    private val staffGroupsWithoutPermission = listOf("PLAYER", "YOUTUBE", "BUILDER", "SR_BUILDER", "TESTER", "DEVELOPER")

    override fun load(api: ClientApi) {

        ServerConnect.BUS.register(this, {
            if (clientNick == "") { clientNick = PlayerUtils.getClientName(api) }
            if (clientNick == "GodzillaS") { enabledRpc = true }
        }, 1)

        ChatSend.BUS.register(this, { a: ChatSend ->
            if (clientNick == "") { clientNick = PlayerUtils.getClientName(api) }
            if (a.message.equals("/glist", ignoreCase = true)) {
                val connections = api.clientConnection().playerInfos.sortedBy { it.gameProfile.name }
                val (_, text) = PlayerUtils.getListOfUsers(connections)
                api.chat().printChatMessage(Text.of("Игроков на сервере: ${connections.size}:\n", TextFormatting.GOLD).append(text))
            }

            if (a.message.equals("/gtime", ignoreCase = true)) {
                enabledTimeInChat = !enabledTimeInChat
                val action: String = if (enabledTimeInChat) { "показываете" } else { "скрываете" }
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

                if (newRpcText.length <= 27) {
                    rpcText = newRpcText
                    enabledRpc = true
                    api.chat().printChatMessage(Text.of("Вы установили '$newRpcText', как ваш статус в rpc.", TextFormatting.GOLD))
                } else {
                    api.chat().printChatMessage(Text.of("Ваше сообщение должно быть меньше 27-ми символов.", TextFormatting.GOLD))
                }
            }
        }, 1)

        ChatReceive.BUS.register(this, { a: ChatReceive ->
            if (clientNick == "") { clientNick = PlayerUtils.getClientName(api) }
            val senderData = PlayerUtils.getUserUuidAndName(a.text.formattedText, api)

            if (senderData.isNotEmpty()) {
                val senderNick = senderData[1].toString()
                val client = api.clientConnection().getPlayerInfo(clientNick)
                val sender = api.clientConnection().getPlayerInfo(senderNick)
                val clientStaffGroup = PlayerUtils.getUserStaffGroup(client)
                val senderStaffGroup = PlayerUtils.getUserStaffGroup(sender)


                if (!staffGroupsWithoutPermission.contains(clientStaffGroup) && staffGroupsWithoutPermission.contains(senderStaffGroup)) {
                    val newText = Text.of("")
                    for (str in a.text.parts) {
                        if (str.unformattedText.contains(senderNick)) {
                            str.style = str.style
                                .setClickEvent(ClickEvent.of(ClickEvent.Action.SUGGEST_COMMAND, "/mute $senderNick "))
                                .setHoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, Text.of("Нажмите для выдачи мута")))
                        }
                        newText.append(str)
                    }
                    a.text = newText
                }
            }

            if (enabledTimeInChat) {
                val date = Text.of("§8[§d" + SimpleDateFormat("HH:mm:ss").format(Date()) + "§8]§r ")
                a.text = Text.of("").append(date).append(a.text)
            }
        }, 2)

        GameLoop.BUS.register(this, {
            if (enabledRpc) {
                api.discordRpc().updateState(rpcText)
            }
        }, 1)
    }

    override fun unload() {
        ServerConnect.BUS.unregisterAll(this)
        ChatSend.BUS.unregisterAll(this)
        ChatReceive.BUS.unregisterAll(this)
        GameLoop.BUS.unregisterAll(this)
    }
}