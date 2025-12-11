package me.stephenminer.weatheringBlocks.transition;


import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class BlockTransitions {
    private final WeatheringBlocks plugin;

    private final Material parent;
    private final Transition[] transitions;
    private final float preChance;
    private final String group;
    private final int stage;
    private final boolean lowerTransitionBlocking;

    public BlockTransitions(String group, int stage, boolean lowerTransitionBlocking, Material parent, float preChance, Transition[] transitions){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);

        this.group = group;
        this.parent = parent;
        this.preChance = preChance;
        this.transitions = transitions;
        this.stage = stage;
        this.lowerTransitionBlocking = lowerTransitionBlocking;
    }

    public BlockTransitions(Material parent, float preChance, Transition[] transitions){
        this("general", 1, false, parent, preChance, transitions);
    }



    public void updateState(Location loc, Random random){
        if (random.nextFloat() >= preChance) return;
        BlockState updated = loc.getBlock().getState();
        Material mat = getNext(loc, random);
        if (mat == parent) return; // failed
        updated.setType(mat);
        BlockFormEvent event = new BlockFormEvent(loc.getBlock(), updated);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        event.getNewState().update(true);
        //getNext(loc, random);
    }

    private float findGroupMod(final Location loc){
        final World world = loc.getWorld();
        int j = 0;
        int k = 0;
        for (int x = loc.getBlockX() - 4; x <= loc.getBlockX() + 4; x++){
            for (int z = loc.getBlockZ() - 4; z <= loc.getBlockZ() + 4; z++){
                for (int y = loc.getBlockY() - 4; y <= loc.getBlockY() + 4; y++){
                    if (x == loc.getBlockX() && y == loc.getBlockY() && z == loc.getBlockZ())
                        continue;
                    if (Math.abs(loc.getBlockX() - x) + Math.abs(loc.getBlockY() - y) + Math.abs(loc.getBlockZ() - z) > 4) continue;
                    Material mat = world.getBlockAt(x, y, z).getType();
                    if (plugin.transitions.containsKey(mat)){
                        BlockTransitions blockTransition = plugin.transitions.get(mat);
                        if (blockTransition.group().equals(group)){
                            if (blockTransition.stage() < stage)
                                if (lowerTransitionBlocking) return 0;
                            if (blockTransition.stage() > stage)
                                k++;
                            else j++;
                        }
                    }
                }
            }
        }
        float f = (k + 1) / (float) (k + j + 1);
       // System.out.println(f);
        return f * f;
    }

    private Material getNext(final Location loc, Random random){
        Arrays.sort(transitions, Comparator.comparingDouble((t1) ->
            t1.realChance(loc)
        ));

        float roll = random.nextFloat();
        float groupMod = findGroupMod(loc);
        float sum = 0;
        Material next = parent;
        for (int i = 0; i < transitions.length; i++){
            Transition transition = transitions[i];
            sum += transition.realChance(loc);
            if (transition.groupingDelay())
                sum *= groupMod;
            if (parent == Material.STONE_BRICKS)
                System.out.println(sum);
            if (roll >= sum) continue;
            next = transition.target();

            break;
        }
        return next;
    }

    public String group(){ return group; }
    public int stage(){ return stage; }
    public boolean lowerTransitionBlocking(){ return lowerTransitionBlocking; }
}
