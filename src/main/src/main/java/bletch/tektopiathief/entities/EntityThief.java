package bletch.tektopiathief.entities;

import bletch.common.MovementMode;
import bletch.common.Interfaces.IDesireEntity;
import bletch.common.Interfaces.IInventoryEntity;
import bletch.common.core.CommonEntities;
import bletch.common.entities.EntityEnemyBase;
import bletch.common.storage.ItemDesire;
import bletch.common.storage.ItemDesireSet;
import bletch.common.storage.ModInventory;
import bletch.common.utils.TextUtils;
import bletch.tektopiathief.core.ModConfig;
import bletch.tektopiathief.core.ModDetails;
import bletch.tektopiathief.entities.ai.*;
import bletch.tektopiathief.schedulers.ThiefScheduler;
import bletch.tektopiathief.utils.LoggerUtils;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillagerRole;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.structures.VillageStructure;
import net.tangotek.tektopia.structures.VillageStructureType;
import net.tangotek.tektopia.tickjob.TickJob;

import java.util.Iterator;

@SuppressWarnings("unchecked")
public class EntityThief extends EntityEnemyBase implements IInventoryEntity, IDesireEntity {

    public static final String ENTITY_NAME = "thief";
    public static final String MODEL_NAME = "thief";
    public static final String RESOURCE_PATH = "thief";
    public static final String ANIMATION_MODEL_NAME = MODEL_NAME + "_m";

    public static long WORK_START_TIME = 16000L; // 22:00 (10:00 PM)
    public static long WORK_END_TIME = 23500L; // 05:30 (5:30 AM)

    protected static final DataParameter<ItemStack> ACTION_ITEM;
    protected static final DataParameter<Boolean> SEEN;

    protected ModInventory inventory;
    protected ItemDesireSet desireSet;

    public EntityThief(World worldIn) {
        super(worldIn, ModDetails.MOD_ID);

        this.inventory = new ModInventory(this, "Items", false, 1);
    }

    public EntityThief(World worldIn, int level) {
    	super(worldIn, ModDetails.MOD_ID, level);

        this.inventory = new ModInventory(this, "Items", false, 1);
    }

