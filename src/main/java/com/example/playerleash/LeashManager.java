package com.example.playerleash;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LeashManager {
    private final Set<UUID> leashedPlayers = new HashSet<>();
    private final Map<UUID, UUID> leashOwners = new HashMap<>();
    private final JavaPlugin plugin;

    public LeashManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isLeashed(Player player) {
        return leashedPlayers.contains(player.getUniqueId());
    }

    public UUID getLeashOwner(Player leashedPlayer) {
        return leashOwners.get(leashedPlayer.getUniqueId());
    }

    public Set<UUID> getLeashedByPlayer(Player owner) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : leashOwners.entrySet()) {
            if (entry.getValue().equals(owner.getUniqueId())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void addLeashedPlayer(Player leashedPlayer, Player owner) {
        leashedPlayers.add(leashedPlayer.getUniqueId());
        leashOwners.put(leashedPlayer.getUniqueId(), owner.getUniqueId());
        applyLeashEffects(leashedPlayer);
    }

    public void removeLeashedPlayer(Player player) {
        leashedPlayers.remove(player.getUniqueId());
        leashOwners.remove(player.getUniqueId());
        removeLeashEffects(player);
    }

    public void clearAllLeashes() {
        for (UUID uuid : new HashSet<>(leashedPlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeLeashEffects(player);
            }
        }
        leashedPlayers.clear();
        leashOwners.clear();
    }

    public Set<UUID> getLeashedPlayers() {
        return new HashSet<>(leashedPlayers);
    }

    private void applyLeashEffects(Player player) {
        try {
            PotionEffectType darkness = PotionEffectType.getByName("DARKNESS");
            if (darkness != null) {
                player.addPotionEffect(new PotionEffect(darkness, Integer.MAX_VALUE, 0, false, false, false));
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
            }
        } catch (Exception e) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, false, false, false));
        }
    }

    private void removeLeashEffects(Player player) {
        try {
            PotionEffectType darkness = PotionEffectType.getByName("DARKNESS");
            if (darkness != null) {
                player.removePotionEffect(darkness);
            }
        } catch (Exception e) {
        }
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    public void updateEffects() {
        for (UUID uuid : leashedPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyLeashEffects(player);
                
                
                UUID ownerUUID = leashOwners.get(uuid);
                if (ownerUUID != null) {
                    Player owner = Bukkit.getPlayer(ownerUUID);
                    if (owner != null && owner.isOnline() && owner.getWorld().equals(player.getWorld())) {
                        Location ownerLoc = owner.getLocation();
                        Location leashedLoc = player.getLocation();
                        Vector direction = ownerLoc.toVector().subtract(leashedLoc.toVector());
                        double distance = direction.length();
                        
                        if (distance > 0.3) {
                            int particles = Math.max(5, (int) (distance * 4));
                            for (int i = 0; i <= particles; i++) {
                                double ratio = (double) i / particles;
                                Location particleLoc = leashedLoc.clone().add(direction.clone().multiply(ratio));
                                particleLoc.add(0, 1.2, 0);
                                try {
                                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 
                                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 90, 43), 0.8f));
                                } catch (Exception e) {
                                    player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1, 0, 0, 0, 0);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private Location findSafeLocation(Location targetLoc, Location preferredLoc) {
        int x = targetLoc.getBlockX();
        int y = targetLoc.getBlockY();
        int z = targetLoc.getBlockZ();
        double preferredDist = targetLoc.distance(preferredLoc);
        Location bestLoc = targetLoc;
        double bestDist = preferredDist;
        
        for (int dy = -2; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Location checkLoc = new Location(targetLoc.getWorld(), x + dx, y + dy, z + dz);
                    if (isSafeLocation(checkLoc)) {
                        double dist = checkLoc.distance(preferredLoc);
                        if (dist < bestDist || bestLoc.equals(targetLoc)) {
                            bestLoc = checkLoc.add(0.5, 0, 0.5);
                            bestDist = dist;
                        }
                    }
                }
            }
        }
        
        if (bestLoc.equals(targetLoc)) {
            for (int dy = 0; dy <= 10; dy++) {
                Location checkLoc = new Location(targetLoc.getWorld(), x, y + dy, z);
                if (isSafeLocation(checkLoc)) {
                    double dist = checkLoc.distance(preferredLoc);
                    if (dist < bestDist) {
                        bestLoc = checkLoc.add(0.5, 0, 0.5);
                        bestDist = dist;
                    }
                }
            }
        }
        
        return bestLoc;
    }
    
    private boolean isSafeLocation(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        
        if (y < loc.getWorld().getMinHeight() || y >= loc.getWorld().getMaxHeight() - 1) {
            return false;
        }
        
        org.bukkit.block.Block feet = loc.getWorld().getBlockAt(x, y, z);
        org.bukkit.block.Block head = loc.getWorld().getBlockAt(x, y + 1, z);
        
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            return false;
        }
        
        return true;
    }
    
    public void pullLeashedPlayers(Player owner) {
        Set<UUID> leashed = getLeashedByPlayer(owner);
        if (leashed.isEmpty()) {
            return;
        }
        
        Location ownerLoc = owner.getLocation();
        
        for (UUID leashedUUID : leashed) {
            Player leashedPlayer = Bukkit.getPlayer(leashedUUID);
            if (leashedPlayer == null || !leashedPlayer.isOnline() || !leashedPlayer.getWorld().equals(owner.getWorld())) {
                continue;
            }
            
            Location leashedLoc = leashedPlayer.getLocation();
            double distance = ownerLoc.distance(leashedLoc);
            
            if (distance > 3.0) {
                Vector toOwner = ownerLoc.toVector().subtract(leashedLoc.toVector());
                Vector direction = toOwner.normalize();
                Location targetLoc = leashedLoc.clone().add(direction.multiply(1.0));
                
                if (targetLoc.distance(ownerLoc) < leashedLoc.distance(ownerLoc)) {
                    Location safeLoc = findSafeLocation(targetLoc, ownerLoc);
                    safeLoc.setYaw(leashedLoc.getYaw());
                    safeLoc.setPitch(leashedLoc.getPitch());
                    leashedPlayer.teleport(safeLoc);
                } else {
                    Vector velocity = direction.multiply(0.15);
                    leashedPlayer.setVelocity(velocity);
                }
            } else if (distance > 2.0) {
                Vector toOwner = ownerLoc.toVector().subtract(leashedLoc.toVector());
                Vector direction = toOwner.normalize();
                double speed = (distance - 2.0) * 0.06;
                Vector velocity = direction.multiply(speed);
                leashedPlayer.setVelocity(velocity);
            }
        }
    }
    
}

