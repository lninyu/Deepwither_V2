package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Component
public class TestListener implements Listener {

    private final TestService testService;

    @Inject
    public TestListener(TestService testService) {
        this.testService = testService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Test that the injected service works when a player joins
        testService.doSomething();
        event.getPlayer().sendMessage("§aDIコンテナによるリスナーの自動登録と依存関係の注入が正常に機能しています！");
    }
}
