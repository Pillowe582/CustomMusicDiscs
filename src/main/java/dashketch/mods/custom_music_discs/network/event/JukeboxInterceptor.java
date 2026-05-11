package dashketch.mods.custom_music_discs.network.event;

import dashketch.mods.custom_music_discs.audio.JukeboxAudioEngine;
import dashketch.mods.custom_music_discs.server.ModConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.essentials.custom_background_music.AudioManager;

import java.io.File;

@EventBusSubscriber(modid = "custom_music_discs") // bus has been removed after 1.21.1
public class JukeboxInterceptor {
    static JukeboxAudioEngine engine = JukeboxAudioEngine.getInstance();
    static AudioManager am = AudioManager.getInstance();
    private static BlockPos playingPos = null;

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
                    engine.stop();
                    playingPos = null; // Clear position safely
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
                    engine.stop();
                    File musicFile = resolveMusicFile(songName);
                    engine.play(musicFile);
                    playingPos = pos;
                    event.getEntity().displayClientMessage(
                            Component.literal("§bNow playing: " + songName.replace(".mp3", "")), true);
                }

                // If this only happens on the server, the ClientTickEvent fires before the
                // client receives the packet, sees HAS_RECORD is false, and instantly kills the
                // music.
                if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
                    jukebox.setTheItem(stack.copyWithCount(1)); // dont copy the whole stack
                    level.setBlock(pos, state.setValue(JukeboxBlock.HAS_RECORD, true), 3);

                    if (!level.isClientSide && !event.getEntity().isCreative()) {
                        stack.shrink(1);
                    }
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || playingPos == null)
            return;

        BlockState state = mc.level.getBlockState(playingPos);

        if (!state.is(Blocks.JUKEBOX) || !state.getValue(JukeboxBlock.HAS_RECORD)) {
            engine.stop();
            playingPos = null;
            return;
        }

        if (!engine.isPlaying()) {
            playingPos = null;
            return;
        }

        // Distance Check / Volume Fading
        if (mc.player != null && engine.isPlaying()) {
            double dx = mc.player.getX() - (playingPos.getX() + 0.5);
            double dy = mc.player.getY() - (playingPos.getY() + 0.5);
            double dz = mc.player.getZ() - (playingPos.getZ() + 0.5);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Fetch the user's vanilla volume settings
            float sliderMultiplier = dashketch.mods.custom_music_discs.client.override.volume_slider.getJukeboxVolume();

            if (ModConfigs.SPEC.isLoaded() && ModConfigs.JUKEBOX_RANGE_BOOL.get()) {
                double maxDistance = ModConfigs.JUKEBOX_RANGE.get() + 16.0;
                double ratio = Math.clamp(distance / maxDistance, 0.0, 1.0);

                // Calculate distance-based volume and multiply by slider
                float volume = (float) Math.pow(1.0 - ratio, 2) * sliderMultiplier;
                engine.setVolume(volume);

            } else {
                double maxDistance = 64.0 + 16.0;
                double ratio = Math.clamp(distance / maxDistance, 0.0, 1.0);

                // Calculate distance-based volume and multiply by slider
                float volume = (float) Math.pow(1.0 - ratio, 2) * sliderMultiplier;
                engine.setVolume(volume);
            }
        }
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