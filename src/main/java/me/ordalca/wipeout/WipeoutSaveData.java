package me.ordalca.wipeout;

import com.pixelmonmod.pixelmon.api.util.helpers.RegistryHelper;
import com.pixelmonmod.pixelmon.storage.playerData.TeleportPosition;
import me.ordalca.wipeout.listener.FaintListener;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

public class WipeoutSaveData extends WorldSavedData {
		public static final String NAME = ModFile.MOD_ID + "_settings";

		public WipeoutSaveData(String key) {
				super(key);
		}

		public WipeoutSaveData() {
				this(NAME);
		}

		public static void refreshData(ServerWorld world) {
				WipeoutSaveData data = world.getDataStorage().computeIfAbsent(WipeoutSaveData::new, WipeoutSaveData.NAME);
				data.setDirty();
		}

		public static WipeoutSaveData getData(ServerWorld world) {
				return world.getDataStorage().get(WipeoutSaveData::new, WipeoutSaveData.NAME);
		}

		@Override
		public void load(CompoundNBT nbt) {
				if (nbt.contains(NAME)) {
						CompoundNBT settingsData = nbt.getCompound(NAME);
						WipeoutSettingsData data = WipeoutSettingsData.serialize(settingsData);
						FaintListener.forceHeal = data.forceHeal;
						FaintListener.deathOnWipe = data.deathOnWipe;
						FaintListener.teleportOverride = data.teleportOverride;
				}
		}

		@Override
		public CompoundNBT save(CompoundNBT nbt) {
				WipeoutSettingsData data = new WipeoutSettingsData(FaintListener.forceHeal,
								FaintListener.deathOnWipe,
								FaintListener.teleportOverride);
				nbt.put(NAME, data.deserialize());
				return nbt;
		}

		static class WipeoutSettingsData{
				private final boolean forceHeal;
				private final boolean deathOnWipe;
				private final TeleportPosition teleportOverride;


				WipeoutSettingsData(boolean forceHeal, boolean deathOnWipe, TeleportPosition teleportOverride) {
						this.forceHeal = forceHeal;
						this.deathOnWipe = deathOnWipe;
						this.teleportOverride = teleportOverride;
				}
				public CompoundNBT deserialize() {
						CompoundNBT nbt = new CompoundNBT();
						nbt.putBoolean("forceHeal", this.forceHeal);
						nbt.putBoolean("deathOnWipe", this.deathOnWipe);
						nbt.putBoolean("hasOverride", this.teleportOverride != null);
						if (this.teleportOverride != null) {
								this.teleportOverride.writeToNBT(nbt);
								nbt.putString("dimensionKey", this.teleportOverride.getDimension().location().toString());
						}
						return nbt;
				}

				public static WipeoutSettingsData serialize(CompoundNBT nbt) {
						boolean heal = nbt.getBoolean("forceHeal");
						boolean death = nbt.getBoolean("deathOnWipe");
						boolean override = nbt.getBoolean("hasOverride");
						TeleportPosition pos = null;
						if (override) {
								pos = new TeleportPosition();
								// pos.readFromNbt(nbt);

								double x = nbt.getDouble("tpPosX");
								double y = nbt.getDouble("tpPosY");
								double z = nbt.getDouble("tpPosZ");
								float yaw = nbt.getFloat("tpRotY");
								float pitch = nbt.getFloat("tpRotP");
								if (nbt.contains("dimensionKey")) {
										String dimensionKey = nbt.getString("dimensionKey");
										RegistryKey<World> newDim = RegistryHelper.getKey(Registry.DIMENSION_REGISTRY, dimensionKey);
										pos.store(newDim, x, y, z, yaw, pitch);
								} else {
										pos.store(World.OVERWORLD, x, y, z, yaw, pitch);
								}
						}

						return new WipeoutSettingsData(heal, death, pos);
				}
		}
}
