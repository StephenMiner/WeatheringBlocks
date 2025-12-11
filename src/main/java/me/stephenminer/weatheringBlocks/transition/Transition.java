package me.stephenminer.weatheringBlocks.transition;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public record Transition(Material target, float baseChance, boolean groupingDelay, ProbabilityFlag[] flags) {


    public float realChance(Location center){
        float proposed = baseChance;
        for (ProbabilityFlag flag : flags){
            proposed = checkFlag(proposed, center, flag);
        }
        return proposed;
    }



    private float checkFlag(float current, Location center, ProbabilityFlag flag){
        World world = center.getWorld();
        int found = 0;
        for (int x = center.getBlockX() - flag.range(); x <= center.getBlockX() + flag.range(); x++){
            for (int y = center.getBlockY() - flag.range(); y <= center.getBlockY() + flag.range(); y++){
                for (int z = center.getBlockZ() - flag.range(); z <= center.getBlockZ() + flag.range(); z++){
                    if (x == center.getBlockX() && y == center.getBlockY() && z == center.getBlockZ())
                        continue;
                    if (Math.abs(center.getBlockX() - x) + Math.abs(center.getBlockY() - y) + Math.abs(center.getBlockZ() - z) > flag.range()) continue;
                    Block block = world.getBlockAt(x,y,z);
                    if (flag.mat() == block.getType())
                        found++;
                }
            }
        }
        if (flag.ratio()){
            float ratio = ((float) found) / (flag.range() * flag.range() * flag.range());
            return current + (flag.mod() * ratio);
        }
        return found == 0 ? current : current + flag.mod();
    }

}
