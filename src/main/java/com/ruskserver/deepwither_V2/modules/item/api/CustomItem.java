package com.ruskserver.deepwither_V2.modules.item.api;

import com.ruskserver.deepwither_V2.core.stat.StatType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * カスタムアイテムの基底となるインターフェース。
 * ハードコーディングでアイテムを定義する際に実装します。
 */
public interface CustomItem {

    /**
     * @return アイテムの一意のID（例: "starter_sword"）
     */
    String getId();

    /**
     * @return アイテムのベースとなるMaterial
     */
    Material getMaterial();

    /**
     * @return アイテムの表示名
     */
    String getDisplayName();

    /**
     * @return アイテムの固定ステータス（ベースステータス）
     */
    Map<StatType, Double> getBaseStats();

    /**
     * @return アイテムのレアリティ
     */
    ItemRarity getRarity();

    /**
     * @return アイテム固有のフレーバーテキスト。文字列は自動で30文字改行されます。
     */
    String getFlavorText();

    /**
     * カスタムモデルデータ番号を取得します。必要に応じてオーバーライドしてください。
     * @return CustomModelData (設定しない場合は 0 を返す)
     */
    default int getCustomModelData() {
        return 0;
    }
}
