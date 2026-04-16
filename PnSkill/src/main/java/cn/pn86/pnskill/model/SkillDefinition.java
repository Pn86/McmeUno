package cn.pn86.pnskill.model;

import java.util.List;

public record SkillDefinition(
        String id,
        String name,
        SkillModeDefinition a,
        SkillModeDefinition b,
        List<String> banWorlds,
        boolean clearItem,
        SkillTitleSetting titleSetting
) {
}
