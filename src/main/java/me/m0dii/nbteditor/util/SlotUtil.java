package me.m0dii.nbteditor.util;

import net.minecraft.entity.EquipmentSlot;

/**
 * <h1>Slot Formats:</h1>
 * <h2>inv - Inventory (PlayerInventory):</h2>
 * <ul>
 *   <li>0-8: Hotbar</li>
 *   <li>9-35: Inventory</li>
 *   <li>36-39: Armor (feet -> head)</li>
 *   <li>40: Off Hand</li>
 * </ul>
 * <h2>container - Inventory Container (PlayerScreenHandler):</h2>
 * <ul>
 *   <li>5-8: Armor <strong>(head -> feet)</strong></li>
 *   <li>9-35: Inventory</li>
 *   <li>36-44: Hotbar</li>
 *   <li>45: Off Hand</li>
 * </ul>
 * <h2>generic - Player Inventory Part of Generic Container (GenericContainerScreenHandler):</h2>
 * These slot indices are meant to be added to the number of slots in the upper inventory
 * <ul>
 *   <li>0-26: Inventory</li>
 *   <li>27-35: Hotbar</li>
 *   <li>36-39: Armor (feet -> head) (not accessible)</li>
 *   <li>40: Off Hand (not visible)</li>
 * </ul>
 * <h2>generic container - All of Generic Container (GenericContainerScreenHandler)</h2>
 * The slot indices of the GenericContainerScreenHandler of size n:
 * <ul>
 *   <li>0-(n-1): Upper Inventory</li>
 *   <li>n-(n+40): Format: generic (see above)</li>
 * </ul>
 */
public class SlotUtil {

    // Conversions
    public static int invToContainer(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }
        if (slot < 9) {
            return slot + 36;
        }
        if (slot < 36) {
            return slot;
        }
        if (slot < 40) {
            return 8 - (slot - 36);
        }
        if (slot == 40) {
            return 45;
        }
        throw new IllegalArgumentException("Invalid slot index: " + slot);
    }

    public static int invToGeneric(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }
        if (slot < 9) {
            return slot + 27;
        }
        if (slot < 36) {
            return slot - 9;
        }
        if (slot <= 40) {
            return slot;
        }
        throw new IllegalArgumentException("Invalid slot index: " + slot);
    }

    public static int containerToInv(int slot) {
        if (slot < 5) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }
        if (slot < 9) {
            return 8 - slot + 36;
        }
        if (slot < 36) {
            return slot;
        }
        if (slot < 45) {
            return slot - 36;
        }
        if (slot == 45) {
            return 40;
        }
        throw new IllegalArgumentException("Invalid slot index: " + slot);
    }

    public static int genericToInv(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot index: " + slot);
        }
        if (slot < 27) {
            return slot + 9;
        }
        if (slot < 36) {
            return slot - 27;
        }
        if (slot <= 40) {
            return slot;
        }
        throw new IllegalArgumentException("Invalid slot index: " + slot);
    }

    // Extractions (inv)

    public static boolean isHotbarFromInv(int slot) {
        return 0 <= slot && slot < 9;
    }

    public static int extractHotbarFromInv(int slot) {
        if (isHotbarFromInv(slot)) {
            return slot;
        }

        throw new IllegalArgumentException("Invalid hotbar index: " + slot);
    }

    public static boolean isInventoryFromInv(int slot) {
        return 9 <= slot && slot < 36;
    }

    public static int extractInventoryFromInv(int slot) {
        if (isInventoryFromInv(slot)) {
            return slot - 9;
        }

        throw new IllegalArgumentException("Invalid inventory index: " + slot);
    }

    public static boolean isArmorFromInv(int slot) {
        return 36 <= slot && slot < 40;
    }

    public static EquipmentSlot extractArmorFromInv(int slot) {
        if (isArmorFromInv(slot)) {
            return EquipmentSlot.values()[slot - 36 + 2];
        }

        throw new IllegalArgumentException("Invalid armor index: " + slot);
    }

    public static boolean isOffHandFromInv(int slot) {
        return slot == 40;
    }

    // Creations (inv)
    public static int createHotbarInInv(int slot) {
        if (0 <= slot && slot < 9) {
            return slot;
        }
        throw new IllegalArgumentException("Invalid hotbar index: " + slot);
    }

    public static int createInventoryInInv(int slot) {
        if (0 <= slot && slot < 27) {
            return slot + 9;
        }
        throw new IllegalArgumentException("Invalid inventory index: " + slot);
    }

    public static int createArmorInInv(EquipmentSlot slot) {
        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return slot.getEntitySlotId() + 36;
        }
        throw new IllegalArgumentException("Invalid armor index: " + slot);
    }

    public static int createOffHandInInv() {
        return 40;
    }

    // Creations (container)

    public static int createHotbarInContainer(int slot) {
        return invToContainer(createHotbarInInv(slot));
    }

    public static int createArmorInContainer(EquipmentSlot slot) {
        return invToContainer(createArmorInInv(slot));
    }

    public static int createOffHandInContainer() {
        return invToContainer(createOffHandInInv());
    }

}
