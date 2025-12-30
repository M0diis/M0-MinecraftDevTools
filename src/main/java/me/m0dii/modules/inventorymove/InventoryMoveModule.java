package me.m0dii.modules.inventorymove;

import me.m0dii.modules.Module;

public class InventoryMoveModule extends Module {

    public static final InventoryMoveModule INSTANCE = new InventoryMoveModule();

    private InventoryMoveModule() {
        super("InventoryMove", "Allows movement while the inventory is open", true);
    }
}
