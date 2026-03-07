package com.pn86.pngoldprospecting;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProspectingBlock {
    private final String id;
    private Location location;
    private Material skin;
    private int resetTimeSeconds;
    private boolean opened;
    private long openedAtMillis;
    private final Map<String, LootEntry> loots = new LinkedHashMap<>();

    public ProspectingBlock(String id, Location location, Material skin, int resetTimeSeconds) {
        this.id = id;
        this.location = location;
        this.skin = skin;
        this.resetTimeSeconds = resetTimeSeconds;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Material getSkin() {
        return skin;
    }

    public void setSkin(Material skin) {
        this.skin = skin;
    }

    public int getResetTimeSeconds() {
        return resetTimeSeconds;
    }

    public void setResetTimeSeconds(int resetTimeSeconds) {
        this.resetTimeSeconds = resetTimeSeconds;
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public long getOpenedAtMillis() {
        return openedAtMillis;
    }

    public void setOpenedAtMillis(long openedAtMillis) {
        this.openedAtMillis = openedAtMillis;
    }

    public Map<String, LootEntry> getLoots() {
        return loots;
    }

    public void tickReset() {
        if (!opened || resetTimeSeconds <= 0) {
            return;
        }
        long elapsedMs = System.currentTimeMillis() - openedAtMillis;
        if (elapsedMs >= resetTimeSeconds * 1000L) {
            opened = false;
            openedAtMillis = 0L;
        }
    }
}
