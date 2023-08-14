package me.ordalca.wipeout.listener;

import com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonFaintEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.raids.EndRaidEvent;
import com.pixelmonmod.pixelmon.api.events.raids.StartRaidEvent;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.util.helpers.DimensionHelper;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.comm.ChatHandler;
import com.pixelmonmod.pixelmon.storage.playerData.TeleportPosition;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.UUID;

import static com.pixelmonmod.pixelmon.api.util.helpers.DimensionHelper.findLocation;

public class FaintListener {
    public static boolean deathOnWipe = false;
    public static TeleportPosition teleportOverride = null;
    public static boolean forceHeal = false;

    public static HashSet<UUID> playersInBattle = new HashSet<>();
    public static HashSet<UUID> playersInRaid = new HashSet<>();

    @SubscribeEvent
    public static void pokemonFaints(PixelmonFaintEvent.Post event) {
        if (event.getPlayer() != null) {
            if (!playersInRaid.contains(event.getPlayer().getUUID())) {
                if (!playersInBattle.contains(event.getPlayer().getUUID())) {
                    checkParty(event.getPlayer());
                }
            }
        }
    }

    @SubscribeEvent
    public static void playerLogsOut(PlayerEvent.PlayerLoggedOutEvent event) {
        playersInRaid.remove(event.getPlayer().getUUID());
        playersInBattle.remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void enterBattle(BattleStartedEvent event) {
        for (PlayerParticipant player : event.getBattleController().getPlayers()) {
            playersInBattle.add(player.player.getUUID());
        }
    }
    @SubscribeEvent
    public static void exitBattle(BattleEndEvent event) {
        for (PlayerParticipant part : event.getBattleController().getPlayers()) {
            playersInBattle.remove(part.player.getUUID());
            checkParty(part.player);
        }
    }
    @SubscribeEvent
    public static void enterRaid(StartRaidEvent event) {
        for (RaidData.RaidPlayer part : event.getRaid().getPlayers()) {
            if (part.playerEntity != null) {
                playersInRaid.add(part.playerEntity.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void endRaid(EndRaidEvent event) {
        for (RaidData.RaidPlayer part : event.getRaid().getPlayers()) {
            if (part.playerEntity != null) {
                playersInRaid.remove(part.playerEntity.getUUID());
            }
        }
    }

    public static void checkParty(ServerPlayerEntity player) {
        PlayerPartyStorage storage = StorageProxy.getParty(player);
        if (storage.countAblePokemon() == 0) {
            ChatHandler.sendChat(player, "Your party has all fainted.  You've whited out.");
            if (FaintListener.forceHeal) {
                storage.heal();
            }

            if (FaintListener.deathOnWipe) {
                player.kill();
            } else {
                TeleportPosition respawnPoint;
                if (FaintListener.teleportOverride != null) {
                    respawnPoint = FaintListener.teleportOverride;
                } else if (!isEmpty(storage.teleportPos)) {
                    respawnPoint = storage.teleportPos;
                } else {
                     respawnPoint = new TeleportPosition();
                    if (player.getRespawnPosition() != null) {
                        BlockPos point = player.getRespawnPosition();
                        respawnPoint.store(player.getRespawnDimension(), point.getX(), point.getY(), point.getZ(), 0, 0);
                    } else {
                        int x = player.level.getLevelData().getXSpawn();
                        int y = player.level.getLevelData().getYSpawn();
                        int z = player.level.getLevelData().getZSpawn();
                        BlockPos point = new BlockPos(x,y,z);
                        respawnPoint.store(World.OVERWORLD, point.getX(), point.getY(), point.getZ(), 0, 0);
                    }
                }

                if (respawnPoint != null) {
                    teleport(player, respawnPoint);
                }
            }
        }
    }

    private static boolean isEmpty(TeleportPosition telePos) {
        BlockPos pos = telePos.getPosition();
        if (pos.getX() != 0 || pos.getY() != 0 || pos.getZ() == 0) return false;
        return (telePos.getDimension() != World.OVERWORLD);
    }

    private static void teleport(ServerPlayerEntity player, TeleportPosition telePos) {
        DimensionHelper.getWorld(telePos.getDimension()).ifPresent(world -> {
            BlockPos original = telePos.getPosition();
            BlockPos coords = findLocation(world, player, original.getX(), original.getY(), original.getZ());
            player.teleportTo(world, coords.getX(), coords.getY(), coords.getZ(), player.yRot, player.xRot);
        });
    }
}
