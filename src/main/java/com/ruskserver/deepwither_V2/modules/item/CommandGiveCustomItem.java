package com.ruskserver.deepwither_V2.modules.item;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Command(name = "givecustom", description = "カスタムアイテムを取得します")
public class CommandGiveCustomItem implements BasicCommand {

    private final ItemManager itemManager;

    @Inject
    public CommandGiveCustomItem(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (!(stack.getSender() instanceof Player)) {
            stack.getSender().sendMessage("プレイヤーのみ実行可能です。");
            return;
        }

        Player player = (Player) stack.getSender();

        if (args.length < 1) {
            player.sendMessage("§c使用方法: /givecustom <アイテムID>");
            return;
        }

        String itemId = args[0];
        ItemStack item = itemManager.generate(itemId);

        if (item == null) {
            player.sendMessage("§cアイテムID '" + itemId + "' は見つかりません。");
            return;
        }

        player.getInventory().addItem(item);
        player.sendMessage("§aカスタムアイテム '" + itemId + "' を取得しました！");
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        String prefix = "";
        if (args.length > 0) {
            prefix = args[args.length - 1].toLowerCase();
        }
        
        final String finalPrefix = prefix;
        return itemManager.getRegisteredItemIds().stream()
                .filter(id -> id.toLowerCase().startsWith(finalPrefix))
                .collect(Collectors.toList());
    }
}
