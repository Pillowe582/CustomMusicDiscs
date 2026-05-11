package dashketch.mods.custom_music_discs;

import com.mojang.logging.LogUtils;
import dashketch.mods.custom_music_discs.item.ModItems;
import dashketch.mods.custom_music_discs.network.MusicUploadPayload;
import dashketch.mods.custom_music_discs.network.ServerPayloadHandler;
import dashketch.mods.custom_music_discs.server.ModConfigs;
import dashketch.mods.custom_music_discs.server.recipeGen;
import dashketch.mods.custom_music_discs.sound.ModSounds;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static dashketch.mods.custom_music_discs.item.ModItems.BLANK_DISC;
import static dashketch.mods.custom_music_discs.item.ModItems.DISC_BURNER;

@Mod(Custom_music_discs.MODID)
public class Custom_music_discs {
    public static final String MODID = "custom_music_discs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Custom_music_discs(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);

        CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::registerNetworking);

        modEventBus.addListener(this::gatherData);

        modContainer.registerConfig(ModConfig.Type.SERVER, ModConfigs.SPEC);

        modContainer.registerConfig(ModConfig.Type.CLIENT, ModConfigs.SPEC);
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);

    // Registering the Creative Tab
    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CUSTOM_DISCS_TAB = CREATIVE_MODE_TABS.register(
            "custom_music_discs",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.custom_music_discs"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> BLANK_DISC.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(BLANK_DISC.get());
                        output.accept(DISC_BURNER.get());
                    }).build());

    private void registerNetworking(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                MusicUploadPayload.TYPE,
                MusicUploadPayload.CODEC,
                ServerPayloadHandler::handleData);
    }

    private void gatherData(final GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new recipeGen(output, lookupProvider));
    }
}