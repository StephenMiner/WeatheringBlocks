package me.stephenminer.weatheringBlocks.command;

import me.stephenminer.weatheringBlocks.WeatheringBlocks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GiveItem implements CommandExecutor, TabCompleter {
    private final WeatheringBlocks plugin;

    public GiveItem(){
        this.plugin = JavaPlugin.getPlugin(WeatheringBlocks.class);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if (!sender.hasPermission("wblocks.commands.glue")){
            sender.sendMessage(ChatColor.RED + "You do not have permission to use htis coammnd!");
            return false;
        }
        int size = args.length;
        if (size < 1){
            sender.sendMessage(ChatColor.RED + "You must at least specify the item you wish to give!");
            return false;
        }
        String id = args[0];
        if (!validId(id)){
            sender.sendMessage(ChatColor.RED + id + " is not a valid item id for this plugin!");
            return false;
        }
        ItemStack glue = plugin.constructGlue();
        if (glue == null) {
            sender.sendMessage(ChatColor.RED + "Failed to create glue item!");
            return false;
        }
        int num = 1;
        if (size >= 2){
            try{
                num = Integer.parseInt(args[1]);
                if (!(sender instanceof Player player)){
                    sender.sendMessage(ChatColor.RED + "You cannot give yourself items as a non-player");
                    return false;
                }
                glue.setAmount(num);
                player.getInventory().addItem(glue);
                sender.sendMessage(ChatColor.GREEN + "Item has been handed out!");
                return true;
            }catch (Exception ignored){}
            Player player = Bukkit.getPlayerExact(args[1]);
            if (player == null){
                sender.sendMessage(ChatColor.RED + "Failed to find player with name " + args[1]);
                return false;
            }
            if (size >= 3){
                try{
                    num = Integer.parseInt(args[2]);
                }catch (Exception e){
                    sender.sendMessage("Failed to parse number from " + args[2]);
                    return false;
                }
            }
            glue.setAmount(num);
            player.getInventory().addItem(glue);
            sender.sendMessage(ChatColor.GREEN + "Item has been handed out!");
            return true;
        }
        if (sender instanceof Player player){
            glue.setAmount(num);
            player.getInventory().addItem(glue);
            sender.sendMessage(ChatColor.GREEN + "Item has been handed out!");
            return true;
        }else sender.sendMessage(ChatColor.RED + "You cannot give yourself items as nonplayer!");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args){
        int size = args.length;
        if (size == 1) return items(args[0]);
        return null;
    }




    public boolean validId(String id){
        return switch (id){
            case "glue" -> true;
            default -> false;
        };
    }

    private List<String> items(String match){
        List<String> items = new ArrayList<>();
        items.add("glue");
        return items;
    }
}
