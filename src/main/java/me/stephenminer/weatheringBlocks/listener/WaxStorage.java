package me.stephenminer.weatheringBlocks.listener;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    @EventHandler
    public void cleanBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        block.removeMetadata("weathering-waxed", plugin);
    }

    @EventHandler
    public void cleanExplode(EntityExplodeEvent event){
        List<Block> blocks = event.blockList();
        for (Block block : blocks){
            block.removeMetadata("weathering-waxed", plugin);
        }
    }

    @EventHandler
    public void cleanBExplode(BlockExplodeEvent event){
        List<Block> blocks = event.blockList();
        for (Block block : blocks){
            block.removeMetadata("weathering-waxed", plugin);
        }
    }

    @EventHandler
    public void fallingBlockClean(EntitySpawnEvent event){
        Block block = event.getLocation().getBlock();
        block.removeMetadata("weathering-waxed",plugin);
    }

    @EventHandler
    public void cleanPistonExtend(BlockPistonExtendEvent event){
        List<Block> blocks = event.getBlocks();
        BlockFace dir = event.getDirection();
        String key = "weathering-waxed";
    //    blocks.getLast().setType(Material.GLASS);
    //    blocks.getLast().getRelative(dir).setType(Material.RED_STAINED_GLASS);
        for (int i = blocks.size()-1; i >= 0; i--){
            Block block = blocks.get(i);
            Block next = block.getRelative(dir);
            System.out.println(block.getLocation());
            if (block.hasMetadata(key)){
                block.removeMetadata(key, plugin);
                next.setMetadata(key, new FixedMetadataValue(plugin, true));
            }
        }
    }

    @EventHandler
    public void cleanPistonRetract(BlockPistonRetractEvent event){
        List<Block> blocks = event.getBlocks();
        BlockFace dir = event.getDirection().getOppositeFace();
        String key = "weathering-waxed";
        for (int i = 0; i < blocks.size(); i++){
            Block block = blocks.get(i);
            Block prev = block.getRelative(dir);
            if (block.hasMetadata(key)){
                block.removeMetadata(key,plugin);
                prev.setMetadata(key, new FixedMetadataValue(plugin, true));
            }
        }
    }
    public void readChunkData(Chunk chunk){
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (!container.has(key, PersistentDataType.BYTE_ARRAY)) return;
        byte[] posArr = container.get(key, PersistentDataType.BYTE_ARRAY);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(posArr);
        while (inputStream.available() > 2) {
            System.out.println(22);
            int d1 = inputStream.read();
            int d2 = inputStream.read();
            int d3 = inputStream.read();
            int data = (d1 << 16) | (d2 << 8) | d3;
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
        long start = System.currentTimeMillis();
        for (int y = minY; y < maxY; y++){
            for (int x = 0; x < 16; x++){
                for (int z = 0; z < 16; z++){
                    Material mat = snapshot.getBlockType(x, y, z);
                    //plugin.getLogger().info(mat.name());
                    if (mat.isAir() || !plugin.transitions.containsKey(mat)) continue;
                    Block b = chunk.getBlock(x, y, z);

                    if (b.hasMetadata(metaKey)) {
                        writePos(outStream, packPosition(b.getLocation()));
                       // outStream.write(packPosition(b.getLocation()));
                    }
                }
            }
        }
        if (outStream.size() == 0 && container.has(key, PersistentDataType.BYTE_ARRAY)) {
            container.remove(key);
            return;
        }
        container.set(key, PersistentDataType.BYTE_ARRAY, outStream.toByteArray());

        long end =  System.currentTimeMillis();
        //System.out.println("Finished write after " + (end - start));

    }

    public void writePos(ByteArrayOutputStream outStream, int pos){
        outStream.write(pos >>> 16 & 0xFF); //y coordinate in bits 16-23
        outStream.write(pos >>> 8 & 0xFF); //x coordinate in bits 8-15
        outStream.write(pos & 0xFF); //z coordinate in bits 0-7
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
