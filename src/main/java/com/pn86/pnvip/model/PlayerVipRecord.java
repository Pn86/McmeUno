package com.pn86.pnvip.model;

import java.util.HashMap;
import java.util.Map;

public class PlayerVipRecord {
    private String name;
    private final Map<String, Long> vipExpireAt = new HashMap<>();
    private final Map<String, String> lastSigninDate = new HashMap<>();

    public PlayerVipRecord(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Long> getVipExpireAt() {
        return vipExpireAt;
    }

    public Map<String, String> getLastSigninDate() {
        return lastSigninDate;
    }
}
