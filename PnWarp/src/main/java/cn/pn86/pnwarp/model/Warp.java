package cn.pn86.pnwarp.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public record Warp(
        String name,
        String description,
        Material icon,
        UUID owner,
        String ownerName,
        Location location
) {
}
