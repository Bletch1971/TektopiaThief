package bletch.tektopiathief.entities;

import java.util.ListIterator;
import java.util.Set;
import com.leviathanstudio.craftstudio.client.animation.ClientAnimationHandler;
import com.leviathanstudio.craftstudio.common.animation.AnimationHandler;

import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.core.ModDetails;
import bletch.tektopiathief.core.ModEntities;
import bletch.tektopiathief.entities.ai.EntityAIEscapeVillage;
import bletch.tektopiathief.entities.ai.EntityAIFleeEntity;
import bletch.tektopiathief.entities.ai.EntityAIIdleCheck;
import bletch.tektopiathief.entities.ai.EntityAILeaveVillage;
import bletch.tektopiathief.entities.ai.EntityAIRetrieveFromStorage;
import bletch.tektopiathief.entities.ai.EntityAIUseGate;
import bletch.tektopiathief.storage.ItemDesire;
import bletch.tektopiathief.storage.ItemDesireSet;
import bletch.tektopiathief.storage.ModInventory;
import bletch.tektopiathief.entities.ai.EntityAIUseDoor;
import bletch.tektopiathief.utils.LoggerUtils;
import bletch.tektopiathief.utils.TextUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.monster.IMob;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.tangotek.tektopia.TekVillager;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillagerRole;
import net.tangotek.tektopia.entities.EntityVillageNavigator;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.tickjob.TickJob;

public class EntityThief extends EntityVillageNavigator implements IMob {

	public static final String ENTITY_NAME = "thief";
	public static final String MODEL_NAME = "thief";
	public static final String RESOURCE_PATH = "thief";
	public static final String ANIMATION_MODEL_NAME = MODEL_NAME + "_m";

	public static final Integer MIN_LEVEL = 1;
	public static final Integer MAX_LEVEL = 5;

	public static long WORK_START_TIME = 13000L; // 19:00 (7:00 PM)
	public static long WORK_END_TIME = 23500L; // 05:30 (5:30 AM)

	protected static final AnimationHandler<EntityThief> animationHandler;
	protected static final DataParameter<String> ANIMATION_KEY;
	protected static final DataParameter<ItemStack> ACTION_ITEM;
	protected static final DataParameter<Integer> LEVEL;
	protected static final DataParameter<Byte> MOVEMENT_MODE;
	protected static final DataParameter<Boolean> SEEN;

	private BlockPos firstCheck;
	private MovementMode lastMovementMode;
	private int idle;
	protected ModInventory inventory;
	protected ItemDesireSet desireSet;

	public EntityThief(World worldIn) {
		super(worldIn, VillagerRole.ENEMY.value | VillagerRole.VISITOR.value);

		this.idle = 0;
        this.inventory = new ModInventory(this, "Items", false, 1);
        
		this.setSize(0.6F, 1.95F);
		this.setRotation(0.0F, 0.0F);
	}

	public EntityThief(World worldIn, int level) {
		this(worldIn);

		this.setLevel(level);
	}

	protected void addTask(int priority, EntityAIBase task) {
		this.tasks.addTask(priority, task);
	}
	
	protected void attachToVillage(Village village) {
		super.attachToVillage(village);
		
		LoggerUtils.info("Attaching to village", true);
	}
	
	public boolean attackEntityFrom(DamageSource source, float amount) {
		float beforeHealth = this.getHealth();

		if (super.attackEntityFrom(source, amount)) {
			float afterHealth = this.getHealth();
			float actualDamage = beforeHealth - afterHealth;
			
			if (this.hasVillage() && actualDamage > 0.0F) {
				String message = TextUtils.translate("message.thief.damage", new Object[] { String.format("%.2f", actualDamage) });
				Entity damagedBy = source.getTrueSource();
				
				if (damagedBy != null) {
					message += " " + TextUtils.translate("message.thief.damagefrom", new Object[] { damagedBy.getDisplayName().getUnformattedText() });
					
					if (damagedBy instanceof EntityVillagerTek) {
						// only output the damage message if a villager is responsible for the damage
						this.village.sendChatMessage(message);
					}
				}
				
				LoggerUtils.info(message, true);
			}
			
			return true;
		} else {
			return false;
		}
	}

	protected void checkStuck() {
		if (this.hasVillage() && this.firstCheck.distanceSq(this.getPosition()) < 10.0D) {
			LoggerUtils.info("Killing self...failed to find a way to the village", true);
			this.setDead();
		}
	}

	protected void detachVillage() {
		super.detachVillage();
		
		LoggerUtils.info("Detaching from village", true);
	}
    
