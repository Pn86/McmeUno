package com.pn86.pnseen.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private long totalPlaytimeMillis;
    private long lastJoinMillis;
    private long lastSeenMillis;
    private String lastWorld;
    private double lastX;
    private double lastY;
    private double lastZ;
    private String lastIp;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalPlaytimeMillis() {
        return totalPlaytimeMillis;
    }

    public void setTotalPlaytimeMillis(long totalPlaytimeMillis) {
        this.totalPlaytimeMillis = totalPlaytimeMillis;
    }

    public long getLastJoinMillis() {
        return lastJoinMillis;
    }

    public void setLastJoinMillis(long lastJoinMillis) {
        this.lastJoinMillis = lastJoinMillis;
    }

    public long getLastSeenMillis() {
        return lastSeenMillis;
    }

    public void setLastSeenMillis(long lastSeenMillis) {
        this.lastSeenMillis = lastSeenMillis;
    }

    public String getLastWorld() {
        return lastWorld;
    }

    public void setLastWorld(String lastWorld) {
        this.lastWorld = lastWorld;
    }

    public double getLastX() {
        return lastX;
    }

    public void setLastX(double lastX) {
        this.lastX = lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public void setLastY(double lastY) {
        this.lastY = lastY;
    }

    public double getLastZ() {
        return lastZ;
    }

    public void setLastZ(double lastZ) {
        this.lastZ = lastZ;
    }

    public String getLastIp() {
        return lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }
}