    @Override
    protected void attachToVillage(Village village) {
        super.attachToVillage(village);

        LoggerUtils.instance.info("Attaching to village", true);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        float beforeHealth = this.getHealth();

        if (super.attackEntityFrom(source, amount)) {
            float afterHealth = this.getHealth();
            float actualDamage = beforeHealth - afterHealth;

            if (this.hasVillage() && actualDamage > 0.0F) {
                String message = TextUtils.translate("message.thief.damage", String.format("%.2f", actualDamage));
                Entity damagedBy = source.getTrueSource();

                if (damagedBy != null) {
                    message += " " + TextUtils.translate("message.thief.damagefrom", damagedBy.getDisplayName().getUnformattedText());

                    if (damagedBy instanceof EntityVillagerTek) {
                        this.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 60));
                        
                        // only output the damage message if a villager is responsible for the damage
                        this.village.sendChatMessage(message);
                    }
                }

                LoggerUtils.instance.info(message, true);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void checkStuck() {
        if (this.hasVillage() && this.firstCheck.distanceSq(this.getPosition()) < 10.0D) {
            LoggerUtils.instance.info("Killing self...failed to find a way to the village", true);
            this.setDead();
        }
    }

    @Override
    protected void detachVillage() {
        super.detachVillage();

        LoggerUtils.instance.info("Detaching from village", true);
    }

    protected void dropAllItems(DamageSource cause) {
        LoggerUtils.instance.info("Dropping all items", true);

        IInventory inventory = this.getInventory();
        Entity damagedBy = cause.getTrueSource();

        for (int slot = 0; slot < inventory.getSizeInventory(); ++slot) {
            ItemStack itemStack = inventory.getStackInSlot(slot);

            if (!itemStack.isEmpty()) {
                String message = "Thief has dropped " + itemStack.getDisplayName();
                if (itemStack.getCount() > 1) {
                    message += " x " + itemStack.getCount();
                }

                if (damagedBy != null) {
                    // only show the message if killed by a villager
                    this.village.sendChatMessage(message);
                }
                LoggerUtils.instance.info(message, true);

                this.entityDropItem(itemStack, 0.5f);
            }
        }

        inventory.clear();
    }

    public void equipActionItem(ItemStack actionItem) {
        this.dataManager.set(ACTION_ITEM, actionItem.copy());
    }

    public ItemStack getActionItem() {
        return this.dataManager.get(ACTION_ITEM);
    }

    @Override
    public float getAIMoveSpeed() {
    	MovementMode movementMode = this.getMovementMode();
    	
    	switch (movementMode) {
    	case CREEP:
    		// find the nearest storage structure
    		VillageStructure storage = this.getVillage().getNearestStructure(VillageStructureType.STORAGE, this.getPosition());
    		if (storage != null) {
        		// get distance to nearest storage structure
        		double distanceTo = storage.getItemFrame().getDistance(this);
        		// check if near the storage structure
        		if (distanceTo > (Village.VILLAGE_SIZE / 2))
        			// not close to storage, so move a little faster
    				return (0.30F + this.getLevel() * 0.02F) * movementMode.speedMultiplier;
    		}
    	default:
    		break;
    	}

        return (0.20F + this.getLevel() * 0.02F) * movementMode.speedMultiplier;
    }

    public ItemStack getAquiredItem() {
        return this.inventory.getStackInSlot(0);
    }

    public float getAvoidanceDistance() {
        float avoidDistance = this.getAvoidanceDistanceBase() - this.getLevel();

        int light = this.world.getLightFor(EnumSkyBlock.BLOCK, this.getPosition());
        // check for minecraft day time (6am - 7pm)
        if (Village.isTimeOfDay(this.world, 0, 13000)) {
            // light level is the maximum of Sky and block light
            light = Math.max(light, this.world.getLightFor(EnumSkyBlock.SKY, this.getPosition()));
        }

        light = (Math.max(0, Math.min(15, light)) + 1) / 2;
        avoidDistance -= 8 - light;

        LoggerUtils.instance.info("EntityThief - getAvoidanceDistance called; avoidDistanceBase=" + this.getAvoidanceDistanceBase() + "; Level=" + this.getLevel() + "; light=" + light + "; avoidDistance=" + avoidDistance, true);

        return Math.max(1.0F, avoidDistance);
    }

    public float getAvoidanceDistanceBase() {
        return 24.0F;
    }

    @Override
    public ItemDesireSet getDesireSet() {
    	if (this.desireSet == null)
    		this.setupDesires();
    	
        return this.desireSet;
    }

    public float getDetectionDistance() {
        float avoidDistance = this.getAvoidanceDistance();
        float detectDistance = avoidDistance - 5.0F;

        LoggerUtils.instance.info("EntityThief - getDetectionDistance called; avoidDistance=" + avoidDistance + "; detectDistance=" + detectDistance, true);

        return Math.max(1.0F, detectDistance);
    }

    @Override
    public ITextComponent getDisplayName() {
        ITextComponent itextcomponent = new TextComponentTranslation("entity." + MODEL_NAME + ".name");
        itextcomponent.getStyle().setHoverEvent(this.getHoverEvent());
        itextcomponent.getStyle().setInsertion(this.getCachedUniqueIdString());
        return itextcomponent;
    }

    @Override
    public ModInventory getInventory() {
        return this.inventory;
    }

    public Boolean getSeen() {
        return this.dataManager.get(SEEN);
    }

    public Boolean hasAcquiredItem() {
        return !this.inventory.hasSlotFree();
    }

    @Override
    protected void initEntityAI() {
    	setupAITasks();
        setupDesires();
    }

    public com.google.common.base.Predicate<Entity> isEnemy() {
        return (e) -> e instanceof EntityVillagerTek && ((EntityVillagerTek) e).isRole(VillagerRole.DEFENDER)
                || ModConfig.thief.thiefdetectsplayer && e instanceof EntityPlayer;
    }

    public boolean isFleeFrom(Entity e) {
        return this.isEnemy().test(e)
                || e instanceof EntityVillagerTek && ((EntityVillagerTek) e).isRole(VillagerRole.VILLAGER);
    }

    @Override
    public boolean isStoragePriority() {
        return this.hasVillage() && this.isWorkTime() && !this.getSeen() && !this.hasAcquiredItem();
    }

    @Override
    public boolean isWorkTime() {
        return isWorkTime(this.world, 0) && !this.world.isRaining();
    }

    @Override
    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        if (!this.world.isRemote) {
            String message = TextUtils.translate("message.thief.killed");
            Entity damagedBy = cause.getTrueSource();

            if (damagedBy != null) {
                message += " " + TextUtils.translate("message.thief.killedby", damagedBy.getDisplayName().getUnformattedComponentText());

                if (damagedBy instanceof EntityVillagerTek) {
                    // only show the message if killed by a villager
                    this.village.sendChatMessage(message);
                }
            }

            LoggerUtils.instance.info(message, true);

            this.dropAllItems(cause);
        }
    }

