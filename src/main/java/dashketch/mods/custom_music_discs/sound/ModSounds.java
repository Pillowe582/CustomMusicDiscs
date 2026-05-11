package dashketch.mods.custom_music_discs.sound;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import dashketch.mods.custom_music_discs.Custom_music_discs;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister
            .create(BuiltInRegistries.SOUND_EVENT, "custom_music_discs");

    public static ResourceLocation RL_BLANK_SONG = ResourceLocation.fromNamespaceAndPath("custom_music_discs",
            "sounds/blank_song");

    public static final DeferredHolder<SoundEvent, SoundEvent> BLANK_SOUND = SOUND_EVENTS.register(
            "blank_song", // must match the resource location on the next line
            () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath("custom_music_discs", "blank_song")));

    // ResourceKey for the data-driven JukeboxSong entry
    // (data/custom_music_discs/jukebox_song/blank_song.json)
    public static final ResourceKey<JukeboxSong> BLANK_SONG = ResourceKey.create(Registries.JUKEBOX_SONG,
            ResourceLocation.fromNamespaceAndPath(Custom_music_discs.MODID, "blank_song"));

    public static DeferredHolder<SoundEvent, SoundEvent> register(String name, Supplier<SoundEvent> supplier) {
        return SOUND_EVENTS.register(name, supplier);
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    public static String getJukeboxSongTranslateKey(String name) {
        return "jukebox_song." + Custom_music_discs.MODID + "." + name;
    }

}
