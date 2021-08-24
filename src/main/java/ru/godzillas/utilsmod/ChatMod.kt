package ru.godzillas.utilsmod

import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.Listener
import dev.xdark.clientapi.event.chat.ChatReceive
import dev.xdark.clientapi.event.chat.ChatSend
import dev.xdark.clientapi.event.input.KeyPress
import dev.xdark.clientapi.event.input.MousePress
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.network.ServerConnect
import dev.xdark.clientapi.event.render.GuiOverlayRender
import dev.xdark.clientapi.item.ItemStack
import dev.xdark.clientapi.network.NetworkPlayerInfo
import dev.xdark.clientapi.text.Text
import dev.xdark.clientapi.text.TextFormatting
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ChatMod : ModMain, Listener {

    private val themeColor = 0xffffff
    private var availableSlots = 0
    private var onlineSeconds = 0
    private var cps = 0f
    private var activeF3 = false
    private var hidden = false
    private var started = true
    private var clicked = false
    private var customDiscordRpcText = false
    private var discordRpcText = "Существует на хоббитоне >:c"
    private var timer: ScheduledExecutorService? = null

    override fun load(api: ClientApi) {

        ServerConnect.BUS.register(this, {
            val player = api.minecraft().player

            val data = player.toString().split(", ").toTypedArray()
            val index1 = data[0].indexOf("'")
            val index2 = data[0].lastIndexOf("'")
            data[0] = data[0].substring(index1 + 1, index2)
            if (data[0] == "GodzillaS") { customDiscordRpcText = true }

            if (timer == null) {
                timer = Executors.newSingleThreadScheduledExecutor()
                timer?.scheduleAtFixedRate({ onlineSeconds += 1 }, 0, 1, TimeUnit.SECONDS)
            }
        }, 1)

        ChatSend.BUS.register(this, { a: ChatSend ->

            if (a.message.equals("/glist", ignoreCase = true)) {
                started = !started
                val connections = api.clientConnection().playerInfos

                var membersOnServer = ""
                for (element in connections) {
                    membersOnServer += String.format("%s, ", element.gameProfile.name)
                }

                val arr = membersOnServer.split(", ").toTypedArray()
                Arrays.sort(arr)
                var sortedMemberOnServer = ""
                for (elem in arr) {
                    sortedMemberOnServer += String.format("%s, ", elem)
                }

                sortedMemberOnServer = sortedMemberOnServer.substring(0, sortedMemberOnServer.length - 1)
                api.chat().printChatMessage(Text.of(String.format("Игроков на сервере: %s:", connections.size), TextFormatting.GOLD))
                api.chat().printChatMessage(Text.of(sortedMemberOnServer, TextFormatting.GRAY))
            }

            if (a.message.equals("/ghide", ignoreCase = true)) {
                hidden = !hidden

                val action: String = if (hidden) {
                    "скрыли"
                } else {
                    "открыли"
                }

                api.chat().printChatMessage(Text.of(String.format("Вы %s HUD", action), TextFormatting.GOLD))
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

                discordRpcText = newRpcText
                customDiscordRpcText = true
                api.chat().printChatMessage(Text.of(String.format("Вы установили '%s', как ваш статус в rpc.", newRpcText), TextFormatting.GOLD))
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
                    api.chat().printChatMessage(Text.of(String.format("Пользователь '%s' не найден", name)))
                    return@register
                }

                val player = api.minecraft().player
                val creative: String = if (player.isCreative) { "Да" } else { "Нет" }

                var properties = ""
                for (elem in networkPlayerInfo.gameProfile.properties.values()) {
                    properties += String.format("%s: %s\n", elem.name, elem.value)
                }

                var info = String.format("Информация об %s:\n", name)
                info += String.format("Uuid: %s\n", networkPlayerInfo.gameProfile.id)
                info += String.format("Пинг: %s\n", networkPlayerInfo.responseTime)
                info += String.format("\n%s", properties)
                info += "Информация об вас:\n"
                info += String.format("Еда: %s\n", player.foodStats.foodLevel)
                info += String.format("Уровень: %s\n", player.experienceLevel)
                info += String.format("Опыт: %s\n", player.experienceTotal)
                info += String.format("В креативе: %s\n", creative)
                info += String.format("В руке: %s\n", player.inventory.currentItem.displayName)
                info += String.format("Количество: %s", player.inventory.currentItem.count)
                api.chat().printChatMessage(Text.of(info))
            }
        }, 1)

        ChatReceive.BUS.register(this, { a: ChatReceive ->
            val date1 = Text.of("[", TextFormatting.DARK_GRAY)
            val date2 = Text.of(SimpleDateFormat("HH:mm:ss").format(Date()), TextFormatting.LIGHT_PURPLE)
            val date3 = Text.of("] ", TextFormatting.DARK_GRAY)

            a.text = Text.of("").append(date1).append(date2).append(date3).append(a.text)
        }, 1)

        KeyPress.BUS.register(this, { a: KeyPress -> if (a.key == 61) { activeF3 = !activeF3 } }, 1)

        MousePress.BUS.register(this, { a: MousePress ->
            if (a.button == 0 && !clicked) {
                cps += 1f
                clicked = true
            } else {
                clicked = false
            }
        }, 1)

        GameLoop.BUS.register(this, {
            if (customDiscordRpcText) {
                api.discordRpc().updateState(discordRpcText)
            }

            var num = 0
            for (i in 0..36) {
                val item = api.minecraft().player.inventory.getStackInSlot(i)
                if (item.isEmpty) {
                    num += 1
                }
            }
            availableSlots = num
        }, 5)

        GuiOverlayRender.BUS.register(this, {
            if (!activeF3 && !hidden) {
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
                api.fontRenderer().drawStringWithShadow(String.format("Ник: %s", networkPlayerInfo.displayName.formattedText), 3.0f, 15.0f + 10.0f * 1, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("Координаты: %s %s %s", data[2], data[3], data[4].replace("]", "")), 3.0f, 15.0f + 10.0f * 2, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("CPS: %s", cps), 3.0f, 15.0f + 10.0f * 3, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("Пинг: %s", networkPlayerInfo.responseTime), 3.0f, 15.0f + 10.0f * 4, returnColor(networkPlayerInfo.responseTime))
                api.fontRenderer().drawStringWithShadow(String.format("Онлайн: %s", getGreatTimeFromSeconds(onlineSeconds)), 3.0f, 15.0f + 10.0f * 5, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("Опыт | Уровень: %s | %s", player.experienceTotal, player.experienceLevel), 3.0f, 15.0f + 10.0f * 6, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("%s:", inv.displayName.formattedText), 3.0f, 15.0f + 10.0f * 7, themeColor)
                api.fontRenderer().drawStringWithShadow(String.format("Свободных слотов: %s", availableSlots), 3.0f, 15.0f + 10.0f * 8, themeColor)

                val myArr = arrayOf(inv.currentItem, inv.armorItemInSlot(3), inv.armorItemInSlot(2), inv.armorItemInSlot(1), inv.armorItemInSlot(0))

                var pos = 9
                var indexInArray = 0
                for (item in myArr) {
                    indexInArray += 1
                    if (!item.isEmpty) {
                        val txt = String.format("%s: %s %s", returnPosition(indexInArray), item.displayName, returnDurability(item))
                        api.fontRenderer().drawStringWithShadow(txt, 21.0f, 20.0f + 10.0f * pos, themeColor)
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
                String.format("[%s]", shit.maxDamage - shit.itemDamage)
            } catch (e: NullPointerException) {
                ""
            }
        }
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
        } else themeColor
    }
}