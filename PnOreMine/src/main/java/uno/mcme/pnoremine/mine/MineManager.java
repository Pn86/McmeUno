package uno.mcme.pnoremine.mine;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MineManager {

    private final Map<String, MineRegion> mineMap = new HashMap<>();

    public void replaceAll(Collection<MineRegion> mines) {
        mineMap.clear();
        for (MineRegion mine : mines) {
            mineMap.put(mine.getName().toLowerCase(), mine);
        }
    }

    public MineRegion findMine(String name) {
        if (name == null) {
            return null;
        }
        return mineMap.get(name.toLowerCase());
    }

    public MineRegion findMineByLocation(Location location) {
        for (MineRegion mine : mineMap.values()) {
            if (mine.contains(location)) {
                return mine;
            }
        }
        return null;
    }

    public Collection<MineRegion> getMines() {
        return new ArrayList<>(mineMap.values());
    }

    public Set<String> getMineWorlds() {
        Set<String> worlds = new HashSet<>();
        for (MineRegion mine : mineMap.values()) {
            worlds.add(mine.getWorldName().toLowerCase());
        }
        return worlds;
    }

    public MineRegion getWorldPrimaryMine(String worldName) {
        for (MineRegion mine : mineMap.values()) {
            if (mine.getWorldName().equalsIgnoreCase(worldName)) {
                return mine;
            }
        }
        return null;
    }

    public boolean canPvp(Location location) {
        for (MineRegion mine : mineMap.values()) {
            if (mine.isPvpEnabled() && mine.containsXZ(location)) {
                return true;
            }
        }
        return false;
    }
}
