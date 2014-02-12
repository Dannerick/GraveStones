package gravestone;

import gravestone.bones.BlockBones;
import gravestone.bones.ItemSkulls;
import gravestone.bones.TEBones;
import gravestone.grave.BlockGrave;
import gravestone.grave.ItemGrave;
import gravestone.grave.te.TEGrave;
import gravestone.handelers.CommandPanel;
import gravestone.handelers.CommonProxy;
import gravestone.handelers.DeathEvent;
import gravestone.handelers.GuiHandler;
import gravestone.handelers.PacketHandler;
import gravestone.handelers.PlayerTracker;

import java.util.Random;
import java.util.logging.Level;

import modUpdateChecked.OnPlayerLogin;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.CommandHandler;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;


@Mod(modid = ModInfo.ID, name = ModInfo.NAME, version = ModInfo.VERSION)
@NetworkMod(clientSideRequired = true, serverSideRequired = false,
clientPacketHandlerSpec =
@SidedPacketHandler(channels = {ModInfo.ID}, packetHandler = PacketHandler.class),
serverPacketHandlerSpec =
@SidedPacketHandler(channels = {ModInfo.ID}, packetHandler = PacketHandler.class))

public class mod_Gravestone{
	public static mod_Gravestone instance;
	public ItemStack[] stack;
	public int renderID = RenderingRegistry.getNextAvailableRenderId();
	Random rand = new Random();
	public static Block gravestone;
	public static Block bones;
	public static Item graveItem;
	public static Item bonesItem;
	@SidedProxy(serverSide = "gravestone.handelers.CommonProxy", clientSide = "gravestone.handelers.ClientProxy")
	public static CommonProxy proxy;

	public mod_Gravestone() {
		instance = this;
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) 
	{
		ConfigClass.instance.loadConfig(event.getSuggestedConfigurationFile());
		LogHelper.init();
	}
	@EventHandler
	public void load(FMLInitializationEvent event)
	{
		LogHelper.log(Level.INFO, "Starting item and block initialization");
		MinecraftForge.EVENT_BUS.register(new DeathEvent());
		GameRegistry.registerPlayerTracker(new OnPlayerLogin(ModInfo.VERSION, ModInfo.NAME, ConfigClass.instance.checkUpdates));
		
		gravestone = new BlockGrave(ConfigClass.instance.graveBlock).setHardness(10).setResistance(6000000.0F).setUnlocalizedName("GraveStone");
		bones = new BlockBones(ConfigClass.instance.bonesBlock, Material.ground).setHardness(2f).setUnlocalizedName("Bones");
		graveItem = new ItemGrave(ConfigClass.instance.grave).setUnlocalizedName("graveItem").setCreativeTab(CreativeTabs.tabDecorations);
		bonesItem = new ItemSkulls(ConfigClass.instance.bones).setUnlocalizedName("bonesItem").setCreativeTab(CreativeTabs.tabDecorations);

		GameRegistry.registerBlock(gravestone,"GraveStone");
		LanguageRegistry.addName(gravestone, "GraveStone");
		GameRegistry.registerBlock(bones, "Bones");
		LanguageRegistry.addName(bones, "Bones");
		LanguageRegistry.addName(graveItem, "Grave");
		LanguageRegistry.addName(bonesItem, "Bone and Skull");

		GameRegistry.addRecipe(new ItemStack(graveItem,1),new Object[] {"BBB", " B ", " B ", 'B',Block.cobblestone });
		GameRegistry.addRecipe(new ItemStack(bonesItem,1),new Object[] {"BBB", " B ", " B ", 'B',Item.bone });

		GameRegistry.registerTileEntity(TEGrave.class, "grave");
		GameRegistry.registerTileEntity(TEBones.class, "playerbody");
		GameRegistry.registerPlayerTracker(new PlayerTracker());
		
		NetworkRegistry.instance().registerGuiHandler(this, new GuiHandler());

		proxy.registerRender();
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		CommandHandler commandManager = (CommandHandler) event.getServer().getCommandManager();
		commandManager.registerCommand(new CommandPanel());
	}

