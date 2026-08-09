package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.guide.RecipeGuideSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Public, read-only recipe guide networking. No admin permission is required. */
public final class RecipeGuideNetwork {
    private RecipeGuideNetwork() {
    }

    static void register(PayloadRegistrar registrar) {
        // Bidirectional registration keeps this on the same established payload
        // path as the editors. The client never sends this payload back.
        registrar.playBidirectional(
                RecipeGuidePayload.TYPE,
                RecipeGuidePayload.STREAM_CODEC,
                RecipeGuideNetwork::handleServerPayload
        );
    }

    public static void openGuide(ServerPlayer player) {
        RecipeGuideSnapshot snapshot = RecipeGuideSnapshot.fromServer();
        PacketDistributor.sendToPlayer(player, RecipeGuidePayload.snapshot(snapshot.toJson()));
    }

    private static void handleServerPayload(RecipeGuidePayload payload, IPayloadContext context) {
        // Read-only server -> client feature. Ignore unexpected client packets.
    }
}
