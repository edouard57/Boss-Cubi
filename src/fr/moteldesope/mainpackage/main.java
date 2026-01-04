package fr.moteldesope.mainpackage;

import org.bukkit.plugin.java.JavaPlugin;
import fr.moteldesope.commands.*;

public class main extends JavaPlugin {
	@Override
	public void onEnable() {
		
		getCommand("boss").setExecutor(new aide());
		
	    getCommand("summonBoss").setExecutor(new summon_and_gui());
	    getServer().getPluginManager().registerEvents(new summon_and_gui(), this);
	    getLogger().info("Plugin allumé !");
	    
	    
	    //Summon Zominel
	    fr.moteldesope.zominel.summon boss = new fr.moteldesope.zominel.summon(this);

	    getCommand("summonZominel").setExecutor(boss);
	    getServer().getPluginManager().registerEvents(boss, this);

	    boss.startBossBarUpdater();

	    //Summon Golgy
	    fr.moteldesope.golem.summon golgy = new fr.moteldesope.golem.summon(this);

	    getCommand("summonGolgy").setExecutor(golgy);
	    getServer().getPluginManager().registerEvents(golgy, this);

	    golgy.startBossBarUpdater();
	    
	    // Autres
	    getCommand("getUltimateSword").setExecutor(new weapons());
	    getCommand("removeBoss").setExecutor(new removeBoss());
	}
	
	@Override
	public void onDisable() {
		getLogger().info("Plugin éteint !");
	}
}

