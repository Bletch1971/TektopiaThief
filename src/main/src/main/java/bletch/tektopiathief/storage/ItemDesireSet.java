package bletch.tektopiathief.storage;

import java.util.ArrayList;
import java.util.List;
import bletch.tektopiathief.entities.EntityThief;
import bletch.tektopiathief.utils.LoggerUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.tangotek.tektopia.VillageManager;

public class ItemDesireSet {
    protected List<ItemDesire> itemDesires;
    protected boolean deliveryDirty;
    protected int deliveryId;
    protected byte deliverySlot;
    protected short deliveryCount;
    protected int totalDeliverySize;
    
    public ItemDesireSet() {
        this.itemDesires = new ArrayList<ItemDesire>();
        this.deliveryDirty = true;
        this.deliveryId = 0;
        this.deliverySlot = -1;
        this.deliveryCount = 0;
        this.totalDeliverySize = 0;
    }
    
    public void clear() {
        this.itemDesires.clear();
    }
    
    public void addItemDesire(ItemDesire desire) {
        this.itemDesires.add(desire);
    }
    
    public void forceUpdate() {
        this.itemDesires.forEach(d -> d.forceUpdate());
        this.deliveryDirty = true;
    }
    
    public void onStorageUpdated(EntityThief entity, ItemStack storageItem) {
        this.itemDesires.forEach(d -> d.onStorageUpdated(entity, storageItem));
    }
    
    public void onInventoryUpdated(EntityThief entity, ItemStack updatedItem) {
        this.itemDesires.forEach(d -> d.onInventoryUpdated(entity, updatedItem));
        this.deliveryDirty = true;
    }
    
    public ItemDesire getNeededDesire(EntityThief entity) {
        for (ItemDesire desire : this.itemDesires) {
            if (desire.shouldPickUp(entity)) {
                return desire;
            }
        }
        return null;
    }
    
    private void updateDeliveryList(EntityThief entity) {
        if (this.deliveryDirty && entity.hasVillage()) {
            this.deliverySlot = -1;
            this.deliveryCount = 0;
            this.totalDeliverySize = 0;
            int bestValue = 0;
            
            for (int i = entity.getInventory().getSizeInventory() - 1; i >= 0; --i) {
                ItemStack itemStack = entity.getInventory().getStackInSlot(i);
                
                if (!itemStack.isEmpty()) {
                    int minDeliver = Integer.MAX_VALUE;
                    
                    for (ItemDesire desire : this.itemDesires) {
                        int toDeliver = desire.getDeliverToStorage(entity, itemStack);
                        
                        if (toDeliver <= 0) {
                            minDeliver = Integer.MAX_VALUE;
                            break;
                        }
                        
                        if (toDeliver <= 0 || toDeliver >= minDeliver) {
                            continue;
                        }
                        
                        minDeliver = toDeliver;
                    }
                    if (minDeliver < Integer.MAX_VALUE) {
                        int value = VillageManager.getItemValue(itemStack.getItem()) * minDeliver;
                        
                        if (value > bestValue) {
                            bestValue = value;
                            this.deliveryCount = (short)minDeliver;
                            this.deliverySlot = (byte)i;
                        }
                        
                        this.totalDeliverySize += value;
                    }
                }
            }
            
            this.deliveryDirty = false;
            ++this.deliveryId;
        }
    }
    
    public int getDeliveryId(EntityThief entity, int requiredDeliverSize) {
        this.updateDeliveryList(entity);
        
        if (this.totalDeliverySize >= requiredDeliverSize) {
            return this.deliveryId;
        }
        
        return 0;
    }
    
    public boolean isDeliveryMatch(int id) {
        return this.deliveryId == id;
    }
    
    public ItemStack getDeliveryItemCopy(EntityThief entity) {
        this.updateDeliveryList(entity);
        
        if (this.deliverySlot >= 0) {
            ItemStack deliverItem = entity.getInventory().getStackInSlot((int)this.deliverySlot).copy();
            deliverItem.setCount((int)this.deliveryCount);
            return deliverItem;
        }
        
        return ItemStack.EMPTY;
    }
    
    public boolean deliverItems(EntityThief entity, TileEntityChest destInv, int deliveryCheckId) {
        if (this.deliveryId != deliveryCheckId) {
        	LoggerUtils.info("Delivery Id mismatch", true);
            this.deliveryDirty = true;
            return false;
        }
        
        if (this.deliverySlot < 0) {
        	LoggerUtils.info("Delivery FAILED. No active delivery.", true);
            this.deliveryDirty = true;
            return false;
        }
        
        ItemStack removedStack = entity.getInventory().decrStackSize((int)this.deliverySlot, (int)this.deliveryCount);
        
        if (removedStack == ItemStack.EMPTY) {
        	LoggerUtils.info("Delivery FAILED. Delivery item not found in entity inventory", true);
            this.deliveryDirty = true;
            return false;
        }
        
        if (!this.deliverOneItem(removedStack, destInv, entity)) {
        	LoggerUtils.info("Delivery FAILED. Returning to entity inventory " + removedStack, true);
            entity.getInventory().addItem(removedStack);
            this.deliveryDirty = true;
            return false;
        }
        
        return true;
    }
    
    private boolean deliverOneItem(ItemStack sourceStack, TileEntityChest destChest, EntityThief entity) {
        int emptySlot = -1;
        
        for (int d = 0; d < destChest.getSizeInventory(); ++d) {
            ItemStack destStack = destChest.getStackInSlot(d);
            
            if (destStack.isEmpty() && emptySlot < 0) {
                emptySlot = d;
            }
            else if (ModInventory.areItemsStackable(destStack, sourceStack)) {
                int k = Math.min(sourceStack.getCount(), destStack.getMaxStackSize() - destStack.getCount());
                
                if (k > 0) {
                    destStack.grow(k);
                    
                    if (entity.hasVillage()) {
                    	entity.getVillage().onStorageChange(destChest, d, destStack);
                    }
                    
                    entity.onInventoryUpdated(destStack);
                    sourceStack.shrink(k);
                    
                    if (sourceStack.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        
        if (emptySlot >= 0) {
            destChest.setInventorySlotContents(emptySlot, sourceStack);
            
            if (entity.hasVillage()) {
            	entity.getVillage().onStorageChange(destChest, emptySlot, sourceStack);
            }
            
            return true;
        }
        
        return false;
    }

}
