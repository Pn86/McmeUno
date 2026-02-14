package cn.pn86.pnlevel.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLevelData {
    private final UUID uuid;
    private String lastName;
    private int level;
    private int exp;
    private int lastClaimedLevel;
    private final Map<String, String> dailyRewardRecord = new HashMap<>();

    public PlayerLevelData(UUID uuid, String lastName, int level, int exp) {
        this.uuid = uuid;
        this.lastName = lastName;
        this.level = level;
        this.exp = exp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = Math.max(0, Math.min(999, exp));
    }

    public int getLastClaimedLevel() {
        return lastClaimedLevel;
    }

    public void setLastClaimedLevel(int lastClaimedLevel) {
        this.lastClaimedLevel = Math.max(0, lastClaimedLevel);
    }

    public Map<String, String> getDailyRewardRecord() {
        return dailyRewardRecord;
    }
}
