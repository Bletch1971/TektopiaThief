package bletch.tektopiathief.storage;

import java.util.ArrayList;
import java.util.List;
import bletch.tektopiathief.entities.EntityThief;
import net.minecraft.item.ItemStack;

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
    
}
