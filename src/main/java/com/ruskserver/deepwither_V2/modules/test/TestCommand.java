package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Command(name = "ditest", aliases = {"dit"}, description = "DIコンテナとコマンド自動登録のテストコマンド")
public class TestCommand implements BasicCommand {

    private final TestService testService;

    @Inject
    public TestCommand(TestService testService) {
        this.testService = testService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, @NotNull String[] args) {
        // Test that the injected service works
        testService.doSomething();
        
        if (commandSourceStack.getSender() instanceof Player) {
            Player player = (Player) commandSourceStack.getSender();
            player.sendMessage("§bDIコンテナによるTestCommandの実行に成功しました！");
            player.sendMessage("§bTestServiceも正常に注入されています。");
        } else {
            commandSourceStack.getSender().sendMessage("DIコンテナによるTestCommandの実行に成功しました！");
        }
    }
}
