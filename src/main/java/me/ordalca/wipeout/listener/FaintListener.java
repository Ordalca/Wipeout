package me.ordalca.wipeout.listener;

import com.pixelmonmod.pixelmon.api.events.BattleStartedEvent;
import com.pixelmonmod.pixelmon.api.events.PixelmonFaintEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.raids.EndRaidEvent;
import com.pixelmonmod.pixelmon.api.events.raids.StartRaidEvent;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.UUID;

public class FaintListener {
    public HashSet<UUID> playersInBattle = new HashSet<>();
    public HashSet<UUID> playersInRaid = new HashSet<>();

    @SubscribeEvent
    public void pokemonFaints(PixelmonFaintEvent.Post event) {
        if (event.getPlayer() != null) {
            if (!playersInRaid.contains(event.getPlayer().getUUID())) {
                if (!playersInBattle.contains(event.getPlayer().getUUID())) {
                    checkParty(event.getPlayer());
                }
            }
        }
    }

    @SubscribeEvent
    public void playerLogsOut(PlayerEvent.PlayerLoggedOutEvent event) {
        playersInRaid.remove(event.getPlayer().getUUID());
        playersInBattle.remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public void enterBattle(BattleStartedEvent event) {
        for (BattleParticipant part : event.bc.participants) {
            if (part instanceof PlayerParticipant) {
                PlayerParticipant player = (PlayerParticipant) part;
                playersInBattle.add(player.player.getUUID());
            }
        }
    }
    @SubscribeEvent
    public void exitBattle(BattleEndEvent event) {
        for (PlayerParticipant part : event.getBattleController().getPlayers()) {
            playersInBattle.remove(part.player.getUUID());
            checkParty(part.player);
        }
    }
    @SubscribeEvent
    public void enterRaid(StartRaidEvent event) {
        for (RaidData.RaidPlayer part : event.getRaid().getPlayers()) {
            if (part.playerEntity != null) {
                playersInRaid.add(part.playerEntity.getUUID());
            }
        }
    }
    @SubscribeEvent
    public void endRaid(EndRaidEvent event) {
        for (RaidData.RaidPlayer part : event.getRaid().getPlayers()) {
            if (part.playerEntity != null) {
                playersInRaid.remove(part.playerEntity.getUUID());
            }
        }
    }

    public void checkParty(ServerPlayerEntity player) {
        PlayerPartyStorage storage = StorageProxy.getParty(player);
        if (storage.countAblePokemon() == 0) {
            player.getFoodData().setFoodLevel(0);
            player.setHealth(0);
        }
    }
}
