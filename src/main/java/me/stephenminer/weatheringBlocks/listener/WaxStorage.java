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
import java.util.ArrayList;
import java.util.List;

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
            b.setMetadata("weathering-waxed", new FixedMetadataValue(plugin, true));
        }
        System.out.println("Reading data from chunk");
        try {
            inputStream.close();
        }catch (Exception ignored){}
    }

    public void writeChunkData(Chunk chunk){
        World world = chunk.getWorld();
        List<Integer> positions = new ArrayList<>();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++){
            for (int x = 0; x < 16; x++){
                for (int z = 0; z < 16; z++){
                    Block b = chunk.getBlock(x, y, z);
                    if (b.hasMetadata("weathering-waxed"))
                        positions.add(packPosition(b.getLocation()));
                }
            }
        }
        if (positions.isEmpty() && container.has(key, PersistentDataType.BYTE_ARRAY)) {
            container.remove(key);
            return;
        }
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        for (Integer pos : positions)
            outStream.write(pos);
        container.set(key, PersistentDataType.BYTE_ARRAY, outStream.toByteArray());
        System.out.println("Writing data to chunk");
        try{
            outStream.close();
        }catch (Exception ignored){}

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
