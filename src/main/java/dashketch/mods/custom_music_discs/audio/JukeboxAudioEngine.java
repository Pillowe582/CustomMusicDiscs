package dashketch.mods.custom_music_discs.audio;

import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.Player;
import net.minecraft.core.BlockPos;
import org.essentials.custom_background_music.MusicMuter;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JukeboxAudioEngine {
    private static final JukeboxAudioEngine INSTANCE = new JukeboxAudioEngine();
    private final Map<BlockPos, TrackedJukebox> trackedJukeboxes = new HashMap<>();
    private Player player;
    private Thread musicThread;
    private BlockPos activePos;
    private String activeTrackKey; // current playing track
    private float volume = 1.0f;

    public static JukeboxAudioEngine getInstance() {
        return INSTANCE;
    }

    public synchronized void upsertInstance(BlockPos pos, String trackKey, File musicFile) {
        // store the info of a custom player instance
        if (pos == null || trackKey == null || musicFile == null) {
            return;
        }
        trackedJukeboxes.put(pos.immutable(), new TrackedJukebox(trackKey, musicFile));
    }

    public synchronized void removeInstance(BlockPos pos) {
        if (pos == null) {
            return;
        }
        BlockPos immutablePos = pos.immutable();
        trackedJukeboxes.remove(immutablePos);

        if (immutablePos.equals(activePos)) {
            stop();
        }
    }

    public synchronized Set<BlockPos> getTrackedPositions() {
        return new HashSet<>(trackedJukeboxes.keySet());
    }

    public synchronized BlockPos getActivePos() {
        return activePos;
    }

    public synchronized void clearInstancesAndStop() {
        trackedJukeboxes.clear();
        stop();
    }

    public synchronized void activate(BlockPos pos) {
        if (pos == null) {
            return;
        }

        TrackedJukebox next = trackedJukeboxes.get(pos);
        if (next == null) {
            return;
        }

        // If track is unchanged, preserve playback and only move the logical source
        // position.
        if (isPlaying() && isSameTrack(next.trackKey)) {
            activePos = pos.immutable();
            return;
        }

        playInternal(next);
        activePos = pos.immutable();
        activeTrackKey = next.trackKey;
    }

    public synchronized boolean isPlaying() {
        return musicThread != null && musicThread.isAlive();
    }

    public synchronized boolean isSameTrack(String trackKey) {
        return activeTrackKey != null && activeTrackKey.equals(trackKey) && isPlaying();
    }

    @SuppressWarnings("unused")
    public float getVolume() {
        return this.volume;
    }

    public synchronized void setVolume(float targetVolume) {
        this.volume = Math.clamp(targetVolume, 0.0f, 1.0f);
        if (player != null) {
            try {
                Field deviceField = Player.class.getDeclaredField("audio");
                deviceField.setAccessible(true);
                AudioDevice device = (AudioDevice) deviceField.get(player);

                if (device instanceof JavaSoundAudioDevice jsDevice) {
                    Field sourceField = JavaSoundAudioDevice.class.getDeclaredField("source");
                    sourceField.setAccessible(true);
                    SourceDataLine source = (SourceDataLine) sourceField.get(jsDevice);

                    if (source != null && source.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gainControl = (FloatControl) source.getControl(FloatControl.Type.MASTER_GAIN);
                        float dB = (float) (Math.log(this.volume <= 0.0f ? 1.0e-4f : this.volume) / Math.log(10.0f)
                                * 20.0f);
                        gainControl.setValue(dB);
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void stop() {
        stopPlayer(true);
        activePos = null;
        activeTrackKey = null;
    }

    private void playInternal(TrackedJukebox jukebox) {
        
        if (!jukebox.musicFile.exists()) {
            return;
        }

        MusicMuter.muteMinecraftMusic();
        stopPlayer(false);

        musicThread = new Thread(() -> {
            try (FileInputStream fis = new FileInputStream(jukebox.musicFile)) {
                synchronized (this) {
                    player = new Player(new BufferedInputStream(fis));
                    setVolume(volume);
                }
                player.play();
            } catch (Exception e) {
                System.out.println("Jukebox stream closed.");
            }
        }, "custom-music-disc-player");
        musicThread.setDaemon(true);
        musicThread.start();
    }

    private synchronized void stopPlayer(boolean unmuteVanillaMusic) {
        if (player != null) {
            player.close();
            player = null;
        }
        if (musicThread != null) {
            musicThread.interrupt();
            musicThread = null;
        }
        if (unmuteVanillaMusic) {
            MusicMuter.unmuteMinecraftMusic();
        }
    }

    private record TrackedJukebox(String trackKey, File musicFile) {
    }
}