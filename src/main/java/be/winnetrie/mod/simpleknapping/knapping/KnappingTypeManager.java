package be.winnetrie.mod.simpleknapping.knapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import java.util.HashMap;
import java.util.Map;

public class KnappingTypeManager extends SimpleJsonResourceReloadListener<JsonElement> {

    public static final Map<Identifier, KnappingType> KNAPPING_TYPES = new HashMap<>();

    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
                dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
                json -> new Dynamic<>(JsonOps.INSTANCE, json)
        );

    public KnappingTypeManager() {
        super(JSON_CODEC, FileToIdConverter.json("knapping_types"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {

        KNAPPING_TYPES.clear();

        for (Map.Entry<Identifier, JsonElement> entry : jsonMap.entrySet()) {

            Identifier id = entry.getKey();
            JsonObject json = entry.getValue().getAsJsonObject();

            Identifier texture = Identifier.parse(json.get("texture").getAsString());
            Identifier materialId = Identifier.parse(json.get("material").getAsString());

            Item material = BuiltInRegistries.ITEM.getValue(materialId);

            int materialCost = json.has("material_cost") ? json.get("material_cost").getAsInt() : 1;

            KNAPPING_TYPES.put(id, new KnappingType(id, material, materialCost, texture));

            System.out.println("Loaded knapping type: " + id);
        }
    }

    public static KnappingType get(Identifier id) {
        return KNAPPING_TYPES.get(id);
    }

    public static KnappingType getByMaterial(Item material) {

        for (KnappingType type : KNAPPING_TYPES.values()) {
            if (type.material() == material) {
                return type;
            }
        }

        return null;
    }
}