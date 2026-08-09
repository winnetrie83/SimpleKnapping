package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Layered knapping-type registry.
 *
 * RESOURCE_TYPES comes from datapack knapping_types JSON resources.
 * SERVER_TYPES comes from the in-game admin editor and overrides matching ids.
 * DISABLED_TYPES suppresses either layer without deleting it.
 * KNAPPING_TYPES remains the public effective map for backwards compatibility.
 */
public class KnappingTypeManager extends SimpleJsonResourceReloadListener<JsonElement> {

    public static final Identifier DEFAULT_TOOL_ID =
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "flint_knapping_tool");
    public static final Identifier DEFAULT_TEXTURE_BLOCK = Identifier.withDefaultNamespace("clay");
    public static final Identifier DEFAULT_TEXTURE = Identifier.withDefaultNamespace("textures/block/clay.png");

    public static final Map<Identifier, KnappingType> KNAPPING_TYPES = new LinkedHashMap<>();

    private static final Map<Identifier, KnappingType> RESOURCE_TYPES = new LinkedHashMap<>();
    private static final Map<Identifier, KnappingType> SERVER_TYPES = new LinkedHashMap<>();
    private static final Set<Identifier> DISABLED_TYPES = new LinkedHashSet<>();

    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            json -> new Dynamic<>(JsonOps.INSTANCE, json)
    );

    public KnappingTypeManager() {
        super(JSON_CODEC, FileToIdConverter.json("knapping_types"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        RESOURCE_TYPES.clear();

        for (Map.Entry<Identifier, JsonElement> entry : jsonMap.entrySet()) {
            Identifier id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                Identifier texture = Identifier.parse(json.get("texture").getAsString());
                Identifier materialId = Identifier.parse(json.get("material").getAsString());
                Identifier toolId = json.has("tool")
                        ? Identifier.parse(json.get("tool").getAsString())
                        : DEFAULT_TOOL_ID;
                int materialCost = json.has("material_cost") ? json.get("material_cost").getAsInt() : 1;

                if (!BuiltInRegistries.ITEM.containsKey(materialId)) {
                    throw new IllegalArgumentException("Unknown material item " + materialId);
                }
                if (!BuiltInRegistries.ITEM.containsKey(toolId)) {
                    throw new IllegalArgumentException("Unknown knapping tool item " + toolId);
                }
                if (materialCost < 1 || materialCost > 99) {
                    throw new IllegalArgumentException("material_cost must be between 1 and 99");
                }

                Item material = BuiltInRegistries.ITEM.getValue(materialId);
                Item tool = BuiltInRegistries.ITEM.getValue(toolId);
                if (material == Items.AIR || tool == Items.AIR) {
                    throw new IllegalArgumentException("Tool/material cannot be air");
                }

                Identifier textureBlock = json.has("texture_block")
                        ? validTextureBlockOrDefault(Identifier.parse(json.get("texture_block").getAsString()))
                        : inferTextureBlock(texture);

                RESOURCE_TYPES.put(id, new KnappingType(
                        id,
                        tool,
                        material,
                        materialCost,
                        texture,
                        textureBlock
                ));
                SimpleKnapping.LOGGER.info("Loaded knapping type: {}", id);
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.error("Could not load knapping type {}", id, exception);
            }
        }

        rebuildEffectiveTypes();
    }

    public static void setServerState(Map<Identifier, KnappingType> types, Set<Identifier> disabled) {
        SERVER_TYPES.clear();
        SERVER_TYPES.putAll(types);
        DISABLED_TYPES.clear();
        DISABLED_TYPES.addAll(disabled);
        rebuildEffectiveTypes();
    }

    private static void rebuildEffectiveTypes() {
        KNAPPING_TYPES.clear();
        KNAPPING_TYPES.putAll(RESOURCE_TYPES);
        KNAPPING_TYPES.putAll(SERVER_TYPES);
        for (Identifier id : DISABLED_TYPES) {
            KNAPPING_TYPES.remove(id);
        }
    }

    public static KnappingType get(Identifier id) {
        return KNAPPING_TYPES.get(id);
    }

    /**
     * Returns the currently shown definition even when the type itself is disabled.
     * Server overrides take priority over resource/datapack definitions.
     */
    public static KnappingType getAnyLayer(Identifier id) {
        KnappingType server = SERVER_TYPES.get(id);
        return server != null ? server : RESOURCE_TYPES.get(id);
    }

    /** Compatibility helper for older call sites. New interactions should match tool + material. */
    public static KnappingType getByMaterial(Item material) {
        for (KnappingType type : KNAPPING_TYPES.values()) {
            if (type.material() == material) {
                return type;
            }
        }
        return null;
    }

    public static KnappingType getByToolAndMaterial(Item tool, Item material) {
        for (KnappingType type : KNAPPING_TYPES.values()) {
            if (type.tool() == tool && type.material() == material) {
                return type;
            }
        }
        return null;
    }

    public static Map<Identifier, KnappingType> getResourceTypes() {
        return Collections.unmodifiableMap(RESOURCE_TYPES);
    }

    public static Map<Identifier, KnappingType> getServerTypes() {
        return Collections.unmodifiableMap(SERVER_TYPES);
    }

    public static Map<Identifier, KnappingType> getEffectiveTypes() {
        return Collections.unmodifiableMap(KNAPPING_TYPES);
    }

    public static Set<Identifier> getDisabledTypes() {
        return Collections.unmodifiableSet(DISABLED_TYPES);
    }

    public static Identifier textureForBlock(Identifier blockId) {
        Identifier safeBlock = validTextureBlockOrDefault(blockId);
        return Identifier.fromNamespaceAndPath(
                safeBlock.getNamespace(),
                "textures/block/" + safeBlock.getPath() + ".png"
        );
    }

    public static Identifier validTextureBlockOrDefault(Identifier blockId) {
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            return DEFAULT_TEXTURE_BLOCK;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(blockId);
        return block == Blocks.AIR ? DEFAULT_TEXTURE_BLOCK : blockId;
    }

    private static Identifier inferTextureBlock(Identifier texture) {
        if (texture != null) {
            String path = texture.getPath();
            String prefix = "textures/block/";
            String suffix = ".png";
            if (path.startsWith(prefix) && path.endsWith(suffix) && path.length() > prefix.length() + suffix.length()) {
                String blockPath = path.substring(prefix.length(), path.length() - suffix.length());
                Identifier candidate = Identifier.fromNamespaceAndPath(texture.getNamespace(), blockPath);
                if (BuiltInRegistries.BLOCK.containsKey(candidate)
                        && BuiltInRegistries.BLOCK.getValue(candidate) != Blocks.AIR) {
                    return candidate;
                }
            }
        }
        return DEFAULT_TEXTURE_BLOCK;
    }
}
