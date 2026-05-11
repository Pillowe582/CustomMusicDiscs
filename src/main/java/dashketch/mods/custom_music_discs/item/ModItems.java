package dashketch.mods.custom_music_discs.item;

import dashketch.mods.custom_music_discs.Custom_music_discs;
import dashketch.mods.custom_music_discs.client.item.DiscBurnerItem;
import dashketch.mods.custom_music_discs.sound.ModSounds;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    // Create the register
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Custom_music_discs.MODID);

    // Register the Blank Disc
    public static final DeferredItem<Item> BLANK_DISC = ITEMS.register("blank_custom_disc",
            () -> new Item(new Item.Properties().stacksTo(64).jukeboxPlayable(ModSounds.BLANK_SONG)));

    // Register the Disc Burner
    public static final DeferredItem<DiscBurnerItem> DISC_BURNER = ITEMS.register("disc_burner",
            () -> new DiscBurnerItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}