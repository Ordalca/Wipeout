package me.ordalca.wipeout;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.pixelmonmod.pixelmon.api.command.PixelmonCommandUtils;
import com.pixelmonmod.pixelmon.api.util.helpers.DimensionHelper;
import com.pixelmonmod.pixelmon.api.util.helpers.RegistryHelper;
import com.pixelmonmod.pixelmon.command.PixelCommand;

import com.pixelmonmod.pixelmon.storage.playerData.TeleportPosition;
import me.ordalca.wipeout.listener.FaintListener;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WipeoutCommand extends PixelCommand {
    public WipeoutCommand(CommandDispatcher<CommandSource> dispatcher) {
        super(dispatcher, "wipeout", "/wipeout <command>", 4);
    }
    @Override
    public void execute(CommandSource sender, String[] args) throws CommandException {
        ServerPlayerEntity player = PixelmonCommandUtils.requireEntityPlayer(sender);
        if (player == null) {
            PixelmonCommandUtils.endCommand("argument.entity.notfound.player", args[0]);
        }

        String command = args[0];
        if (command.equalsIgnoreCase("print")) {
            String message = "Current Settings:";
            message = message+"\n  On Wipe: "+(FaintListener.deathOnWipe?"DEATH":"TELEPORT");
            if (FaintListener.teleportOverride != null) {
                message = message+"\n  To: "+ getWipeLocation();
            }
            message = message+"\n  Force heal on wipe: "+ (FaintListener.forceHeal?"YES":"NO");

            PixelmonCommandUtils.sendMessage(player, message);
        } else {
            if (command.equalsIgnoreCase("death")) {
                FaintListener.deathOnWipe = true;
                FaintListener.teleportOverride = null;
                PixelmonCommandUtils.sendMessage(player, "Set wipe penalty to 'Trainer death.'");
            } else if (command.equalsIgnoreCase("teleport")) {
                FaintListener.deathOnWipe = false;
                FaintListener.teleportOverride = null;
                PixelmonCommandUtils.sendMessage(player, "Set wipe penalty to 'Teleport to healer/spawn.'");
            } else if (command.equalsIgnoreCase("override")) {
                FaintListener.deathOnWipe = false;
                RegistryKey<World> dim = player.getLevel().dimension();
                BlockPos loc  = player.blockPosition();
                Vector2f rot = player.getRotationVector();

                if (args.length >= 4) {
                    int x = parse(player, args[1], Axis.X);
                    int y = parse(player, args[2], Axis.Y);
                    int z = parse(player, args[3], Axis.Z);
                    loc = new BlockPos(x,y,z);

                    if (args.length >= 5) {
                        RegistryKey<World> newDim = RegistryHelper.getKey(Registry.DIMENSION_REGISTRY, args[4]);
                        if (newDim != null) {
                            dim = newDim;
                        }
                    }
                }

                TeleportPosition newPos = new TeleportPosition();
                newPos.store(dim, loc.getX(), loc.getY(), loc.getZ(), rot.x, rot.y);
                FaintListener.teleportOverride = newPos;
                PixelmonCommandUtils.sendMessage(player, "Set wipe penalty to 'Teleport to "+getWipeLocation()+"'");
            } else if (command.equalsIgnoreCase("heal")) {
                FaintListener.forceHeal = !FaintListener.forceHeal;
                PixelmonCommandUtils.sendMessage(player, "Set 'heal on wipe' to "+(FaintListener.forceHeal ? "YES":"NO"));
            } else {
                PixelmonCommandUtils.sendMessage(player, "Unrecognized command");
                return;
            }

            Optional<ServerWorld> serverworld1 = DimensionHelper.getWorld(World.OVERWORLD);
            if (serverworld1.isPresent()) {
                WipeoutSaveData.refreshData(serverworld1.get());
            } else {
                PixelmonCommandUtils.endCommand("Could not load Overworld to save settings.");
            }
        }
    }
    private int parse(ServerPlayerEntity player, String loc, Axis axis) {
        int point = 0;
        int offset = 0;
        if (loc.startsWith("~")) {
            point = player.blockPosition().get(axis);
            loc = loc.substring(1);
        }
        if (loc.length() > 0) {
            offset = PixelmonCommandUtils.requireInt(loc, "Unknown location for " + axis.getName() + "-coordinate");
        }
        return point+offset;
    }
    private String getWipeLocation() {
        return FaintListener.teleportOverride.getPosition().toShortString()
                + " in " + FaintListener.teleportOverride.getDimension().location().getPath();
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, CommandSource sender, String[] args, BlockPos pos) {
        List<String> list = Lists.newArrayList();
        if (args.length == 1) {
            list.add("print");
            list.add("heal");
            list.add("death");
            list.add("teleport");
            list.add("override");
        } else if (args[0].equalsIgnoreCase("override")) {
            if (args.length <= 4) {
                list.add("~");
                if (args.length == 2) {
                    list.add("~ ~ ~");
                } else if (args.length == 3){
                    list.add("~ ~");
                }
            } else if (args.length == 5) {
                Set<ResourceLocation> dims = server.registryAccess().dimensionTypes().keySet();
                for (ResourceLocation entry : dims) {
                    list.add(entry.toString());
                }
            }
        }
        return list;
    }
}
