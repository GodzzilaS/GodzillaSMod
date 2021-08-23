package ru.godzillas.utilsmod;

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

    private final int themeColor = 0xffffff;
    private boolean activeF3;
    private boolean hidden;
    private String discordRpcText = "Существует на хоббитоне >:c";

    @Override
    public void load(ClientApi api) {

        ChatSend.BUS.register(this, a -> {
            if (a.getMessage().equalsIgnoreCase("/glist")){
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

                api.chat().printChatMessage(Text.of(String.format("Игроков на сервере: %s:", connections.size()), TextFormatting.GOLD));
                api.chat().printChatMessage(Text.of(sortedMemberOnServer, TextFormatting.GRAY));
            }

            if (a.getMessage().equalsIgnoreCase("/ghide")){
                hidden = !hidden;

                String action;
                if (hidden) {
                    action = "скрыли";
                } else {
                    action = "открыли";
                }

                api.chat().printChatMessage(Text.of(String.format("Вы %s HUD", action), TextFormatting.GOLD));
            }

            if (a.getMessage().startsWith("/gsetrpc")){
                String[] data = a.getMessage().split(" ");

                String newRpcText;
                try {
                    newRpcText = data[1];
                } catch(IndexOutOfBoundsException e) {
                    api.chat().printChatMessage(Text.of("Вы не указали текст."));
                    return;
                }

                discordRpcText = newRpcText;
                api.chat().printChatMessage(Text.of(String.format("Вы установили '%s', как ваш статус.", newRpcText), TextFormatting.GOLD));
            }

            if (a.getMessage().startsWith("/ginfo")) {
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
                info += String.format("\n%s", properties);
                info += "Информация об вас:\n";
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

            String msg, editedText = "";
            TextFormatting style = null;
            try {
                for (Text element : text){
                    style = element.getStyle().getColor();
                }

                String[] crutch = text.getFormattedText().split(" ");
                int index = Arrays.asList(crutch).indexOf("»");

                if (style != null && index != -1) {
                    int i = 0;
                    for (String word : crutch) {
                        if (i > index) {
                            editedText += style + word + " ";
                        } else {
                            editedText += word + " ";
                            i += 1;
                        }
                    }
                } else {
                    editedText = text.getFormattedText();
                }

                msg = Text.of(editedText).getFormattedText();
            } catch (NullPointerException e) {
                msg = Text.of(text.getFormattedText()).getFormattedText();
            }

            api.discordRpc().updateState(discordRpcText);
            String message = date1.getFormattedText() + date2.getFormattedText() + date3.getFormattedText() + " " + msg;
            a.setText(Text.of(message));
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

                api.overlayRenderer().drawRect(1, 12, 240, 74, 0x3B000000);
                api.overlayRenderer().drawRect(1, 12, 240, 24, 0x40000000);

                api.fontRenderer().drawStringWithShadow("UtilsMod by GodzillaS", 120 - (api.fontRenderer().getStringWidth("UtilsMod by GodzillaS") / 2), 15.0F, 0x55ffff);
                api.fontRenderer().drawStringWithShadow(String.format("Ник: %s", networkPlayerInfo.getDisplayName().getFormattedText()), 3.0F, 15.0F + (10.0F * 1), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Координаты: %s %s %s", data[2], data[3], data[4].replace("]", "")), 3.0F, 15.0F + (10.0F * 2), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Пинг: %s", networkPlayerInfo.getResponseTime()), 3.0F, 15.0F + (10.0F * 3), returnColor(networkPlayerInfo.getResponseTime(), "ping"));
                api.fontRenderer().drawStringWithShadow(String.format("Еда: %s", player.getFoodStats().getFoodLevel()), 3.0F, 15.0F + (10.0F * 4), returnColor(player.getFoodStats().getFoodLevel(), "food"));
                api.fontRenderer().drawStringWithShadow(String.format("Опыт | Уровень: %s | %s", player.getExperienceTotal(), player.getExperienceLevel()), 3.0F, 15.0F + (10.0F * 5), themeColor);

                InventoryPlayer inv = player.getInventory();
                ItemStack[] myArr = {inv.getCurrentItem(), inv.armorItemInSlot(3), inv.armorItemInSlot(2), inv.armorItemInSlot(1), inv.armorItemInSlot(0)};
                int pos = 6;
                int indexInArray = 0;

                for (ItemStack item : myArr) {
                    indexInArray += 1;
                    if (!item.isEmpty()) {
                        String txt = String.format("%s: %s %s", returnPosition(indexInArray), item.getDisplayName(), returnDurability(item));
                        api.fontRenderer().drawStringWithShadow(txt, 21.0F, 20.0F + (10.0F * (pos)), themeColor);
                        api.renderItem().renderItemAndEffectIntoGUI(item, 3, 15 + (10 * (pos)));
                        pos += 2;
                        //api.overlayRenderer().displayItemActivation(item);
                    }
                }
            }
        }, 1);
    }

    @Override
    public void unload() { }

    String returnPosition (int index) {
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

    String returnDurability (ItemStack shit) {
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

    int returnColor (Integer num, String param) {
        if (param.equals("food")) {
            if (num < 6) {
                return 0xf80000;
            }
            if (num < 12) {
                return 0xfde910;
            }
        } else {
            if (num > 50) {
                return 0xf80000;
            }
            if (num > 12) {
                return 0xfde910;
            }
        }
        return themeColor;
    }
}

