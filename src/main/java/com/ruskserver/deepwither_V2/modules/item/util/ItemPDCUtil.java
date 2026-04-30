package com.ruskserver.deepwither_V2.modules.item.util;

import com.ruskserver.deepwither_V2.Deepwither_V2;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ItemPDCUtil {

    private final NamespacedKey idKey;
    private final NamespacedKey modifierKey;

    @Inject
    public ItemPDCUtil(Deepwither_V2 plugin) {
        this.idKey = new NamespacedKey(plugin, "custom_item_id");
        this.modifierKey = new NamespacedKey(plugin, "custom_item_modifiers");
    }

    public void setItemId(ItemStack item, String id) {
        if (item == null || item.getItemMeta() == null) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    public String getItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    public void setModifiers(ItemStack item, Map<StatType, Double> modifiers) {
        if (item == null || item.getItemMeta() == null || modifiers == null) return;
        ItemMeta meta = item.getItemMeta();
        
        // シンプルな文字列シリアライズ形式 (例: "ATTACK_DAMAGE:2.4,DEFENSE:1.5")
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<StatType, Double> entry : modifiers.entrySet()) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(entry.getKey().name()).append(":").append(entry.getValue());
        }

        meta.getPersistentDataContainer().set(modifierKey, PersistentDataType.STRING, sb.toString());
        item.setItemMeta(meta);
    }

    public Map<StatType, Double> getModifiers(ItemStack item) {
        Map<StatType, Double> result = new EnumMap<>(StatType.class);
        if (item == null || item.getItemMeta() == null) return result;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String modifierString = pdc.get(modifierKey, PersistentDataType.STRING);
        
        if (modifierString != null && !modifierString.isEmpty()) {
            String[] parts = modifierString.split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2) {
                    try {
                        StatType type = StatType.valueOf(kv[0]);
                        double value = Double.parseDouble(kv[1]);
                        result.put(type, value);
                    } catch (IllegalArgumentException ignored) {
                        // StatTypeの名前が変わった、あるいは無効な値の場合は無視
                    }
                }
            }
        }
        return result;
    }
}
