package me.stephenminer.weatheringBlocks.transition;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFormEvent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;


public class BlockTransitions {
    private final Material parent;
    private final Transition[] transitions;
    private final float preChance;

    public BlockTransitions(Material parent, float preChance, Transition[] transitions){
        this.parent = parent;
        this.preChance = preChance;
        this.transitions = transitions;
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

    public Material getNext(final Location loc, Random random){
        Arrays.sort(transitions, Comparator.comparingDouble((t1) ->
            t1.realChance(loc)
        ));

        float roll = random.nextFloat();
        float sum = 0;
        Material next = parent;
        for (int i = 0; i < transitions.length; i++){
            Transition transition = transitions[i];
            sum += transition.realChance(loc);
            if (roll >= sum) continue;
            next = transition.target();
            break;
        }
        return next;
    }
}
