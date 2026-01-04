package fr.moteldesope.commands;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class summon_and_gui implements CommandExecutor, org.bukkit.event.Listener {

    private ItemStack getCompass() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();

        meta.setDisplayName("§cOutil d'apparition");
        meta.setLore(Arrays.asList(
                "§eClique droit pour ouvrir"
        ));

        clock.setItemMeta(meta);
        return clock;
    }

    // Commande /summonBoss
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
        	Player player = (Player) sender;
            player.getInventory().addItem(getCompass());
            player.sendMessage("§6[§eLifesteal SMP Metz§6] §aVous avez obtenu l'Outil d'apparition !");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (action == Action.RIGHT_CLICK_AIR) {
        	if (item.getType() == Material.CLOCK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§cOutil d'apparition")) {
        		p.sendMessage("§6[§eLifesteal SMP Metz§6] §aOuverture du menu...");
        		
        		Inventory inv = Bukkit.createInventory(null, 27, "§cOutil d'apparition");
        		
        		//+======================================================================+
        		
        		ItemStack Zominel = new ItemStack(Material.ZOMBIE_HEAD);
                ItemMeta ZominelM = Zominel.getItemMeta();
        		
        		ZominelM.setDisplayName("§2Le Père Zominel §c(l'original)");
        		ZominelM.setLore(Arrays.asList(
                        "§eClique gauche pour faire apparaitre",
                        "",
        				"§cNécessite pack de Noël Lifesteal SMP Metz Saison 2"
                ));
        		Zominel.setItemMeta(ZominelM);
        		inv.setItem(10, Zominel);
        		
        		//+======================================================================+
        		
        		ItemStack Golgy = new ItemStack(Material.IRON_GOLEM_SPAWN_EGG);
        		ItemMeta GolgyM = Golgy.getItemMeta();
        		
        		GolgyM.setDisplayName("§7Golgy");
        		GolgyM.setLore(Arrays.asList(
                        "§eClique gauche pour faire apparaitre",
                        "",
        				"§cPack en cours de dev (il y en aura un peut être)"
                ));
        		Golgy.setItemMeta(GolgyM);
        		inv.setItem(12, Golgy);
        		
        		//+======================================================================+
        		
        		p.openInventory(inv);
        	}
        }
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();

        if (event.getView().getTitle().equalsIgnoreCase("§cOutil d'apparition")) {
            event.setCancelled(true); // Empêche tout mouvement d'item dans cet inventaire

            if (current == null || !current.hasItemMeta()) return;

            if (current.getType() == Material.ZOMBIE_HEAD &&
                current.getItemMeta().getDisplayName().equals("§2Le Père Zominel §c(l'original)")) {
                p.closeInventory();
                p.performCommand("summonzominel");
            }
            if (current.getType() == Material.IRON_GOLEM_SPAWN_EGG &&
                    current.getItemMeta().getDisplayName().equals("§7Golgy")) {
                    p.closeInventory();
                    p.performCommand("summongolgy");
            }
        }
    }

    // Pour bloquer le drag (glisser-déposer)
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase("§cOutil d'apparition")) {
            event.setCancelled(true);
        }
    }
}
