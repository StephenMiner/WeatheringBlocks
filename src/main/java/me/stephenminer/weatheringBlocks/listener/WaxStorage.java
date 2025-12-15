package me.stephenminer.weatheringBlocks.listener;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class WaxStorage implements Listener {
    private final NamespacedKey key;
    private final WeatheringBlocks plugin;
    public WaxStorage(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
        key = new NamespacedKey(plugin, "waxed-storage");
    }


    @EventHandler
    public void onLoad(ChunkLoadEvent event){
       Chunk chunk = event.getChunk();
       readChunkData(chunk);

    }

    @EventHandler
    public void onUnload(ChunkUnloadEvent event){
        Chunk chunk = event.getChunk();
        writeChunkData(chunk);
    }

    public void readChunkData(Chunk chunk){
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (!container.has(key, PersistentDataType.BYTE_ARRAY)) return;
        byte[] posArr = container.get(key, PersistentDataType.BYTE_ARRAY);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(posArr);
        while (inputStream.available() > 0) {
            int data = inputStream.read();
            Block b = unpackPosition(chunk, data);
            if (!plugin.transitions.containsKey(b.getType())) return;
            b.setMetadata("weathering-waxed", new FixedMetadataValue(plugin, true));
        }
    }

    public void writeChunkData(Chunk chunk){
        World world = chunk.getWorld();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream(128);
        String metaKey = "weathering-waxed";
        ChunkSnapshot snapshot = chunk.getChunkSnapshot();
        int maxY = world.getMaxHeight();
        int minY = world.getMinHeight();
        long start = System.nanoTime();
        for (int y = maxY; y < minY; y++){
            for (int x = 0; x < 16; x++){
                for (int z = 0; z < 16; z++){
                    Material mat = snapshot.getBlockType(x, y, z);
                    if (mat.isAir() || !plugin.transitions.containsKey(mat)) continue;
                    Block b = chunk.getBlock(x, y, z);
                    if (b.hasMetadata(metaKey))
                        outStream.write(packPosition(b.getLocation()));
                }
            }
        }
        if (outStream.size() == 0 && container.has(key, PersistentDataType.BYTE_ARRAY)) {
            container.remove(key);
            return;
        }
        container.set(key, PersistentDataType.BYTE_ARRAY, outStream.toByteArray());
        long end =  System.nanoTime();
        System.out.println("Finished write after " + (end - start));

    }

    public int packPosition(Location loc){
        int x = loc.getBlockX() & 0xF;
        int z = loc.getBlockZ() & 0xF;
        return loc.getBlockY() << 8 | x << 4 | z;
    }

    public Block unpackPosition(Chunk chunk, int pos) {
        int y = pos >> 8;
        int x = (pos >> 4) & 0xF;
        int z = pos & 0xF;
        return chunk.getBlock(x, y, z);
    }
}
