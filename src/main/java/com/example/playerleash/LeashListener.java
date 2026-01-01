package com.example.playerleash;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LeashListener implements Listener {

    private final LeashManager leashManager;
    private final JavaPlugin plugin;
    private final Set<String> processedInteractions = new HashSet<>();

    public LeashListener(LeashManager leashManager, JavaPlugin plugin) {
        this.leashManager = leashManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (clickedEntity instanceof Player) {
            Player target = (Player) clickedEntity;
            
            if (target.getUniqueId().equals(player.getUniqueId())) {
                return;
            }
            
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item.getType() == Material.LEAD) {
                String interactionKey = player.getUniqueId() + "_" + target.getUniqueId();
                
                if (processedInteractions.contains(interactionKey)) {
                    event.setCancelled(true);
                    return;
                }
                
                processedInteractions.add(interactionKey);
                
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    processedInteractions.remove(interactionKey);
                }, 20L);
                
                boolean wasLeashed = leashManager.isLeashed(target);
                
                if (wasLeashed) {
                    leashManager.removeLeashedPlayer(target);
                    player.sendMessage("§aИгрок " + target.getName() + " отвязан!");
                    target.sendMessage("§aВы отвязаны!");
                } else {
                    leashManager.addLeashedPlayer(target, player);
                    player.sendMessage("§aИгрок " + target.getName() + " привязан!");
                    target.sendMessage("§cВы привязаны! Вы не можете двигаться и использовать предметы.");
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (leashManager.isLeashed(player)) {
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getY() != event.getTo().getY() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        } else {
            leashManager.pullLeashedPlayers(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (leashManager.isLeashed(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        
        if (leashManager.isLeashed(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (leashManager.isLeashed(player)) {
                event.setCancelled(true);
            }
        }
    }
}

