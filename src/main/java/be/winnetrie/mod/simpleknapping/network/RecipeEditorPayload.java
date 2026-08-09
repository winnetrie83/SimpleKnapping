package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * One bidirectional payload keeps registration simple and side-safe:
 * - server -> client: kind=snapshot
 * - client -> server: kind=action
 */
public record RecipeEditorPayload(String kind, String json) implements CustomPacketPayload {
    public static final Type<RecipeEditorPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "recipe_editor")
    );

    public static final StreamCodec<ByteBuf, RecipeEditorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RecipeEditorPayload::kind,
            ByteBufCodecs.STRING_UTF8,
            RecipeEditorPayload::json,
            RecipeEditorPayload::new
    );

    public static RecipeEditorPayload snapshot(String json) {
        return new RecipeEditorPayload("snapshot", json);
    }

    public static RecipeEditorPayload action(String json) {
        return new RecipeEditorPayload("action", json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
