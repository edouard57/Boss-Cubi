package fr.moteldesope.golem;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class summon implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Golem> bosses = new HashMap<>();
    private final Map<UUID, BukkitTask> fireballTasks = new HashMap<>();

    private static final double BOSSBAR_RANGE = 50.0;

    public summon(JavaPlugin plugin) {
        this.plugin = plugin;
        startBossBarUpdater();
        startBossPotionAttacks();
    }

	 // ===============================
	 // COMMANDE /summonGolgy
	 // ===============================
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	   if (!(sender instanceof Player)) return false;
	   Player player = (Player) sender;
	   if (!cmd.getName().equalsIgnoreCase("summonGolgy")) return false;
	
	   Location loc = player.getLocation();
	
	   // Spawn du Golem
	   Golem golem = (Golem) loc.getWorld().spawnEntity(loc, EntityType.IRON_GOLEM);
	   Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
	           "attribute @e[type=iron_golem,limit=1,sort=nearest] minecraft:scale base set 1.5");
	
	   // Stats du boss
	   golem.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
	   golem.setHealth(500);
	   golem.addScoreboardTag("boss");
	
	   // Effets permanents
	   golem.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false, false));
	   golem.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
	
	   // BossBar
	   BossBar bar = Bukkit.createBossBar("§7Golgy", BarColor.WHITE, BarStyle.SEGMENTED_20);
	   bar.setProgress(1.0);
	   bossBars.put(golem.getUniqueId(), bar);
	   bosses.put(golem.getUniqueId(), golem);
	
	   // Metadata
	   golem.setMetadata("lastQuarter", new FixedMetadataValue(plugin, 4));
	
	   // Messages
	   Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §cUn Boss est apparu : §7Golgy §c!");
	   player.sendMessage("§6[§eLifesteal SMP Metz§6] §aLe boss a été invoqué avec succès !");
	   
	   // Tâche planifiée : Lévitation toutes les 20s
	   Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
	       @Override
	       public void run() {
	    	   for (Player player : golem.getWorld().getPlayers()) {
	               if (player.getGameMode() != GameMode.SURVIVAL) continue;
		           if (golem.isValid()) {
		               for (Entity entity : golem.getNearbyEntities(10, 10, 10)) {
		                   if (entity instanceof Player) {
		                       Player target = (Player) entity;
		                       target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 1));
		                   }
		               }
		               golem.getWorld().playSound(golem.getLocation(),
		                                          Sound.BLOCK_BEACON_POWER_SELECT,
		                                          1f, 1f);
		           }
	    	   }
	       }
	   }, 0L, 400L); // 400 ticks = 20 secondes

	   // Attaque de boule de feu
	   startFireballAttack(golem);
	
	   return true;
	}
	 
    // ==========================================
    // ATTAQUE BOULE DE FEU
    // ==========================================
    private void startFireballAttack(Golem golem) {

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (golem.isDead() || !golem.isValid()) return;
            
            Player target = getNearestPlayer(golem, 25);

            if (target == null || target.getGameMode() != GameMode.SURVIVAL) return;

            golem.getWorld().playSound(golem.getLocation(),
                    Sound.ENTITY_ENDER_DRAGON_SHOOT, 2f, 0.6f);
            
            launchFireball(golem, target);

        }, 100L, 20L * 8); // toutes les 8 secondes

        fireballTasks.put(golem.getUniqueId(), task);
    }

    private Player getNearestPlayer(Golem golem, double radius) {
        Player nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Player p : golem.getWorld().getPlayers()) {
            double dist = p.getLocation().distanceSquared(golem.getLocation());
            if (dist <= radius * radius && dist < minDist) {
                minDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    private void launchFireball(Golem golem, Player target) {

        Location start = golem.getLocation().add(0, 2.5, 0);
        Vector direction = target.getLocation().add(0, 1, 0)
                .toVector()
                .subtract(start.toVector())
                .normalize();

        Fireball fireball = golem.getWorld().spawn(start, Fireball.class);
        fireball.setShooter(golem);
        fireball.setDirection(direction);
        fireball.setYield(0);
        fireball.setIsIncendiary(false);
    }

    // ==========================================
    // DÉGÂTS + BOSSBAR + VAGUES
    // ==========================================
    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Golem golem)) return;
        if (!golem.getScoreboardTags().contains("boss")) return;

        event.setDamage(event.getDamage() * 0.01);

        BossBar bar = bossBars.get(golem.getUniqueId());
        if (bar == null) return;

        double maxHp = golem.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newHp = Math.max(0, golem.getHealth() - event.getFinalDamage());
        bar.setProgress(Math.max(0.0, newHp / maxHp));

        int quarter = (int) (newHp / (maxHp / 4));
        int lastQuarter = golem.getMetadata("lastQuarter").get(0).asInt();

        if (quarter < lastQuarter) {
            spawnCopperWave(golem);
            golem.setMetadata("lastQuarter", new FixedMetadataValue(plugin, quarter));
        }
    }

    // ==========================================
    // FIREBALL → BRÛLURE JOUEUR
    // ==========================================
    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Fireball fireball)) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(fireball.getShooter() instanceof Golem golem)) return;
        if (!golem.getScoreboardTags().contains("boss")) return;

        event.setCancelled(true);
        player.setFireTicks(20 * 6);
    }

    // ==========================================
    // BLOQUER DESTRUCTION BLOCS
    // ==========================================
    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (event.getEntity() instanceof Fireball) {
            event.blockList().clear();
        }
    }

    // ==========================================
    // MORT DU BOSS
    // ==========================================
    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Golem golem)) return;
        if (!golem.getScoreboardTags().contains("boss")) return;

        BossBar bar = bossBars.remove(golem.getUniqueId());
        if (bar != null) bar.removeAll();

        BukkitTask task = fireballTasks.remove(golem.getUniqueId());
        if (task != null) task.cancel();

        bosses.remove(golem.getUniqueId());
        event.getDrops().clear();

        Player killer = golem.getKiller();
        if (killer != null) {
            Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §7Golgy §4a été vaincu par §c" + killer.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "money give " + killer.getName() + " 10000");
        }
    }

    // ===============================
    // MISE À JOUR BOSSBAR SELON DISTANCE
    // ===============================
    public void startBossBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, BossBar>> it = bossBars.entrySet().iterator();
                
                while (it.hasNext()) {
                    Map.Entry<UUID, BossBar> entry = it.next();
                    UUID uuid = entry.getKey();
                    BossBar bar = entry.getValue();
                    Entity entity = Bukkit.getEntity(uuid);

                    // 1. SI LE BOSS N'EXISTE PLUS : ON SUPPRIME TOUT
                    if (entity == null || !entity.isValid() || entity.isDead()) {
                        bar.removeAll(); // Retire la barre de l'écran de TOUS les joueurs
                        it.remove();     // Supprime de la mémoire
                        continue;
                    }

                    // 2. MISE À JOUR DE LA VIE
                    if (entity instanceof LivingEntity) {
                        LivingEntity boss = (LivingEntity) entity;
                        bar.setProgress(Math.max(0.0, Math.min(1.0, boss.getHealth() / 500.0)));
                    }

                    // 3. GESTION DE LA DISTANCE (PROXIMITÉ)
                    double rangeSquared = BOSSBAR_RANGE * BOSSBAR_RANGE; // 50 * 50
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        
                        // Si le joueur est dans le même monde et à moins de 50 blocs
                        if (player.getWorld().equals(entity.getWorld()) && 
                            player.getLocation().distanceSquared(entity.getLocation()) <= rangeSquared) {
                            
                            if (!bar.getPlayers().contains(player)) {
                                bar.addPlayer(player); // Affiche la barre
                            }
                        } else {
                            // Trop loin ou pas le bon monde : on cache la barre
                            bar.removePlayer(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Mise à jour 2 fois par seconde
    }
    
    // ===============================
    // SPAWN VAGUE DE COPPER GOLEM
    // ===============================
    private void spawnCopperWave(Golem golem) {
        Location loc = golem.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < 6; i++) {
            double x = loc.getX() + (Math.random() * 6 - 3);
            double z = loc.getZ() + (Math.random() * 6 - 3);

            CopperGolem golemC = (CopperGolem) world.spawnEntity(
                    new Location(world, x, loc.getY(), z),
                    EntityType.COPPER_GOLEM
            );

            // Stats
            golemC.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            golemC.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
            golemC.setHealth(40.0);

            // Tag kamikaze
            golemC.addScoreboardTag("kamikaze");
            startCopperChase(golemC);
            startCopperAttack(golemC);

            Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §cUne vague de §6Copper Golem Kamikaze §cvient au secours de §7Golgy §c!");
        }
    }
    
	 // ===============================
	 // ATTAQUE PROXIMITÉ DES COPPER GOLEMS
	 // ===============================
    private void startCopperAttack(CopperGolem golemC) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (golemC.isDead() || !golemC.isValid()) {
                    cancel();
                    return;
                }

                for (Player player : golemC.getWorld().getPlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL) continue;

                    double dist = player.getLocation().distance(golemC.getLocation());
                    if (dist <= 2) {
                        // Inflige 1/2 cœur
                        player.damage(0.5, golemC);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // toutes les 10 ticks (0.5s)
    }
    
	 // ===============================
	 // COPPER GOLEM POURSUITE JOUEUR
	 // ===============================
    private void startCopperChase(CopperGolem golemC) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (golemC.isDead() || !golemC.isValid()) {
                    cancel();
                    return;
                }

                Player target = null;
                double minDist = Double.MAX_VALUE;

                for (Player p : golemC.getWorld().getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL) continue;

                    double dist = p.getLocation().distanceSquared(golemC.getLocation());
                    if (dist < 30 * 30 && dist < minDist) {
                        minDist = dist;
                        target = p;
                    }
                }

                if (target == null) return;

                Location gl = golemC.getLocation();
                Location pl = target.getLocation();

                // Direction horizontale
                org.bukkit.util.Vector direction = pl.toVector().subtract(gl.toVector());
                direction.setY(0);
                if (direction.lengthSquared() == 0) return;

                // Rotation approximative
                float yaw = (float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) - 90;
                gl.setYaw(yaw);
                gl.setPitch(0);
                golemC.teleport(gl); // rotation appliquée

                // Vélocité un peu plus forte pour qu’il avance vraiment
                org.bukkit.util.Vector velocity = direction.normalize().multiply(0.25);
                golemC.setVelocity(velocity);
            }
        }.runTaskTimer(plugin, 0L, 1L); // tick plus rapide = meilleure poursuite
    }



    
    // ================================
    // EXPLOSION COPPER GOLEM À LA MORT
    // ================================
    
    @EventHandler
    public void onCopperGolemDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof CopperGolem golemC)) return;
        if (!golemC.getScoreboardTags().contains("kamikaze")) return;
        
        Player killer = golemC.getKiller();

        Location loc = golemC.getLocation();
        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 1.5f, false, false);
        
        event.getDrops().clear();
        
        String coins = "coins give " + killer.getName() + " 10";
        killer.sendMessage("\"§6[§eLifesteal SMP Metz§6] §aBien joué tient voilà §210 coins §a!");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), coins);
    }

    // ===============================
    // POTIONS DU BOSS
    // ===============================
    public void startBossPotionAttacks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Golem golem : bosses.values()) {
                    if (golem == null || golem.isDead()) continue;

                    Location bossLoc = golem.getLocation();
                    for (Player player : golem.getWorld().getPlayers()) {
                        if (player.getLocation().distance(bossLoc) <= 10) {
                            Random random = new Random();
                            int choice = random.nextInt(3);

                            switch (choice) {
                                case 0 -> {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 1));
                                }
                                case 1 -> {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15 * 20, 1));
                                }
                                case 2 -> {
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 10 * 25, 3));
                                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 10 * 25, 1));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 15); // toutes les 15s
    }
}