    protected void dropAllItems() {
		LoggerUtils.info("Dropping all items", true);
		
        IInventory inventory = (IInventory)this.getInventory();
        
        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack itemStack = inventory.getStackInSlot(slot);
            
            if (!itemStack.isEmpty()) {
        		LoggerUtils.info("Dropping item " + itemStack.getDisplayName() + " x " + itemStack.getCount(), true);
                this.entityDropItem(itemStack, 0.5f);
            }
        }
        
        inventory.clear();
    }

	public void equipActionItem(ItemStack actionItem) {
		this.dataManager.set(ACTION_ITEM, actionItem.copy());
	}

	public ItemStack getActionItem() {
		return (ItemStack)this.dataManager.get(ACTION_ITEM);
	}

	public float getAIMoveSpeed() {
		return (0.20F + this.getLevel() * 0.02F) * this.getMovementMode().speedMultiplier;
	}

	protected SoundEvent getAmbientSound() {
		return null;
	}

	public float getAvoidanceDistance() {
		return this.getAvoidanceDistanceBase() - this.getLevel();
	}

	public float getAvoidanceDistanceBase() {
		return 24.0F;
	}

	protected boolean getCanUseDoors() {
		return true;
	}
	
	public ItemDesireSet getDesireSet() {
		return this.desireSet;
	}

	public float getDetectionDistance() {
		float avoidDistance = this.getAvoidanceDistance() - 3.0F;

		int light = this.world.getLightFor(EnumSkyBlock.BLOCK, this.getPosition());
		// check for minecraft day time (6am - 7pm)
		if (Village.isTimeOfDay(this.world, 0, 13000)) {
			// light level is the maximum of Sky and block light
			light = Math.max(light, this.world.getLightFor(EnumSkyBlock.SKY, this.getPosition()));
		}
		// make sure light level is between 0 and 15
		float detectDistance = avoidDistance - 15 + light;
    	
		LoggerUtils.info("EntityThief - getDetectionDistance called; avoidDistance=" + avoidDistance + "; light=" + light + "; detection distance=" + detectDistance, true);

		return Math.max(0.0F, detectDistance);
	}

	public ITextComponent getDisplayName() {
		ITextComponent itextcomponent = new TextComponentTranslation("entity." + MODEL_NAME + ".name", new Object[0]);
		itextcomponent.getStyle().setHoverEvent(this.getHoverEvent());
		itextcomponent.getStyle().setInsertion(this.getCachedUniqueIdString());
		return itextcomponent;
	}

	public int getIdle() {
		return this.idle;
	}
	
	public ModInventory getInventory() {
		return this.inventory;
	}

	public int getLevel() {
		return ((Integer)this.dataManager.get(LEVEL)).intValue();
	}

	public MovementMode getMovementMode() {
		return MovementMode.valueOf(((Byte)this.dataManager.get(MOVEMENT_MODE)).byteValue());
	}

	public Boolean getSeen() {
		return ((Boolean)this.dataManager.get(SEEN));
	}

	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	public World getWorld() {
		return this.world;
	}
	
	public ItemStack getAquiredItem() {
		return this.inventory.getStackInSlot(0);
	}
	
	public Boolean hasAcquiredItem() {
		return !this.inventory.hasSlotFree();
	}

	protected void initEntityAI() {
		setupDesires();
		setupAITasks();
	}

	public com.google.common.base.Predicate<Entity> isEnemy() {
		return (e) -> this.isHostile().test(e);
	}

	public boolean isFleeFrom(Entity e) {
		return this.isHostile().test(e) || e instanceof EntityVillagerTek && ((EntityVillagerTek)e).isRole(VillagerRole.VILLAGER);
	}

	public com.google.common.base.Predicate<Entity> isHostile() {
		return (e) -> e instanceof EntityVillagerTek && ((EntityVillagerTek)e).isRole(VillagerRole.DEFENDER);
	}

	public boolean isMale() {
		return this.getUniqueID().getLeastSignificantBits() % 2L == 0L;
	}	
	
    public boolean isStoragePriority() {
        return this.hasVillage() && this.isWorkTime() && !this.getSeen() && !this.hasAcquiredItem();
    }

	public boolean isWorkTime() {
		return isWorkTime(this.world, 0) && !this.world.isRaining();
	}
    
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        if (!this.world.isRemote) {
        	String message = TextUtils.translate("message.thief.killed", new Object[0]);
        	Entity damagedBy = cause.getTrueSource();
        	
        	if (damagedBy != null) {
        		message += " " + TextUtils.translate("message.thief.killedby", new Object[] { damagedBy.getDisplayName().getUnformattedComponentText() });
        		
        		if (damagedBy instanceof EntityVillagerTek) {
            		// only show the message if killed by a villager
        			this.village.sendChatMessage(message);
        		}
        	}
        	
			LoggerUtils.info(message, true);
			
            this.dropAllItems();
        }
    }

	public void onInventoryUpdated(ItemStack updatedItem) {
		this.desireSet.onInventoryUpdated(this, updatedItem);
	}

	public void onStorageChange(ItemStack storageItem) {
		this.desireSet.onStorageUpdated(this, storageItem);
	}

	public void onUpdate() {
		super.onUpdate();

		if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
			LoggerUtils.info("Killing self...difficulty is peaceful", true);
			this.setDead();
		}
	}

	private void prepStuck() {
		this.firstCheck = this.getPosition();
	}

	protected void scanForEnemies() {
		if (!this.hasVillage())
			return;
		
		float detectionDistance = this.getDetectionDistance();
		if (detectionDistance == 0)
			return;
		
		AxisAlignedBB boundingBox = this.getEntityBoundingBox().grow(detectionDistance, 6.0F, detectionDistance);
		ListIterator<EntityLiving> entityList = this.world.getEntitiesWithinAABB(EntityLiving.class, boundingBox, this.isEnemy()).listIterator();

		while (entityList.hasNext()) {
			EntityLiving entity = (EntityLiving)entityList.next();
			
			if (entity.canEntityBeSeen(this)) {
				LoggerUtils.info("EntityThief - scanForEnemies called, seen by entity" 
						+ "; entity=" + entity.getName() 
						+ "; detection distance=" + detectionDistance 
						+ "; distance=" + entity.getDistance(this)
						, true);
				
				if (!this.getSeen()) {
					this.village.sendChatMessage(TextUtils.translate("message.thief.seen", new Object[0]));
				}
				
				this.village.addOrRenewEnemy(this, 1);
				this.setSeen(true);
			}
		}		
	}

	public void setIdle(int idle) {
		this.idle = idle;
	}

	public void setLevel(int level) {
		this.dataManager.set(LEVEL, Math.max(MIN_LEVEL, Math.min(level, MAX_LEVEL)));
	}

	public void setMovementMode(MovementMode mode) {
		LoggerUtils.info("EntityThief - setMovementMode called; mode=" + mode.name(), true);
		
		this.dataManager.set(MOVEMENT_MODE, mode.id);
	}

	public void setSeen(Boolean seen) {
		LoggerUtils.info("EntityThief - setSeen called; seen=" + seen, true);
		
		this.dataManager.set(SEEN, seen);
	}
	
	protected void setupAITasks() {
		this.addTask(0, new EntityAISwimming(this));

		this.addTask(1, new EntityAIFleeEntity(this, 
				(e) -> e.isWorkTime() && !e.getSeen() && !e.hasAcquiredItem(),
				(e) -> this.isFleeFrom(e)));

		this.addTask(15, new EntityAIUseDoor(this));

		this.addTask(15, new EntityAIUseGate(this));
		
		this.addTask(30, new EntityAILeaveVillage(this, 
				(e) -> !e.isWorkTime() && !e.getSeen() && !e.hasAcquiredItem(), 
				(e) -> e.getVillage().getEdgeNode(), 
				MovementMode.WALK, (Runnable)null, 
				() -> {
					LoggerUtils.info("Killing Self...left the village", true);
					this.setDead();
				}));
		
		this.addTask(40, new EntityAIEscapeVillage(this, 
				(e) -> e.getSeen() || e.hasAcquiredItem(), 
				(e) -> e.getVillage().getEdgeNode(),
				MovementMode.RUN, (Runnable)null, 
				() -> {
					if (this.hasAcquiredItem()) {
						ItemStack aquiredItem = this.getAquiredItem();
						
						if (aquiredItem != ItemStack.EMPTY) {
							String aquiredItemDescription = aquiredItem.getDisplayName() + " x " + aquiredItem.getCount();
							this.village.sendChatMessage(TextUtils.translate("message.thief.escaped", new Object[] { aquiredItemDescription }));
							
							LoggerUtils.info(TextUtils.translate("message.thief.escaped", new Object[] { aquiredItemDescription }), true);
						}
					}
					
					LoggerUtils.info("Killing Self...escaped the village", true);
					this.setDead();
				}));

		this.addTask(50, new EntityAIRetrieveFromStorage(this));

		this.addTask(150, new EntityAIIdleCheck(this));
	}
	
	protected void setupDesires() {
        this.desireSet = new ItemDesireSet();
        
        // CROPS
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BEETROOT, 5, e -> e.isMale())); 
        this.getDesireSet().addItemDesire(new ItemDesire(Items.CARROT, 5, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.POTATO, 5, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.WHEAT, 5, e -> e.getLevel() < 3));
        
        // FOOD
        this.getDesireSet().addItemDesire(new ItemDesire(Items.APPLE, 3, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BAKED_POTATO, 2, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BEEF, 1, e -> e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BEETROOT_SOUP, 1, e -> e.getLevel() > 2));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BREAD, 1, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.CAKE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.CHICKEN, 1, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COOKED_BEEF, 1, e -> e.getLevel() > 1 && e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COOKED_CHICKEN, 1, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COOKED_MUTTON, 1, e -> e.getLevel() > 1 && !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COOKED_PORKCHOP, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COOKIE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.EGG, 1, e -> e.getLevel() < 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.GOLDEN_APPLE, 1, e -> e.getLevel() > 3 && !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.GOLDEN_CARROT, 1, e -> e.getLevel() > 3 && e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.MILK_BUCKET, 1, e -> e.getLevel() > 2));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.MUTTON, 1, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.PORKCHOP, 1, e -> e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.PUMPKIN_PIE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.SUGAR, 1, null));
        
        // RESOURCES
        this.getDesireSet().addItemDesire(new ItemDesire(Items.COAL, 1, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Blocks.GOLD_ORE, 2, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.GOLD_INGOT, 2, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Blocks.IRON_ORE, 2, e -> e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_INGOT, 2, e -> e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Blocks.LOG, 3, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.REDSTONE, 5, e -> e.getLevel() > 2));
        this.getDesireSet().addItemDesire(new ItemDesire(Blocks.WOOL, 1, null));
        
        // TOOLS
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BOOK, 1, e -> !e.isMale()));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.BUCKET, 1, null));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.PAPER, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.SHEARS, 1, e -> e.isMale()));
        
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_AXE, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_HOE, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_PICKAXE, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_SHOVEL, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_AXE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_HOE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_PICKAXE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_SHOVEL, 1, e -> e.getLevel() > 3));
        
        // WEAPONS
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_SWORD, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_SWORD, 1, e -> e.getLevel() > 3));
        
        // ARMOR
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_HELMET, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_CHESTPLATE, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_LEGGINGS, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.IRON_BOOTS, 1, e -> e.getLevel() > 1));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_HELMET, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_CHESTPLATE, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_LEGGINGS, 1, e -> e.getLevel() > 3));
        this.getDesireSet().addItemDesire(new ItemDesire(Items.DIAMOND_BOOTS, 1, e -> e.getLevel() > 3));
	}

	protected void setupServerJobs() {
		super.setupServerJobs();

		this.addJob(new TickJob(100, 0, false, 
				() -> this.prepStuck()));

		this.addJob(new TickJob(400, 0, false, 
				() -> this.checkStuck()));

		this.addJob(new TickJob(30, 30, true, 
				() -> this.scanForEnemies()));

		this.addJob(new TickJob(300, 100, true, 
				() -> {
					if (!this.hasVillage() || !this.getVillage().isValid()) {
						LoggerUtils.info("Killing self...no village", true);
						this.setDead();
					}
				}
		));
	}

	@SideOnly(Side.CLIENT)
	protected void startWalking() {
		MovementMode mode = this.getMovementMode();

		if (mode != this.lastMovementMode) {
			if (this.lastMovementMode != null) {
				this.stopWalking();
			}

			this.lastMovementMode = mode;
			if (mode != null) {
				this.playClientAnimation(mode.animation);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	protected void stopWalking() {
		if (this.lastMovementMode != null) {
			this.stopClientAnimation(this.lastMovementMode.animation);
			this.lastMovementMode = null;
		}
	}

	public void unequipActionItem() {
		this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
	}

	public void unequipActionItem(ItemStack actionItem) {
		if (actionItem != null && actionItem.getItem() == this.getActionItem().getItem()) {
			this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
		}
	}

	public void readEntityFromNBT(NBTTagCompound compound) {
		super.readEntityFromNBT(compound);

		if (compound.hasKey("level"))
			this.setLevel(compound.getInteger("level"));
		if (compound.hasKey("seen"))
			this.setSeen(compound.getBoolean("seen"));
		
        this.inventory.readNBT(compound);
        this.getDesireSet().forceUpdate();
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		super.writeEntityToNBT(compound);

		compound.setInteger("level", this.getLevel());
		compound.setBoolean("seen", this.getSeen());
		
        this.inventory.writeNBT(compound);
	}

	static {
		ANIMATION_KEY = EntityDataManager.createKey(EntityThief.class, DataSerializers.STRING);
        ACTION_ITEM = EntityDataManager.createKey(EntityThief.class, DataSerializers.ITEM_STACK);
		LEVEL = EntityDataManager.createKey(EntityThief.class, DataSerializers.VARINT);
		MOVEMENT_MODE = EntityDataManager.createKey(EntityThief.class, DataSerializers.BYTE);
		SEEN = EntityDataManager.createKey(EntityThief.class, DataSerializers.BOOLEAN);

		animationHandler = TekVillager.getNewAnimationHandler(EntityThief.class);
		setupCraftStudioAnimations(animationHandler, ANIMATION_MODEL_NAME);
	}

	public static boolean isWorkTime(World world, int sleepOffset) {
		return Village.isTimeOfDay(world, WORK_START_TIME, WORK_END_TIME, (long)sleepOffset);
	}

	protected static void setupCraftStudioAnimations(AnimationHandler<EntityThief> animationHandler, String modelName) {
		animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_WALK, modelName, true);
		animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_RUN, modelName, true);
		animationHandler.addAnim(ModDetails.MOD_ID, ModEntities.ANIMATION_VILLAGER_CREEP, modelName, true);
	}

	@Override
	public AnimationHandler<EntityThief> getAnimationHandler() {
		return animationHandler;
	}

	@Override
	public void playClientAnimation(String animationName) {
		if (!this.getAnimationHandler().isAnimationActive(ModDetails.MOD_ID, animationName, this)) {
			this.getAnimationHandler().startAnimation(ModDetails.MOD_ID, animationName, this);
		}
	}

	@Override
	public void stopClientAnimation(String animationName) {
		super.stopClientAnimation(animationName);

		if (this.getAnimationHandler().isAnimationActive(ModDetails.MOD_ID, animationName, this)) {
			this.getAnimationHandler().stopAnimation(ModDetails.MOD_ID, animationName, this);
		}
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0D);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(64.0D);
		this.dataManager.set(ANIMATION_KEY, "");
        this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
		this.dataManager.set(MOVEMENT_MODE, MovementMode.WALK.id);
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(ANIMATION_KEY, "");
        this.dataManager.register(ACTION_ITEM, ItemStack.EMPTY);
		this.dataManager.register(LEVEL, Integer.valueOf(1));
		this.dataManager.register(MOVEMENT_MODE, (byte)0);
		this.dataManager.register(SEEN, false);
		
		super.entityInit();
	}

	protected void updateClientAnimation(String animationName) {
		ClientAnimationHandler<EntityThief> clientAnimationHandler = (ClientAnimationHandler<EntityThief>)this.getAnimationHandler();

		Set<String> animChannels = clientAnimationHandler.getAnimChannels().keySet();
		animChannels.forEach(a -> clientAnimationHandler.stopAnimation(a, this));

		if (!animationName.isEmpty()) {
			clientAnimationHandler.startAnimation(ModDetails.MOD_ID, animationName, this);
		}
	}

	@Override
	public void notifyDataManagerChange(DataParameter<?> key) {
		super.notifyDataManagerChange(key);

		if (this.isWorldRemote() && ANIMATION_KEY.equals(key)) {
			this.updateClientAnimation(this.dataManager.get(ANIMATION_KEY));
		}

		if (MOVEMENT_MODE.equals(key) && this.isWalking()) {
			this.startWalking();
		}
	}   

	@Override
	public void stopServerAnimation(String animationName) {
		this.dataManager.set(ANIMATION_KEY, "");
	}

	@Override
	public void playServerAnimation(String animationName) {
		this.dataManager.set(ANIMATION_KEY, animationName);
	}

	@Override
	public boolean isPlayingAnimation(String animationName) {
		return animationName == this.dataManager.get(ANIMATION_KEY);
	}

	public static enum MovementMode {
		WALK((byte)1, 1.0F, ModEntities.ANIMATION_VILLAGER_WALK),
		RUN((byte)2, 1.4F, ModEntities.ANIMATION_VILLAGER_RUN),
		CREEP((byte)3, 0.8F, ModEntities.ANIMATION_VILLAGER_CREEP);

		public byte id;
		public float speedMultiplier;
		public String animation;

		private MovementMode(byte id, float speedMultiplier, String animation) {
			this.id = id;
			this.speedMultiplier = speedMultiplier;
			this.animation = animation;
		}

		public static MovementMode valueOf(byte id) {
			for (MovementMode mode : values()) {
				if (mode.id == id) {
					return mode;
				}
			}

			return null;
		}
	}
}
