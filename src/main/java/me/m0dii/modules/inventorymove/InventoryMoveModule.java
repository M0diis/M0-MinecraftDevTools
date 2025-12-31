package me.m0dii.modules.inventorymove;

import me.m0dii.modules.Module;

public class InventoryMoveModule extends Module {

    public static final InventoryMoveModule INSTANCE = new InventoryMoveModule();

    private InventoryMoveModule() {
        super("inventory_move", "Inventory Move", true);
    }
}
