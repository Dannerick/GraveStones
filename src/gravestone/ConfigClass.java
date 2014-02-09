package gravestone;

import java.io.File;

import net.minecraftforge.common.Configuration;

public class ConfigClass {
	private static final int MAXPLACEMENTATTEMPTS = 1600;
	private static final int MAXGRAVEHEIGHT = 512;
	
    public static ConfigClass instance = new ConfigClass();


    public int grave;
    public int bones;
    public int graveBlock;
    public int bonesBlock;

    public boolean onDeathEvent = true;
    public boolean displayGraveBecon = true;
    public int maxPlaceAttempts;
    public int maxGraveHeight;
    public int bonesPlacementChance;
    
    public boolean checkUpdates = false;
    
    private ConfigClass() {}

    public void loadConfig(File file) {
        Configuration config = new Configuration(file);
        config.load();
        loadSettings(config);
        config.save();
    }
    private void loadSettings(Configuration config){

        grave = config.getItem("Grave Item Id", 5000).getInt(5000);
        bones = config.getItem("Bones Item Id", 5001).getInt(5001);
        
        graveBlock = config.getBlock("Grave Block Id", 500).getInt(500);
        bonesBlock = config.getBlock("Bones Block Id", 501).getInt(501);
        
        config.addCustomCategoryComment("Grave", "Setting for determining the grave placement and display.");
        onDeathEvent = config.get("Grave", "Spawn Grave on Death", true).getBoolean(true);
        displayGraveBecon = config.get("Grave", "Display grave beacon", true).getBoolean(true);
        maxPlaceAttempts = config.get("Grave", "Max Grave place attempts", 200).getInt(200);
        maxGraveHeight = config.get("Grave", "Max Grave Height", 256).getInt(256);
        bonesPlacementChance = config.get("Grave", "Bones placement chance", 50).getInt(50);
        // do sanity checks on config values
        maxPlaceAttempts = Math.max(Math.min(maxPlaceAttempts, MAXPLACEMENTATTEMPTS), 10);
        maxGraveHeight = Math.max(Math.min(maxGraveHeight, MAXGRAVEHEIGHT), 2);
        bonesPlacementChance = Math.max(Math.min(bonesPlacementChance, 100), 0);
        
        checkUpdates = config.get(config.CATEGORY_GENERAL, "Check for updates", false).getBoolean(false);
    }
}
