package com.serverdoctor.paper.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

/**
 * Marks an inventory as one of ours and records which screen it is. Click handling checks
 * {@code inventory.getHolder() instanceof MenuHolder} rather than matching titles.
 */
public final class MenuHolder implements InventoryHolder {

    private final MenuType type;
    private Inventory inventory;

    public MenuHolder(MenuType type) {
        this.type = type;
    }

    public MenuType type() {
        return type;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }
}
