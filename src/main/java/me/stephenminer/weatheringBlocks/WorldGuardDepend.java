package me.stephenminer.weatheringBlocks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;

public class WorldGuardDepend {
    private final WeatheringBlocks plugin;

    public WorldGuardDepend(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
    }



    public boolean violatesRegion(Location loc){
        World w = loc.getWorld();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(w);
        RegionManager manager =container.get(world);
        if (manager == null) return false;
        Map<String, ProtectedRegion> regionMap = manager.getRegions();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        if (plugin.blacklistAllRegions){
            Collection<ProtectedRegion> regions = regionMap.values();
            for (ProtectedRegion region : regions){
                if (region.contains(x, y, z)) return true;
            }
        }else{
            for (String regionName : plugin.blacklistedWorldGuardRegions){
                if (!regionMap.containsKey(regionName)) continue;
                ProtectedRegion region = regionMap.get(regionName);
                if (region.contains(x, y, z)) return true;
            }
        }
        return false;
    }
}
