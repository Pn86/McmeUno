package com.pn86.pntimeworks;

import java.util.List;

public record WorkGroup(String id, List<TimeRule> rules, List<String> commands) {
}
