package ru.godzillas.chatmod;

import com.mojang.authlib.properties.Property;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.event.input.KeyPress;
import dev.xdark.clientapi.event.render.GuiOverlayRender;
import dev.xdark.clientapi.inventory.InventoryPlayer;
import dev.xdark.clientapi.item.ItemStack;
import dev.xdark.clientapi.network.NetworkPlayerInfo;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Arrays;

public final class ChatMod implements ModMain, Listener {

    private final int themeColor = 0xfad201;
    private boolean activeF3;
    private boolean hidden;

    @Override
    public void load(ClientApi api) {

        ChatSend.BUS.register(this, a -> {

            if (a.getMessage().equals("/glist")){
                Collection<NetworkPlayerInfo> connections = api.clientConnection().getPlayerInfos();

                String membersOnServer = "";
                for (NetworkPlayerInfo element : connections){
                    membersOnServer += String.format("%s, ", element.getGameProfile().getName());
                }
                String[] arr = membersOnServer.split(", ");
                Arrays.sort(arr);

                String sortedMemberOnServer = "";
                for (String elem : arr) {
                    sortedMemberOnServer += String.format("%s, ", elem);
                }

                api.chat().printChatMessage(Text.of(String.format("Игроков на сервере: %s:", connections.size()), TextFormatting.LIGHT_PURPLE));
                api.chat().printChatMessage(Text.of(sortedMemberOnServer, TextFormatting.GRAY));
            }

            if (a.getMessage().equals("/ghide")){
                hidden = !hidden;

                String action;
                if (hidden) {
                    action = "скрыли";
                } else {
                    action = "открыли";
                }

                api.chat().printChatMessage(Text.of(String.format("Вы %s HUD", action), TextFormatting.LIGHT_PURPLE));
            }

            if (a.getMessage().contains("/ginfo")) {
                String[] data = a.getMessage().split(" ");

                String name;
                try {
                    name = data[1];
                } catch(IndexOutOfBoundsException e) {
                    api.chat().printChatMessage(Text.of("Вы не указали имя игрока."));
                    return;
                }

                NetworkPlayerInfo networkPlayerInfo;
                try {
                    networkPlayerInfo = api.clientConnection().getPlayerInfo(name);
                    networkPlayerInfo.getGameProfile().getId();
                } catch (NullPointerException e){
                    api.chat().printChatMessage(Text.of(String.format("Пользователь '%s' не найден", name)));
                    return;
                }

                EntityPlayerSP player = api.minecraft().getPlayer();

                String creative;
                if (player.isCreative()) {
                    creative = "Да";
                } else {
                    creative = "Нет";
                }

                String properties = "";
                for (Property elem : networkPlayerInfo.getGameProfile().getProperties().values()) {
                    properties += String.format("%s: %s\n", elem.getName(), elem.getValue());
                }

                String info = String.format("Информация об %s:\n", name);
                info += String.format("Uuid: %s\n", networkPlayerInfo.getGameProfile().getId());
                info += String.format("Пинг: %s\n", networkPlayerInfo.getResponseTime());
                info += String.format("У этого игрока: \n%s", properties);
                info += String.format("Еда: %s\n", player.getFoodStats().getFoodLevel());
                info += String.format("Уровень: %s\n", player.getExperienceLevel());
                info += String.format("Опыт: %s\n", player.getExperienceTotal());
                info += String.format("В креативе: %s\n", creative);
                info += String.format("В руке: %s\n", player.getInventory().getCurrentItem().getDisplayName());
                info += String.format("Количество: %s", player.getInventory().getCurrentItem().getCount());

                api.chat().printChatMessage(Text.of(info));
            }
        }, 1);

        ChatReceive.BUS.register(this, a -> {
            Text text = a.getText();

            Text date1 = Text.of("[", TextFormatting.DARK_GRAY);
            Text date2 = Text.of(new SimpleDateFormat("HH:mm:ss").format(new Date()), TextFormatting.LIGHT_PURPLE);
            Text date3 = Text.of("]", TextFormatting.DARK_GRAY);
            String message = date1.getFormattedText() + date2.getFormattedText() + date3.getFormattedText() + " " + text.getFormattedText();

            a.setText(Text.of(message));
            api.discordRpc().updateState("Существует на хоббитоне с:");
        }, 1);

        KeyPress.BUS.register(this, a -> { if (a.getKey() == 61) { activeF3 = !activeF3; } }, 1);

        GuiOverlayRender.BUS.register(this, a -> {
            if (!activeF3 && !hidden) {
                EntityPlayerSP player = api.minecraft().getPlayer();

                String[] data = player.toString().split(", ");
                int index1 = data[0].indexOf("'");
                int index2 = data[0].lastIndexOf("'");
                data[0] = data[0].substring(index1 + 1, index2);
                NetworkPlayerInfo networkPlayerInfo = api.clientConnection().getPlayerInfo(data[0]);

                api.fontRenderer().drawStringWithShadow("UtilsMod by GodzillaS", 2.0F, 15.0F, themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Ник: %s", data[0]), 2.0F, 15.0F + (10.0F * 1), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Координаты: %s %s %s", data[2], data[3], data[4].replace("]", "")), 2.0F, 15.0F + (10.0F * 2), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Пинг: %s", networkPlayerInfo.getResponseTime()), 2.0F, 15.0F + (10.0F * 3), themeColor);

                InventoryPlayer inv = player.getInventory();
                ItemStack inTheHand = inv.getCurrentItem();
                ItemStack[] myArr = {inTheHand, inv.armorItemInSlot(3), inv.armorItemInSlot(2), inv.armorItemInSlot(1), inv.armorItemInSlot(0)};
                int pos = 3;
                int indexInArray = 0;

                for (ItemStack item : myArr) {
                    indexInArray += 1;
                    if (!item.isEmpty()) {
                        String txt = String.format("%s: %s %s", returnText(indexInArray), item.getDisplayName(), checkForNull(item));
                        api.fontRenderer().drawStringWithShadow(txt, 20.0F, 19.0F + (10.0F * (pos += 1)), themeColor);
                        api.renderItem().renderItemIntoGUI(item, 2, 5 + (10 * (pos += 1)));
                    }
                }
            }
        }, 1);
    }

    String returnText (int index) {
        if (index == 1) {
            return "В руке";
        }
        if (index == 2) {
            return "Шлем";
        }
        if (index == 3) {
            return "Нагрудник";
        }
        if (index == 4) {
            return "Штаны";
        }
        if (index == 5) {
            return "Ботинки";
        }
        return "";
    }

    String checkForNull (ItemStack shit) {
        if (shit.isEmpty() || shit.getMaxDamage() == 0 || shit.getMaxDamage() == shit.getMaxDamage() - shit.getItemDamage()) {
            return "";
        } else {
            try {
                return String.format("[%s]", shit.getMaxDamage() - shit.getItemDamage());
            } catch (NullPointerException e) {
                return "";
            }
        }
    }

    @Override
    public void unload() {
    }
}

