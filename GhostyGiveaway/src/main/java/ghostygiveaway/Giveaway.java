package ghostygiveaway;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Giveaway {

    private final String id;
    private final ItemStack prize;
    private final UUID creatorUUID;
    private final String creatorName;
    private final long durationSeconds;
    private long remainingSeconds;
    private final Set<UUID> participants = new HashSet<>();
    private BukkitTask countdownTask;
    private boolean ended = false;

    public Giveaway(String id, ItemStack prize, Player creator, long durationSeconds) {
        this.id              = id;
        this.prize           = prize.clone();
        this.creatorUUID     = creator.getUniqueId();
        this.creatorName     = creator.getName();
        this.durationSeconds = durationSeconds;
        this.remainingSeconds = durationSeconds;
    }

    public String getId()               { return id; }
    public ItemStack getPrize()         { return prize.clone(); }
    public UUID getCreatorUUID()        { return creatorUUID; }
    public String getCreatorName()      { return creatorName; }
    public long getDurationSeconds()    { return durationSeconds; }
    public long getRemainingSeconds()   { return remainingSeconds; }
    public Set<UUID> getParticipants()  { return Collections.unmodifiableSet(participants); }
    public BukkitTask getCountdownTask(){ return countdownTask; }
    public boolean isEnded()            { return ended; }

    public void setRemainingSeconds(long s) { remainingSeconds = s; }
    public void setCountdownTask(BukkitTask t) { countdownTask = t; }
    public void setEnded(boolean b)     { ended = b; }

    public boolean addParticipant(UUID uuid) { return participants.add(uuid); }
    public boolean hasParticipant(UUID uuid) { return participants.contains(uuid); }

    public UUID pickWinner() {
        if (participants.isEmpty()) return null;
        int idx = (int)(Math.random() * participants.size());
        return participants.stream().skip(idx).findFirst().orElse(null);
    }
}
