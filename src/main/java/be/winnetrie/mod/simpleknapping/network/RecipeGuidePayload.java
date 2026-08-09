package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> client payload used by the public knapping recipe guide. */
public record RecipeGuidePayload(String kind, String json) implements CustomPacketPayload {
    public static final Type<RecipeGuidePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "recipe_guide")
    );

    public static final StreamCodec<ByteBuf, RecipeGuidePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            RecipeGuidePayload::kind,
            ByteBufCodecs.STRING_UTF8,
            RecipeGuidePayload::json,
            RecipeGuidePayload::new
    );

    public static RecipeGuidePayload snapshot(String json) {
        return new RecipeGuidePayload("snapshot", json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
