package cn.pn86.pnskill.model;

import java.util.List;

public record SkillModeDefinition(String title, long cooldownSeconds, List<String> actions) {
}
