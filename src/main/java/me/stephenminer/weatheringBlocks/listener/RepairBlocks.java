package me.stephenminer.weatheringBlocks.listener;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import me.stephenminer.weatheringBlocks.transition.BlockTransitions;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class RepairBlocks implements Listener {
    private final WeatheringBlocks plugin;
    public RepairBlocks(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
    }


    @EventHandler
    public void waxBlock(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (item.getType() != Material.HONEYCOMB) return;
        Block block = event.getClickedBlock();
        Material type = block.getType();
        if (!plugin.transitions.containsKey(type)) return;
        Player player = event.getPlayer();
        if (block.hasMetadata("weathering-waxed")) {
            player.sendMessage(ChatColor.YELLOW + "This block is already waxed!");
            return;
        }
        block.setMetadata("weathering-waxed", new FixedMetadataValue(plugin, true));
        item.setAmount(item.getAmount() - 1);
    }

    @EventHandler
    public void waxOff(PlayerInteractEvent event){
       // Bukkit.broadcastMessage(Bukkit.getServer().getVersion());
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (!isAxe(item.getType())) return;
        Block block = event.getClickedBlock();
        if (!plugin.transitions.containsKey(block.getType()) || !block.hasMetadata("weathering-waxed")) return;
        block.removeMetadata("weathering-waxed", plugin);
        World world = block.getWorld();
        int[] version = plugin.unboxVersionStr();
        Location loc = block.getLocation();
        if (version.length >= 2 && version[1] > 17)
            world.playEffect(loc, Effect.COPPER_WAX_OFF, 5);
        else world.spawnParticle(Particle.SMOKE, loc.clone().add(0.5,1,0.5), 50);
    }

    private boolean isAxe(Material mat){
        String sMat = mat.toString().toLowerCase(Locale.ROOT);
        return !sMat.contains("pickaxe") && sMat.contains("axe");
    }


    @EventHandler
    public void attemptRepair(PlayerInteractEvent event){
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (!isGlue(item)) return;
        Player player = event.getPlayer();
        World world = player.getWorld();
        world.playSound(player, Sound.BLOCK_SLIME_BLOCK_BREAK,1, 2);
        world.spawnParticle(Particle.ITEM_SLIME, player.getLocation().clone().add(0.5,1,0.5), 50);
        item.setAmount(item.getAmount() - 1);
        fixAreaManhattan(player.getLocation(), plugin.glueArea());
    }


    private void fixAreaManhattan(Location center, int area){
        World world = center.getWorld();
        for (int x = center.getBlockX() - area; x <= center.getBlockX() + area; x++){
            for (int z = center.getBlockZ() - area; z <= center.getBlockZ() + area; z++){
                for (int y = center.getBlockY() - area; y <= center.getBlockY() + area; y++){
                    if (Math.abs(center.getBlockX() - x) + Math.abs(center.getBlockY() - y) + Math.abs(center.getBlockZ() - z) > area) continue;
                    BlockState state= world.getBlockState(x, y, z);
                    Material changeTo = findRepairMat(state.getType());
                    if (changeTo == null) continue;
                    state.setType(changeTo);
                    state.update(true);
                }
            }
        }
    }

    private Material findRepairMat(Material current){
        if (!plugin.transitions.containsKey(current)) return null;
        BlockTransitions transition = plugin.transitions.get(current);
        if (transition.stage() == 0) return null;
        if (plugin.glueFullRepair())
            return findStage(transition, 0);
        if (transition.repairTo() != null) return transition.repairTo();
        return findStage(transition, transition.stage() - 1);
    }

    private Material findStage(BlockTransitions current, int stage){
        String groupId = current.group();
        List<BlockTransitions> group = plugin.transitionGroups.get(groupId);
        List<Material> pool = new ArrayList<>(4);
        for (int i = 0; i < group.size(); i++){
            BlockTransitions transition  = group.get(i);
            if (transition.stage() != stage) continue;
            if (i + 1 < group.size() - 1 && group.get(i + 1).stage() == transition.stage()) {
                pool.add(group.get(i+1).parent());
                i++;
            }
            pool.add(transition.parent());
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private boolean isGlue(ItemStack item){
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(plugin.itemKey, PersistentDataType.STRING) && container.get(plugin.itemKey, PersistentDataType.STRING).equals("glue");
    }
}