    @Override
    public void onInventoryUpdated(ItemStack updatedItem) {
        this.desireSet.onInventoryUpdated(this, updatedItem);
    }

    public void onStorageChange(ItemStack storageItem) {
        this.desireSet.onStorageUpdated(this, storageItem);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL && !ModConfig.thief.thiefSpawnsWhenPeaceful) {
            LoggerUtils.instance.info("Killing self...difficulty is peaceful", true);
            this.setDead();
        }
    }

    @Override
    public void pickupDesiredItem(ItemStack desiredItem) {
        this.equipActionItem(desiredItem);
    }

    protected void scanForEnemies() {
        if (!this.hasVillage())
            return;

        float detectionDistance = this.getDetectionDistance();
        if (detectionDistance == 0)
            return;

        Predicate<EntityLivingBase> entityPredicate = Predicates.and(EntitySelectors.CAN_AI_TARGET,
                e -> e.isEntityAlive() && e.canEntityBeSeen(this),
                this.isEnemy());

        AxisAlignedBB boundingBox = this.getEntityBoundingBox().grow(detectionDistance, 6.0F, detectionDistance);
        Iterator<EntityLivingBase> entityList = this.world.getEntitiesWithinAABB(EntityLivingBase.class, boundingBox, entityPredicate).stream()
                .filter((e) -> e.getDistance(this) <= detectionDistance)
                .iterator();

        while (entityList.hasNext()) {
            EntityLivingBase entity = entityList.next();

            LoggerUtils.instance.info("EntityThief - scanForEnemies called, seen by entity"
                            + "; entity=" + entity.getName()
                            + "; detection distance=" + detectionDistance
                            + "; distance=" + entity.getDistance(this)
                    , true);

            this.village.addOrRenewEnemy(this, 1);

            if (!this.getSeen()) {
                this.setSeen(true);
                this.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 120));
                this.village.sendChatMessage(TextUtils.translate("message.thief.seen"));
            }
        }
    }

    public void setSeen(Boolean seen) {
        LoggerUtils.instance.info("EntityThief - setSeen called; seen=" + seen, true);

        this.dataManager.set(SEEN, seen);
    }

    @Override
    protected void setupAITasks() {
    	super.setupAITasks();

        this.addTask(1, new EntityAIFleeEntity(this,
                (e) -> e.isWorkTime() && !e.getSeen() && !e.hasAcquiredItem(),
                (e) -> this.isFleeFrom(e)));

        this.addTask(30, new EntityAILeaveVillage(this,
                (e) -> !e.isWorkTime() && !e.getSeen() && !e.hasAcquiredItem(),
                (e) -> e.getVillage().getEdgeNode(),
                MovementMode.WALK, null,
                () -> {
                    LoggerUtils.instance.info("Killing Self...left the village", true);
                    this.setDead();
                }));

        this.addTask(40, new EntityAIEscapeVillage(this,
                (e) -> e.getSeen() || e.hasAcquiredItem(),
                (e) -> e.getVillage().getEdgeNode(),
                MovementMode.RUN, null,
                () -> {

                    if (this.hasAcquiredItem()) {
                        String message = this.getSeen()
                                ? TextUtils.translate("message.thief.escapedseen")
                                : TextUtils.translate("message.thief.escaped");
                        ItemStack aquiredItem = this.getAquiredItem();

                        if (aquiredItem != null && !aquiredItem.isEmpty()) {
                            ThiefScheduler.setGracePeriod(this.village, 2);

                            String aquiredItemDescription = aquiredItem.getDisplayName();
                            if (aquiredItem.getCount() > 1) {
                                aquiredItemDescription += " x " + aquiredItem.getCount();
                            }
                            message += " " + TextUtils.translate("message.thief.escapedwith", aquiredItemDescription);
                        }

                        this.village.sendChatMessage(message);
                        LoggerUtils.instance.info(message, true);
                    } else if (this.getSeen()) {
                        String message = TextUtils.translate("message.thief.escapedseen");

                        this.village.sendChatMessage(message);
                        LoggerUtils.instance.info(message, true);
                    }

                    LoggerUtils.instance.info("Killing Self...escaped the village", true);
                    this.setDead();
                }));

        this.addTask(50, new EntityAIRetrieveFromStorage(this));
    }

    @Override
    public void setupDesires() {
        this.desireSet = new ItemDesireSet();

        // CROPS
        this.desireSet.addItemDesire(new ItemDesire(Items.BEETROOT, 5, e -> e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.CARROT, 5, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.POTATO, 5, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.WHEAT, 5, e -> e.getLevel() < 3));

        // FOOD
        this.desireSet.addItemDesire(new ItemDesire(Items.APPLE, 4, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.BAKED_POTATO, 3, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.BEEF, 1, e -> e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.BEETROOT_SOUP, 1, e -> e.getLevel() > 2));
        this.desireSet.addItemDesire(new ItemDesire(Items.BREAD, 2, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.CAKE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.CHICKEN, 1, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.COOKED_BEEF, 1, e -> e.getLevel() > 1 && e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.COOKED_CHICKEN, 1, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.COOKED_MUTTON, 1, e -> e.getLevel() > 1 && !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.COOKED_PORKCHOP, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.COOKIE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.EGG, 4, e -> e.getLevel() < 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.GOLDEN_APPLE, 1, e -> e.getLevel() > 3 && !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.GOLDEN_CARROT, 1, e -> e.getLevel() > 3 && e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.MILK_BUCKET, 1, e -> e.getLevel() > 2));
        this.desireSet.addItemDesire(new ItemDesire(Items.MUTTON, 1, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.PORKCHOP, 1, e -> e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.PUMPKIN_PIE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.SUGAR, 5, null));

        // RESOURCES
        this.desireSet.addItemDesire(new ItemDesire(Items.COAL, 5, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Blocks.GOLD_ORE, 2, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.GOLD_INGOT, 2, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Blocks.IRON_ORE, 2, e -> e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_INGOT, 2, e -> e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Blocks.LOG, 3, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.REDSTONE, 5, e -> e.getLevel() > 2));
        this.desireSet.addItemDesire(new ItemDesire(Blocks.WOOL, 2, null));

        // TOOLS
        this.desireSet.addItemDesire(new ItemDesire(Items.BOOK, 1, e -> !e.isMale()));
        this.desireSet.addItemDesire(new ItemDesire(Items.BUCKET, 1, null));
        this.desireSet.addItemDesire(new ItemDesire(Items.PAPER, 4, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.SHEARS, 1, e -> e.isMale()));

        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_AXE, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_HOE, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_PICKAXE, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_SHOVEL, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_AXE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_HOE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_PICKAXE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_SHOVEL, 1, e -> e.getLevel() > 3));

        // WEAPONS
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_SWORD, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_SWORD, 1, e -> e.getLevel() > 3));

        // ARMOR
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_HELMET, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_CHESTPLATE, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_LEGGINGS, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.IRON_BOOTS, 1, e -> e.getLevel() > 1));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_HELMET, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_CHESTPLATE, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_LEGGINGS, 1, e -> e.getLevel() > 3));
        this.desireSet.addItemDesire(new ItemDesire(Items.DIAMOND_BOOTS, 1, e -> e.getLevel() > 3));
    }

    @Override
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
                        LoggerUtils.instance.info("Killing self...no village", true);
                        this.setDead();
                    }
                }
        ));
    }

    public void unequipActionItem() {
        this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
    }

    public void unequipActionItem(ItemStack actionItem) {
        if (actionItem != null && actionItem.getItem() == this.getActionItem().getItem()) {
            this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey("seen"))
            this.setSeen(compound.getBoolean("seen"));

        this.inventory.readNBT(compound);
        this.getDesireSet().forceUpdate();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);

        compound.setBoolean("seen", this.getSeen());

        this.inventory.writeNBT(compound);
    }

    static {
        ACTION_ITEM = EntityDataManager.createKey(EntityThief.class, DataSerializers.ITEM_STACK);
        SEEN = EntityDataManager.createKey(EntityThief.class, DataSerializers.BOOLEAN);

        setupCraftStudioAnimations(ModDetails.MOD_ID, ANIMATION_MODEL_NAME);
    }

    protected static void setupCraftStudioAnimations(String modId, String modelName) {
    	EntityEnemyBase.setupCraftStudioAnimations(modId, modelName);
    	
        animationHandler.addAnim(modId, CommonEntities.ANIMATION_VILLAGER_CREEP, modelName, true);
    }

    public static boolean isWorkTime(World world, int sleepOffset) {
        return Village.isTimeOfDay(world, WORK_START_TIME, WORK_END_TIME, sleepOffset);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();

        this.dataManager.set(ACTION_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected void entityInit() {
        this.dataManager.register(ACTION_ITEM, ItemStack.EMPTY);
        this.dataManager.register(SEEN, false);

        super.entityInit();
    }
    
}
