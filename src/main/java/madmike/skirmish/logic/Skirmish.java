package madmike.skirmish.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Set;
import java.util.UUID;

public class Skirmish {
    private SkirmishChallenge challenge;

    private Set<UUID> challengers;

    private Set<UUID> opponents;

    private Set<UUID> spectators;

    long chShipId;

    long oppShipId;

    private long expiresAt;

    public SkirmishChallenge getChallenge() {
        return challenge;
    }

    public long getExpiredTime() {
        return expiresAt;
    }

    public boolean isPlayerInSkirmish(ServerPlayerEntity player) {
        return challengers.contains(player.getUuid()) || opponents.contains(player.getUuid());
    }

    public boolean handlePlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (challengers.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (challengers.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.OPPONENTS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                spectators.add(id);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
            return false;
        }

        if (opponents.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                spectators.add(id);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
            return false;
        }

        // player not in a skirmish, return true to allow death
        return true;
    }

    public boolean isShipInSkirmish(long id) {
        return id == chShipId || id == oppShipId;
    }

    public boolean isChallengerShip(long id) {
        return chShipId == id;
    }

    public void handlePlayerQuit(MinecraftServer server, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        if (opponents.remove(id)) {
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
        }

        if (opponents.remove(id)) {
            player.setHealth(player.getMaxHealth());
            if (opponents.isEmpty()) {
                SkirmishManager.INSTANCE.endSkirmish(player.getServer(), EndOfSkirmishType.CHALLENGERS_WIN);
            }
            else {
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You Died. Spectator Mode Enabled."));
            }
        }
    }
}
