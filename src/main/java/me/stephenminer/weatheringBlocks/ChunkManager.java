package me.stephenminer.weatheringBlocks;

import me.stephenminer.weatheringBlocks.transition.BlockTransitions;
import org.bukkit.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class ChunkManager implements Listener {
    private final WeatheringBlocks plugin;
    private final Random random;

    private boolean stop;
    public ChunkManager(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
        this.random = new Random();
    }


    public void start(){
        stop = false;
        new BukkitRunnable(){
            @Override
            public void run(){
                if (stop) {
                    this.cancel();
                    return;
                }
                for (World world : Bukkit.getWorlds()) {
                    tickChunks(world);
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    public void stop(){
        stop = true;
    }

    public void tickChunks(World world){
        int randomTick = world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
        Chunk[] chunks = world.getLoadedChunks();
        for (Chunk chunk : chunks){
            if (chunk.getLoadLevel() != Chunk.LoadLevel.ENTITY_TICKING) continue;
            randomTickChunk(world, chunk.getChunkSnapshot(),randomTick);
        }
    }


    public void  randomTickChunk(World world, ChunkSnapshot chunk, int randomTick){
        for (int by = world.getMinHeight(); by < world.getMaxHeight(); by += 16){
            for (int i = 0; i < randomTick; i++) {
                int y = random.nextInt(by, by + 16);
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                Material mat = chunk.getBlockType(x, y, z);
                System.out.println(mat.name());
                if (!plugin.transitions.containsKey(mat)) continue;

                BlockTransitions transition = plugin.transitions.get(mat);
                Location loc = new Location(world, chunk.getX() * 16 + x, y, chunk.getZ() * 16 + z);
                transition.updateState(loc, random);
            }
        }
    }

}
