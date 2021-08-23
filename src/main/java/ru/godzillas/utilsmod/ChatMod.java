package ru.godzillas.utilsmod;

import com.mojang.authlib.properties.Property;
import dev.xdark.clientapi.ClientApi;
import dev.xdark.clientapi.entity.EntityPlayerSP;
import dev.xdark.clientapi.entry.ModMain;
import dev.xdark.clientapi.event.Listener;
import dev.xdark.clientapi.event.chat.ChatReceive;
import dev.xdark.clientapi.event.chat.ChatSend;
import dev.xdark.clientapi.event.input.KeyPress;
import dev.xdark.clientapi.event.input.MousePress;
import dev.xdark.clientapi.event.lifecycle.GameLoop;
import dev.xdark.clientapi.event.network.ServerConnect;
import dev.xdark.clientapi.event.render.GuiOverlayRender;
import dev.xdark.clientapi.inventory.InventoryPlayer;
import dev.xdark.clientapi.item.ItemStack;
import dev.xdark.clientapi.network.NetworkPlayerInfo;
import dev.xdark.clientapi.text.Text;
import dev.xdark.clientapi.text.TextFormatting;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ChatMod implements ModMain, Listener {

    private final int themeColor = 0xffffff;
    private int availableSlots = 0;
    public int onlineSeconds = 0;
    private float cps = 0;
    private boolean activeF3;
    private boolean hidden;
    private boolean started = true;
    private boolean customDiscordRpcText = false;
    private boolean clicked = false;
    private String discordRpcText = "Существует на хоббитоне >:c";

    private static ScheduledExecutorService timer = null;


    @Override
    public void load(ClientApi api) {

        ServerConnect.BUS.register(this, a -> {
            if (timer != null) {
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(() -> onlineSeconds += 1, 0, 1, TimeUnit.SECONDS);
            }
        }, 1);

        ChatSend.BUS.register(this, a -> {
            if (a.getMessage().equalsIgnoreCase("/glist")){
                started = !started;
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

                sortedMemberOnServer = sortedMemberOnServer.substring(0, sortedMemberOnServer.length() - 1);
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

                for (int i = 1; i < data.length; i++) {
                    if (i != 1) {
                        newRpcText += " " + data[i];
                    }
                }

                discordRpcText = newRpcText;
                customDiscordRpcText = true;
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

            String message = date1.getFormattedText() + date2.getFormattedText() + date3.getFormattedText() + " " + msg;
            a.setText(Text.of(message));
        }, 1);

        KeyPress.BUS.register(this, a -> { if (a.getKey() == 61) { activeF3 = !activeF3; } }, 1);

        MousePress.BUS.register(this, a -> {
            if (a.getButton() == 0 && !clicked) {
                int sec = new Date().getSeconds();
                cps += 1;
                clicked = true;
            } else {
                clicked = false;
            }
        }, 1);

        GameLoop.BUS.register(this, a -> {
            if (customDiscordRpcText) {
                api.discordRpc().updateState(discordRpcText);
            }

            int num = 0;
            for (int i = 0; i < 37; i++) {
                ItemStack item = api.minecraft().getPlayer().getInventory().getStackInSlot(i);
                if (item.isEmpty()) {
                    num += 1;
                }
            }
            availableSlots = num;

        }, 5);

        GuiOverlayRender.BUS.register(this, a -> {
            if (!activeF3 && !hidden) {
                EntityPlayerSP player = api.minecraft().getPlayer();
                InventoryPlayer inv = player.getInventory();

                String[] data = player.toString().split(", ");
                int index1 = data[0].indexOf("'");
                int index2 = data[0].lastIndexOf("'");
                data[0] = data[0].substring(index1 + 1, index2);
                NetworkPlayerInfo networkPlayerInfo = api.clientConnection().getPlayerInfo(data[0]);

                api.overlayRenderer().drawRect(1, 12, 240, 104, 0x3B000000);
                api.overlayRenderer().drawRect(1, 12, 240, 24, 0x40000000);

                api.fontRenderer().drawStringWithShadow("UtilsMod by GodzillaS", 120 - (api.fontRenderer().getStringWidth("UtilsMod by GodzillaS") / 2), 14, 0x55ffff);
                api.fontRenderer().drawStringWithShadow(String.format("Ник: %s", networkPlayerInfo.getDisplayName().getFormattedText()), 3.0F, 15.0F + (10.0F * 1), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Координаты: %s %s %s", data[2], data[3], data[4].replace("]", "")), 3.0F, 15.0F + (10.0F * 2), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("CPS: %s", cps), 3.0F, 15.0F + (10.0F * 3), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Пинг: %s", networkPlayerInfo.getResponseTime()), 3.0F, 15.0F + (10.0F * 4), returnColor(networkPlayerInfo.getResponseTime()));
                api.fontRenderer().drawStringWithShadow(String.format("Онлайн: %s", getGreatTimeFromSeconds(onlineSeconds)), 3.0F, 15.0F + (10.0F * 5), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Опыт | Уровень: %s | %s", player.getExperienceTotal(), player.getExperienceLevel()), 3.0F, 15.0F + (10.0F * 6), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("%s:", inv.getDisplayName().getFormattedText()), 3.0F, 15.0F + (10.0F * 7), themeColor);
                api.fontRenderer().drawStringWithShadow(String.format("Свободных слотов: %s", availableSlots), 3.0F, 15.0F + (10.0F * 8), themeColor);

                ItemStack[] myArr = {inv.getCurrentItem(), inv.armorItemInSlot(3), inv.armorItemInSlot(2), inv.armorItemInSlot(1), inv.armorItemInSlot(0)};
                int pos = 9;
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

    String getGreatTimeFromSeconds (int seconds) {
        String timeString;
        int h, m, s;
        h = seconds / 3600;
        m = (seconds % 3600) / 60;
        s = seconds % 60;

        timeString = String.format("%02d:%02d:%02d", h, m, s);
        return timeString;
    }

    int returnColor (Integer num) {
        if (num > 50) {
            return 0xf80000;
        }
        if (num > 12) {
            return 0xfde910;
        }
        return themeColor;
    }
}