	public void buildGravestone(EntityPlayer player, InventoryPlayer inv ) {

		int x = MathHelper.floor_double(player.posX),
			y = MathHelper.floor_double(player.posY),
			z = MathHelper.floor_double(player.posZ);
		int x_off, z_off, dist_off;
		try
		{
			if(player.worldObj.isAirBlock(x, y, z)){
				while(y >= 0 && player.worldObj.isAirBlock(x, y, z)){
					y--;
				}
				if(y < 0){
					throw new NoGraveException("Death not above solid ground");
				}
			}
			
			int scanTime = 0,
				grave_y = checkGraveLocation(player, x, y, z, scanTime);
			while(scanTime < ConfigClass.instance.maxPlaceAttempts && grave_y == 0){
				scanTime++;
				grave_y = checkGraveLocation(player, x, y, z, scanTime);
			}
			if (scanTime == ConfigClass.instance.maxPlaceAttempts) {
				throw new NoGraveException("No position located after " + 
					ConfigClass.instance.maxPlaceAttempts + " attempts");
			}
			y = grave_y + 1;
			x_off = (scanTime & 1) * 2 - 1;
			z_off = ((scanTime >>> 1) & 1) * 2 - 1;
			dist_off = scanTime >>> 2;
		}
		catch (NoGraveException ex)
		{
			LogHelper.log(Level.INFO, "No grave made for " + player.getDisplayName() + ": " + ex.getMessage());
			return;
		}
		
		// set location
		x = x + x_off * dist_off;
		z = z + z_off * dist_off;

		// place grave
		player.worldObj.setBlock(x, y, z, gravestone.blockID);
		TileEntity te = player.worldObj.getBlockTileEntity(x, y, z);
		LogHelper.log(Level.INFO, String.format("Grave placed for %s at (%d, %d, %d)", player.getDisplayName(), x, y, z));
		
		// check for bones placement
		int c = rand.nextInt(100);
		if(ConfigClass.instance.bonesPlacementChance > 0 && c < ConfigClass.instance.bonesPlacementChance)
		{
			player.worldObj.setBlock(x, y-2, z, bones.blockID);
			player.worldObj.setBlockTileEntity(x, y-2, z,new TEBones());
		}

		placeGrave(te, player, inv);
	}
	
	private int checkGraveLocation(EntityPlayer player, int start_x, int start_y, int start_z, int count)
	{
		int x_off = (count & 1) * 2 - 1, 
			z_off = ((count >>> 1) & 1) * 2 - 1,
			dist_off = count >>> 2;
		int x = start_x + x_off * dist_off,
			z = start_z + z_off * dist_off,
			y = start_y;

		while(y > 0 && (player.worldObj.isAirBlock(x, y, z) || (player.worldObj.getBlockMaterial(x, y, z).isReplaceable() && !player.worldObj.getBlockMaterial(x,  y,  z).isLiquid() )) ){
			y--;
		}
		while(y < ConfigClass.instance.maxGraveHeight && !(player.worldObj.isAirBlock(x, y + 1, z) || (player.worldObj.getBlockMaterial(x, y, z).isReplaceable() && !player.worldObj.getBlockMaterial(x,  y,  z).isLiquid() )) ){
			y++;
		}
		if (y < 2 || y >= ConfigClass.instance.maxGraveHeight)
		{
			return 0;
		}

		if (player.worldObj.getBlockMaterial(x, y, z).isSolid() && player.worldObj.getBlockMaterial(x, y - 1, z).isSolid())
		{
			if (player.worldObj.getBlockTileEntity(x, y, z) == null && 
				player.worldObj.getBlockTileEntity(x, y - 1, z) == null && 
				player.worldObj.getBlockTileEntity(x, y + 1, z) == null) {
					return y;
			}
		}
		return 0;
	}
	
	private void placeGrave(TileEntity te , EntityPlayer player, InventoryPlayer inv){
		if(te != null) {
			try {
				TEGrave tegrave = (TEGrave)te;
				tegrave.setName(player.username);
				tegrave.setMeta(proxy.getRenderID(player.username));
				tegrave.setPlayer(player);
				if(stack != null)
					tegrave.setItems(stack);

				for(int id = 0; id <inv.getSizeInventory(); id++)
				{
					ItemStack is = inv.getStackInSlot(id);
					if(is != null && id < tegrave.getSizeInventory())
					{
						tegrave.setInventorySlotContents(id, is);
						inv.setInventorySlotContents(id, null);
					}

				}
			}
			catch(Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
}