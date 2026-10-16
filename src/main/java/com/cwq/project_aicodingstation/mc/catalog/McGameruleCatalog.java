package com.cwq.project_aicodingstation.mc.catalog;

import java.util.List;

public final class McGameruleCatalog {

    public record Def(String id, String label, String hint, boolean hot, boolean defaultOn) {}

    public static final List<Def> NON_CHEAT = List.of(
            new Def("dofiretick", "火焰蔓延", "火焰是否会点燃邻近方块", true, true),
            new Def("showcoordinates", "显示坐标", "玩家屏幕上显示 XYZ", true, true),
            new Def("dodaylightcycle", "昼夜循环", "太阳是否移动", true, true),
            new Def("doweathercycle", "天气循环", "是否下雨、打雷", true, true),
            new Def("domobspawning", "生物生成", "自然生成生物", true, true),
            new Def("mobgriefing", "生物破坏", "苦力怕、末影人等能否改地形", true, true),
            new Def("tntexplodes", "TNT 爆炸", "TNT 是否能炸开方块", true, true),
            new Def("pvp", "玩家互伤", "玩家之间能否造成伤害", true, false),
            new Def("falldamage", "摔落伤害", "从高处落下是否受伤", true, true),
            new Def("firedamage", "火焰伤害", "着火或站在岩浆里是否受伤", true, true),
            new Def("drowningdamage", "溺水伤害", "氧气耗尽后是否持续掉血", true, true),
            new Def("doimmediaterespawn", "立即重生", "跳过死亡界面", true, false),
            new Def("showdaysplayed", "显示游玩天数", "HUD 上显示这个世界已过了多少天", true, false)
    );

    public static boolean isAllowed(String id) {
        return NON_CHEAT.stream().anyMatch(d -> d.id().equals(id));
    }

    private McGameruleCatalog() {}
}
