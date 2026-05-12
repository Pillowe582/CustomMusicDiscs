package dashketch.mods.custom_music_discs.network.event;

import dashketch.mods.custom_music_discs.audio.JukeboxAudioEngine;
import dashketch.mods.custom_music_discs.server.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.essentials.custom_background_music.AudioManager;

import java.io.File;

@EventBusSubscriber(modid = "custom_music_discs") // bus has been removed after 1.21.1
public class JukeboxInterceptor {
    static JukeboxAudioEngine engine = JukeboxAudioEngine.getInstance();
    static AudioManager am = AudioManager.getInstance();

    @SubscribeEvent
    public static void onJukeboxRightClick(PlayerInteractEvent.RightClickBlock event) {

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack stack = event.getItemStack();
        BlockState state = level.getBlockState(pos);

        if (state.is(Blocks.JUKEBOX)) {
            // 1. EJECTION LOGIC
            if (state.getValue(JukeboxBlock.HAS_RECORD)) {
                if (level.isClientSide) {
                    engine.removeInstance(pos);
                }
                // Stop processing here. Let vanilla handle the ejection.
                // Do NOT fall through to the insertion logic.
                return;
            }

            // 2. INSERTION LOGIC (Jukebox is definitively empty)
            if (level.isClientSide) {
                am.stop();
            }

            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.copyTag().contains("SelectedSong")) {
                String songName = customData.copyTag().getString("SelectedSong");

                if (level.isClientSide) {
                    File musicFile = resolveMusicFile(songName);
                    engine.upsertInstance(pos, songName, musicFile);
                    if (engine.getActivePos() == null) {
                        engine.activate(pos);
                    }

                    // This message will be covered by vanilla message,
                    // i havent found a way to block it
                    event.getEntity().displayClientMessage(
                            Component.literal("§bNow playing: " + songName.replace(".mp3", "")), true);
                }

            }
        }
    }

    @SubscribeEvent
    public static void onSoundPlay(VanillaGameEvent event) {
        var holder = event.getVanillaEvent();
        if (!holder.equals(GameEvent.JUKEBOX_PLAY)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = BlockPos.containing(event.getEventPosition());

        if (!(level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox)) {
            return;
        }

        ItemStack record = jukebox.getTheItem();
        if (record.isEmpty() || record.get(DataComponents.CUSTOM_DATA) == null
                || !record.get(DataComponents.CUSTOM_DATA).copyTag().contains("SelectedSong")) {
            return;
        }

        String songName = record.get(DataComponents.CUSTOM_DATA).copyTag().getString("SelectedSong");
        File musicFile = resolveMusicFile(songName);
        engine.upsertInstance(pos, songName, musicFile);

    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            engine.clearInstancesAndStop();
            return;
        }

        for (BlockPos pos : engine.getTrackedPositions()) {
            BlockState trackedState = level.getBlockState(pos);
            if (!trackedState.is(Blocks.JUKEBOX) || !trackedState.getValue(JukeboxBlock.HAS_RECORD)) {
                engine.removeInstance(pos);
            }

        }

        if (engine.getTrackedPositions().isEmpty()) {
            if (engine.isPlaying()) {
                engine.stop();
            }
            return;
        }
        BlockPos nearestPos = findNearestTrackedPos(player);
        if (nearestPos == null) {
            return;
        }

        engine.activate(nearestPos);
        // If the current track finished naturally, remove the source and fall through
        // to next nearest.
        BlockPos activePos = engine.getActivePos();
        if (activePos == null) {
            return;
        }

        double dx = player.getX() - (activePos.getX() + 0.5);
        double dy = player.getY() - (activePos.getY() + 0.5);
        double dz = player.getZ() - (activePos.getZ() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float sliderMultiplier = dashketch.mods.custom_music_discs.client.override.volume_slider.getJukeboxVolume();
        double maxDistance = getMaxDistance();
        double ratio = Math.clamp(distance / maxDistance, 0.0, 1.0);

        float volume = (float) Math.pow(1.0 - ratio, 2) * sliderMultiplier;
        engine.setVolume(volume);
    }

    private static BlockPos findNearestTrackedPos(Player player) {
        BlockPos currentActive = engine.getActivePos();
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (BlockPos pos : engine.getTrackedPositions()) {
            double distSq = player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = pos;
            } else if (distSq == nearestDistSq && currentActive != null && pos.equals(currentActive)) {
                // Keep the current source on exact tie to avoid rapid source flapping.
                nearest = currentActive;
            }
        }

        return nearest;
    }

    private static double getMaxDistance() {
        if (ModConfigs.SPEC.isLoaded() && ModConfigs.JUKEBOX_RANGE_BOOL.get()) {
            return ModConfigs.JUKEBOX_RANGE.get() + 16.0;
        }
        return 64.0 + 16.0;
    }

    private static File resolveMusicFile(String fileName) {
        Minecraft mc = Minecraft.getInstance();
        File mcDir = mc.gameDirectory;
        if (mc.getSingleplayerServer() != null) {
            String worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
            return new File(mcDir, "saves/" + worldName + "/config/uploaded_music/" + fileName);
        }
        return new File(mcDir, "config/uploaded_music/" + fileName);
    }
}