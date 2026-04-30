package com.ruskserver.deepwither_V2.modules.item.modifier;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.stat.StatType;
import com.ruskserver.deepwither_V2.modules.item.api.CustomItem;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

@Service
public class ModifierManager {

    private final Random random = new Random();

    /**
     * カスタムアイテム生成時に、共通のルールに従ってランダムなモディファイアを付与します。
     *
     * @param item 生成対象のCustomItem
     * @return 付与されるモディファイアのマップ
     */
    public Map<StatType, Double> rollModifiers(CustomItem item) {
        Map<StatType, Double> modifiers = new EnumMap<>(StatType.class);

        // アイテムのベースステータスが存在するStatTypeに対して、一定のランダムボーナスを付与する簡易的な実装
        for (StatType type : item.getBaseStats().keySet()) {
            // 20%の確率でモディファイアが付く
            if (random.nextDouble() < 0.20) {
                // ベース値の 1% 〜 10% くらいのランダムボーナス (簡易実装)
                double base = item.getBaseStats().get(type);
                double bonus = base * (0.01 + (random.nextDouble() * 0.09));
                
                // 四捨五入して小数第1位までにする
                bonus = Math.round(bonus * 10.0) / 10.0;
                if (bonus > 0) {
                    modifiers.put(type, bonus);
                }
            }
        }
        
        // （将来拡張用）完全ランダムでHPボーナスなどが付くなどのロジックも追加可能

        return modifiers;
    }
}
