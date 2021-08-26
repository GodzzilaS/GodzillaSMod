package ru.godzillas.utilsmod

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.Listener
import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.chat.TabComplete
import dev.xdark.clientapi.event.input.KeyPress
import dev.xdark.clientapi.event.input.MousePress
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.network.ServerConnect
import dev.xdark.clientapi.event.render.GuiOverlayRender
import dev.xdark.clientapi.item.ItemStack
import dev.xdark.clientapi.network.NetworkPlayerInfo
import dev.xdark.clientapi.text.ClickEvent
import dev.xdark.clientapi.text.HoverEvent
import dev.xdark.clientapi.text.Text
import dev.xdark.clientapi.text.TextFormatting
import org.lwjgl.input.Keyboard
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*

class UtilsMod : ModMain, Listener {

    private val themeColor = TextFormatting.LIGHT_PURPLE // Сделать возможность смены цвета
    private var availableSlots = 0
    private var onlineSeconds = 0
    private var cps = 0 // Сделать по человечески счётчик кпс, а не просто макс значение
    private var maxCps = 0
    private var index = 0
    private var activeF3 = false
    private var hiddenHUD = false
    private var lmbDown = false
    private var onServer = false
    private var customDiscordRpcText = false
    private var discordRpcText = "Существует на хоббитоне >:c"
    private var timer: ScheduledFuture<*>? = null
    private var resetCPS: ScheduledFuture<*>? = null
    private var beforeContent = "." // Костыльнуть метод TabComplete
    private var content = ""

