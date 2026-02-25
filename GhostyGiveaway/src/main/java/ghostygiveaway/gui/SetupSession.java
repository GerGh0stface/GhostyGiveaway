package ghostygiveaway.gui;

import org.bukkit.inventory.ItemStack;

public class SetupSession {

    private ItemStack prizeItem = null;
    private int hours   = 0;
    private int minutes = 5;

    public ItemStack getPrizeItem()  { return prizeItem; }
    public void setPrizeItem(ItemStack item) { this.prizeItem = item; }

    public int getHours()   { return hours; }
    public int getMinutes() { return minutes; }

    public void adjustHours(int delta)   { hours   = Math.max(0, Math.min(167, hours + delta)); }
    public void adjustMinutes(int delta) { minutes = Math.max(0, Math.min(59,  minutes + delta)); }

    public long getTotalSeconds() { return (long) hours * 3600L + (long) minutes * 60L; }

    public boolean hasPrize()        { return prizeItem != null && !prizeItem.getType().isAir(); }
    public boolean hasValidDuration(){ return getTotalSeconds() >= 10; }
}
