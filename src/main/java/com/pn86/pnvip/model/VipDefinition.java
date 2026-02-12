package com.pn86.pnvip.model;

import java.util.List;

public record VipDefinition(String key, String displayName, List<String> permissions, List<String> signinCommands,
                            List<String> giftCommands) {
}
