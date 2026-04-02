package uno.mcme.pnoremine.mine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MineRegion {

    private final String name;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int resetTimeSeconds;
    private final DropMode dropMode;
    private final List<OreEntry> ores;
    private final int spawnX;
    private final int spawnY;
    private final int spawnZ;
    private final boolean pvpEnabled;
    private int remainingSeconds;
    private int totalWeight;

    public MineRegion(String name,
                      String worldName,
                      int minX,
                      int minY,
                      int minZ,
                      int maxX,
                      int maxY,
                      int maxZ,
                      int resetTimeSeconds,
                      DropMode dropMode,
                      List<OreEntry> ores,
                      int spawnX,
                      int spawnY,
                      int spawnZ,
                      boolean pvpEnabled) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.resetTimeSeconds = resetTimeSeconds;
        this.dropMode = dropMode;
        this.ores = ores;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.pvpEnabled = pvpEnabled;
        this.remainingSeconds = resetTimeSeconds;
        this.totalWeight = ores.stream().mapToInt(OreEntry::weight).sum();
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public DropMode getDropMode() {
        return dropMode;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public Location getSpawnLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, spawnX + 0.5, spawnY, spawnZ + 0.5);
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean containsXZ(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public OreEntry getOreEntry(Material material) {
        for (OreEntry ore : ores) {
            if (ore.material() == material) {
                return ore;
            }
        }
        return null;
    }

    public void tick() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }

    public boolean isReadyToReset() {
        return remainingSeconds <= 0;
    }

    public void reset() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    block.setType(nextMaterial(), false);
                }
            }
        }
        remainingSeconds = resetTimeSeconds;
    }

    private Material nextMaterial() {
        int rand = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int sum = 0;
        for (OreEntry ore : ores) {
            sum += ore.weight();
            if (rand <= sum) {
                return ore.material();
            }
        }
        return ores.isEmpty() ? Material.STONE : ores.getFirst().material();
    }
}
