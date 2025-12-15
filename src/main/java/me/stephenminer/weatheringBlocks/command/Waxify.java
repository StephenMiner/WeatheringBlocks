package me.stephenminer.weatheringBlocks.command;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Waxify implements CommandExecutor, TabCompleter {
    private final WeatheringBlocks plugin;


    public Waxify(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!sender.hasPermission("wblocks.commands.waxify")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return false;
        }

        if (args.length < 1){
            sender.sendMessage(ChatColor.RED + "You need to specify what block you want to get!");
            return false;
        }
        String matStr = args[0];
        Material mat = plugin.materialFromString(matStr);
        if (mat == null){
            sender.sendMessage(ChatColor.RED + "Could not find a material with the name: " + mat);
            return false;
        }
        if (!mat.isBlock()){
            sender.sendMessage(ChatColor.RED + "The chosen material needs to be a block!");
            return false;
        }
        if (args.length >= 2){
            try{
                int i = Integer.parseInt(args[1]);
                if (sender instanceof Player player){
                    player.getInventory().addItem(waxedBlock(mat, i));
                    sender.sendMessage(ChatColor.GREEN + "Item has been handed out!");
                    return true;
                }else {
                    sender.sendMessage(ChatColor.RED + "You cannot give yourself an item as a non-player!");
                    return false;
                }
            }catch (Exception ignored){}
            Player player = Bukkit.getPlayerExact(args[1]);
            if (player == null){
                sender.sendMessage(ChatColor.RED + "Could not find a player with the name: " + args[1] + "!");
                return false;
            }
            int i = 1;
            if (args.length >= 3){
                try{
                    i = Integer.parseInt(args[2]);
                }catch (Exception e){
                    sender.sendMessage(ChatColor.RED + "Failed to read a number from your third argument: " + args[2]);
                }
            }
            player.getInventory().addItem(waxedBlock(mat, i));
            sender.sendMessage(ChatColor.GREEN + "Item has been handed out!");
            return true;
        }else if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You cannot give yourself an item as a non-player!");
            return false;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return blocks(args[0]);
        return null;
    }


    private ItemStack waxedBlock(Material mat, int num){
        ItemStack item = new ItemStack(mat, num);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(plugin.itemKey, PersistentDataType.STRING, "waxed");
        item.setItemMeta(meta);
        return item;
    }




    private List<String> blocks(String match){
        List<String> baseList = new ArrayList<>();
        Registry.MATERIAL.forEach((mat)->{
            if (mat.isBlock())
                baseList.add(mat.name().toLowerCase());
        });
        return plugin.filter(baseList, match);
    }
}
