package xyz.superez.dynamickeepinv;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class DeathGuiHolder implements InventoryHolder {
    private Inventory inventory;

    public void setInventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
