package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Dedicated payload for admin settings plus lightweight tier-state sync. */
@SuppressWarnings("null")
public record SettingsPayload(String kind, String json) implements CustomPacketPayload {
    public static final Type<SettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "settings")
    );

    public static final StreamCodec<ByteBuf, SettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SettingsPayload::kind,
            ByteBufCodecs.STRING_UTF8,
            SettingsPayload::json,
            SettingsPayload::new
    );

    public static SettingsPayload snapshot(String json) {
        return new SettingsPayload("snapshot", json);
    }

    /** Silent server -> client state sync; never opens the admin GUI. */
    public static SettingsPayload state(String json) {
        return new SettingsPayload("state", json);
    }

    public static SettingsPayload action(String json) {
        return new SettingsPayload("action", json);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
