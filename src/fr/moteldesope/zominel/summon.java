package fr.moteldesope.zominel;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class summon implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Giant> bosses = new HashMap<>();

    private static final double BOSSBAR_RANGE = 50.0;

    public summon(JavaPlugin plugin) {
        this.plugin = plugin;
        startBossBarUpdater();
        startBossPotionAttacks();
        startSilverfishWaves();
    }

    // ===============================
    // COMMANDE /summonZominel
    // ===============================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (!cmd.getName().equalsIgnoreCase("summonZominel")) return false;

        Location loc = player.getLocation();

        // Création du boss directement
        Giant giant = (Giant) loc.getWorld().spawnEntity(loc, EntityType.GIANT);
        giant.setAI(false); // Remplace NoAI
        giant.getAttribute(Attribute.MAX_HEALTH).setBaseValue(500);
        giant.setHealth(500);
        giant.addScoreboardTag("boss");

        // BossBar
        BossBar bar = Bukkit.createBossBar(
                "§4Le Père Zominel",
                BarColor.GREEN,
                BarStyle.SEGMENTED_20
        );
        bar.setProgress(1.0);

        bossBars.put(giant.getUniqueId(), bar);
        bosses.put(giant.getUniqueId(), giant);

        // Initialiser metadata pour les vagues
        giant.setMetadata("lastQuarter", new FixedMetadataValue(plugin, 4));

        Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §cUn Boss est apparu : §4Le Père Zominel §c!");
        player.sendMessage("§6[§eLifesteal SMP Metz§6] §aLe boss a été invoqué avec succès !");

        return true;
    }

    // ===============================
    // DÉGÂTS + BOSSBAR + VAGUES DE ZOMBIES
    // ===============================
    @EventHandler
    public void onBossDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Giant giant)) return;
        if (!giant.getScoreboardTags().contains("boss")) return;

        event.setDamage(event.getDamage() * 0.01); // Réduit les dégâts à 1%

        BossBar bar = bossBars.get(giant.getUniqueId());
        if (bar == null) return;

        double maxHp = giant.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newHp = Math.max(0, giant.getHealth() - event.getFinalDamage());

        bar.setProgress(Math.max(0.0, newHp / maxHp));

        int quarter = (int) (newHp / (maxHp / 4));
        int lastQuarter = giant.getMetadata("lastQuarter").get(0).asInt();

        if (quarter < lastQuarter) {
            spawnZombieWave(giant);
            giant.setMetadata("lastQuarter", new FixedMetadataValue(plugin, quarter));
        }
    }

    // ===============================
    // SUPPRESSION BOSSBAR ET BOSS
    // ===============================
    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Giant giant)) return;
        if (!giant.getScoreboardTags().contains("boss")) return;

        BossBar bar = bossBars.remove(giant.getUniqueId());
        bosses.remove(giant.getUniqueId());

        if (bar != null) bar.removeAll();

        Player killer = giant.getKiller();
        if (killer != null) {
            Bukkit.broadcastMessage(
                    "§6[§eLifesteal SMP Metz§6] §4Le Père Zominel a été vaincu par §c" + killer.getName() + " §4!"
            );
        } else {
            Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §4Le Père Zominel a été vaincu §c!");
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
    // SPAWN VAGUE DE ZOMBIES
    // ===============================
    private void spawnZombieWave(Giant giant) {
        Location loc = giant.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < 4; i++) {
            double x = loc.getX() + (Math.random() * 6 - 3);
            double z = loc.getZ() + (Math.random() * 6 - 3);

            Zombie zombie = (Zombie) world.spawnEntity(new Location(world, x, loc.getY(), z), EntityType.ZOMBIE);

            zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
            zombie.addPotionEffect(new PotionEffect(PotionEffectType.OOZING, Integer.MAX_VALUE, 0, false, false));

            zombie.getEquipment().setChestplate(createArmor(Material.NETHERITE_CHESTPLATE, 2));
            zombie.getEquipment().setHelmet(createArmor(Material.DIAMOND_HELMET, 1));
            zombie.getEquipment().setLeggings(createArmor(Material.DIAMOND_LEGGINGS, 1));
            zombie.getEquipment().setBoots(createArmor(Material.DIAMOND_BOOTS, 1));

            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 1, true);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.KNOCKBACK, 2, true);
                sword.setItemMeta(meta);
            }
            zombie.getEquipment().setItemInMainHand(sword);

            zombie.setRemoveWhenFarAway(false);
            zombie.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
            zombie.setHealth(40.0);
        }

        Bukkit.broadcastMessage("§6[§eLifesteal SMP Metz§6] §cUne vague de zombie vient au secours du §4Père Zominel §c!");
    }

    // ===============================
    // SPAWN VAGUE DE SILVERFISH
    // ===============================
    private void spawnSilverfishWave(Giant giant) {
        Location loc = giant.getLocation();
        World world = loc.getWorld();

        for (int i = 0; i < 5; i++) {
            double x = loc.getX() + (Math.random() * 12 - 6);
            double z = loc.getZ() + (Math.random() * 12 - 6);

            Silverfish sf = (Silverfish) world.spawnEntity(new Location(world, x, loc.getY(), z), EntityType.SILVERFISH);
            sf.addPotionEffect(new PotionEffect(PotionEffectType.WEAVING, Integer.MAX_VALUE, 1, false, false));
            sf.setMetadata("explosiveSilverfish", new FixedMetadataValue(plugin, true));
        }
    }

    // ===============================
    // EXPLOSION SILVERFISH À LA MORT
    // ===============================
    @EventHandler
    public void onSilverfishDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Silverfish sf)) return;
        if (!sf.hasMetadata("explosiveSilverfish")) return;

        Location l = sf.getLocation();
        l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 2.5f, false, false);
    }

    // ===============================
    // VAGUES SILVERFISH AUTOMATIQUES
    // ===============================
    public void startSilverfishWaves() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Giant giant : bosses.values()) {
                    if (giant == null || giant.isDead()) continue;
                    spawnSilverfishWave(giant);
                }
            }
        }.runTaskTimer(plugin, 20L * 50, 20L * 50); // toutes les 50s
    }

    // ===============================
    // POTIONS DU BOSS
    // ===============================
    public void startBossPotionAttacks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Giant giant : bosses.values()) {
                    if (giant == null || giant.isDead()) continue;

                    Location bossLoc = giant.getLocation();
                    for (Player player : giant.getWorld().getPlayers()) {
                        if (player.getLocation().distance(bossLoc) <= 10) {
                            Random random = new Random();
                            int choice = random.nextInt(3);

                            switch (choice) {
                                case 0 -> player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 30, 2));
                                case 1 -> player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * (10 + random.nextInt(6)), 1));
                                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * (8 + random.nextInt(6)), 2));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 15); // toutes les 15s
    }

    // ===============================
    // MÉTHODE UTILITAIRE ARMURE
    // ===============================
    private ItemStack createArmor(Material material, int protectionLevel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.PROTECTION, protectionLevel, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}





