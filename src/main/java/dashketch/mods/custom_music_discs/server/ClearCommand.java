package dashketch.mods.custom_music_discs.server;

import dashketch.mods.custom_music_discs.Custom_music_discs;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Custom_music_discs.MODID)
public class ClearCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ClearUploads")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            // Get the server and source from the context
                            MinecraftServer server = context.getSource().getServer();
                            CommandSourceStack source = context.getSource();

                            clearUploads(server, source);
                            return 1;
                        })
        );
    }

    public static void clearUploads(MinecraftServer server, CommandSourceStack source) {
        // Get the server path without relying on a player instance
        Path serverPath = server.getWorldPath(LevelResource.ROOT).resolve("config/uploaded_music");

        try {
            if (Files.exists(serverPath)) {
                try (Stream<Path> files = Files.list(serverPath)) {
                    files.forEach(file -> {
                        try {
                            if (!Files.isDirectory(file)) {
                                Files.delete(file);
                            }
                        } catch (IOException e) {
                            Custom_music_discs.LOGGER.error("Failed to delete file: {}", file, e);
                        }
                    });
                }
                // Send success message to the person who ran the command
                source.sendSuccess(() -> Component.literal("§aAll uploaded music files cleared!"), true);
            } else {
                source.sendFailure(Component.literal("§cUpload directory does not exist."));
            }
        } catch (IOException e) {
            Custom_music_discs.LOGGER.error("Could not access server music path", e);
            source.sendFailure(Component.literal("§cAn error occurred while clearing files. Check logs."));
        }
    }
}