    override fun load(api: ClientApi) {

        ServerConnect.BUS.register(this, {
            val player = api.minecraft().player

            // println(URL("https://google.com").readText())

            val data = player.toString().split(", ").toTypedArray()
            val index1 = data[0].indexOf("'")
            val index2 = data[0].lastIndexOf("'")
            data[0] = data[0].substring(index1 + 1, index2)
            if (data[0] == "GodzillaS") { customDiscordRpcText = true }

            if (timer == null || timer!!.isCancelled) {
                onServer = true
                timer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ onlineSeconds += 1 }, 0, 1, TimeUnit.SECONDS)
                resetCPS = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ if (cps != 0) { cps = 0 } }, 0, 1, TimeUnit.SECONDS)
            }
        }, 1)

        ChatSend.BUS.register(this, { a: ChatSend ->

            if (a.message.equals("/glist", ignoreCase = true)) {
                val connections = api.clientConnection().playerInfos.sortedBy { it.gameProfile.name }
                val (_, text) = returnListOfMembers(connections)

                api.chat().printChatMessage(Text.of("Игроков на сервере: ${connections.size}:\n", TextFormatting.GOLD).append(text))
            }

            if (a.message.equals("/ghide", ignoreCase = true)) {
                hiddenHUD = !hiddenHUD

                val action: String = if (hiddenHUD) {
                    "скрыли"
                } else {
                    "открыли"
                }

                api.chat().printChatMessage(Text.of("Вы $action HUD", TextFormatting.GOLD))
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

            if (a.message.startsWith("/ginfo")) {
                val data = a.message.split(" ").toTypedArray()

                val name: String = try {
                    data[1]
                } catch (e: IndexOutOfBoundsException) {
                    api.chat().printChatMessage(Text.of("Вы не указали имя игрока."))
                    return@register
                }

                val networkPlayerInfo: NetworkPlayerInfo
                try {
                    networkPlayerInfo = api.clientConnection().getPlayerInfo(name)
                    networkPlayerInfo.gameProfile.id
                } catch (e: NullPointerException) {
                    api.chat().printChatMessage(Text.of("Пользователь '$name' не найден"))
                    return@register
                }

                val player = api.minecraft().player
                val creative: String = if (player.isCreative) "Да" else "Нет"

                var properties = ""
                for (elem in networkPlayerInfo.gameProfile.properties.values()) {
                    properties += "${elem.name}: ${elem.value}\n"
                }

                var info = "Информация об $name:\n"
                info += "Uuid: ${networkPlayerInfo.gameProfile.id}\n"
                info += "Пинг: ${networkPlayerInfo.responseTime}\n"
                info += "\n$properties"
                info += "Информация об вас:\n"
                info += "Еда: ${player.foodStats.foodLevel}\n"
                info += "Уровень: ${player.experienceLevel}\n"
                info += "Опыт: ${player.experienceTotal}\n"
                info += "В креативе: $creative\n"
                info += "В руке: ${player.inventory.currentItem.displayName}\n"
                info += "Количество: ${player.inventory.currentItem.count}"
                api.chat().printChatMessage(Text.of(info))
            }
        }, 1)

        ChatReceive.BUS.register(this, { a: ChatReceive ->
            val date1 = Text.of("[", TextFormatting.DARK_GRAY)
            val date2 = Text.of(SimpleDateFormat("HH:mm:ss").format(Date()), themeColor)
            val date3 = Text.of("] ", TextFormatting.DARK_GRAY)

            a.text = Text.of("").append(date1).append(date2).append(date3).append(a.text)
        }, 1)

        KeyPress.BUS.register(this, { a: KeyPress -> if (a.key == 61) { activeF3 = !activeF3 } }, 1)

        TabComplete.BUS.register(this,  { a: TabComplete ->
            var btnPressed = false
            val data = a.input.split(" ").toTypedArray()

            if (data.lastIndex != 0) {
                if (!data[data.lastIndex].startsWith(beforeContent, ignoreCase = true)) {
                    content = data[data.lastIndex]
                    beforeContent = content[0].toString()
                    index = 0
                }

                val connections = api.clientConnection().playerInfos.sortedBy { it.gameProfile.name }
                val newCollection: MutableList<NetworkPlayerInfo> = arrayListOf()

                for (element in connections) {
                    if (element.gameProfile.name.startsWith(beforeContent, ignoreCase = true)) {
                        newCollection.add(element)
                    } else {
                        continue
                    }
                }

                val (_, text) = returnListOfMembers(newCollection)
                api.chat().printChatMessage(text)

                for ((i, element) in newCollection.withIndex()) {
                    if (element.gameProfile.name.startsWith(beforeContent, ignoreCase = true) && !btnPressed && i == index) {
                        a.setCompletions(newCollection[i].gameProfile.name)
                        btnPressed = true
                        index = if (i + 1 == newCollection.size) { 0 } else { i + 1 }
                    }
                }
            }
        }, 1)

        MousePress.BUS.register(this, { a: MousePress ->
            lmbDown = if (a.button == 0 && !lmbDown) {
                ++cps
                true
            } else {
                false
            }
        }, 1)

        GameLoop.BUS.register(this, {
            if (customDiscordRpcText) {
                api.discordRpc().updateState(discordRpcText)
            }

            if (onServer) {
                val btnTabPressed = Keyboard.isKeyDown(Keyboard.KEY_TAB)
                hiddenHUD = btnTabPressed

                try {
                    var num = 0
                    for (i in 0..36) {
                        val item = api.minecraft().player.inventory.getStackInSlot(i)
                        if (item.isEmpty) {
                            ++num
                        }
                    }
                    availableSlots = num
                } catch (e: NullPointerException) {
                    onServer = false
                    timer?.cancel(false)
                }

                if (cps > maxCps) {
                    maxCps = cps
                }
            }
        }, 5)

        GuiOverlayRender.BUS.register(this, {
            if (!activeF3 && !hiddenHUD && onServer) {
                val player = api.minecraft().player
                val inv = player.inventory

                val data = player.toString().split(", ").toTypedArray()
                val index1 = data[0].indexOf("'")
                val index2 = data[0].lastIndexOf("'")
                data[0] = data[0].substring(index1 + 1, index2)
                val networkPlayerInfo = api.clientConnection().getPlayerInfo(data[0])

                api.overlayRenderer().drawRect(1, 12, 240, 104, 0x3B000000)
                api.overlayRenderer().drawRect(1, 12, 240, 24, 0x40000000)

                api.fontRenderer().drawStringWithShadow("UtilsMod by GodzillaS", (120 - api.fontRenderer().getStringWidth("UtilsMod by GodzillaS") / 2).toFloat(), 14f, 0x55ffff)
                api.fontRenderer().drawStringWithShadow("Ник: ${networkPlayerInfo.displayName.formattedText}", 3.0f, 15.0f + 10.0f * 1, 0xffffff)
                api.fontRenderer().drawStringWithShadow("Координаты: ${data[2]} ${data[3]} ${data[4].replace("]", "")}", 3.0f, 15.0f + 10.0f * 2, 0xffffff)
                api.fontRenderer().drawStringWithShadow("CPS | max: $cps | $maxCps", 3.0f, 15.0f + 10.0f * 3, 0xffffff)
                api.fontRenderer().drawStringWithShadow("Пинг: ${networkPlayerInfo.responseTime}", 3.0f, 15.0f + 10.0f * 4, returnColor(networkPlayerInfo.responseTime))
                api.fontRenderer().drawStringWithShadow("Онлайн: ${getGreatTimeFromSeconds(onlineSeconds)}", 3.0f, 15.0f + 10.0f * 5, 0xffffff)
                api.fontRenderer().drawStringWithShadow("Опыт | Уровень: ${player.experienceTotal} | ${player.experienceLevel}", 3.0f, 15.0f + 10.0f * 6, 0xffffff)
                api.fontRenderer().drawStringWithShadow("${inv.displayName.formattedText}:", 3.0f, 15.0f + 10.0f * 7, 0xffffff)
                api.fontRenderer().drawStringWithShadow("Свободных слотов: $availableSlots", 3.0f, 15.0f + 10.0f * 8, 0xffffff)

                val myArr = arrayOf(inv.currentItem, inv.armorItemInSlot(3), inv.armorItemInSlot(2), inv.armorItemInSlot(1), inv.armorItemInSlot(0))

                var pos = 9
                var indexInArray = 0
                for (item in myArr) {
                    ++indexInArray
                    if (!item.isEmpty) {
                        val txt = "${returnPosition(indexInArray)}: ${item.displayName} ${returnDurability(item)}"
                        api.fontRenderer().drawStringWithShadow(txt, 21.0f, 20.0f + 10.0f * pos, 0xffffff)
                        api.renderItem().renderItemAndEffectIntoGUI(item, 3, 15 + 10 * pos)
                        pos += 2
                        //api.overlayRenderer().displayItemActivation(item);
                    }
                }
            }
        }, 1)
    }

    override fun unload() {}

    private fun returnPosition(index: Int): String {
        if (index == 1) {
            return "В руке"
        }
        if (index == 2) {
            return "Шлем"
        }
        if (index == 3) {
            return "Нагрудник"
        }
        if (index == 4) {
            return "Штаны"
        }
        return if (index == 5) {
            "Ботинки"
        } else ""
    }

    private fun returnDurability(shit: ItemStack): String {
        return if (shit.isEmpty || shit.maxDamage == 0 || shit.maxDamage == shit.maxDamage - shit.itemDamage) {
            ""
        } else {
            try {
                "[${shit.maxDamage - shit.itemDamage}]"
            } catch (e: NullPointerException) {
                ""
            }
        }
    }

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

    private fun getGreatTimeFromSeconds(seconds: Int): String {
        val timeString: String
        val h: Int = seconds / 3600
        val m: Int = seconds % 3600 / 60
        val s: Int = seconds % 60
        timeString = String.format("%02d:%02d:%02d", h, m, s)
        return timeString
    }

    private fun returnColor(num: Int): Int {
        if (num > 50) {
            return 0xf80000
        }
        return if (num > 12) {
            0xfde910
        } else 0xffffff
    }
}