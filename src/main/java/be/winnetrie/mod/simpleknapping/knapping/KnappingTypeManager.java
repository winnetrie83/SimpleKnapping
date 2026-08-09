package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Collections;
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
@SuppressWarnings("null")
public class KnappingTypeManager extends SimpleJsonResourceReloadListener {

    public static final ResourceLocation DEFAULT_TOOL_ID =
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "flint_knapping_tool");
    public static final ResourceLocation DEFAULT_TEXTURE_BLOCK = ResourceLocation.withDefaultNamespace("clay");
    public static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/clay.png");

    public static final Map<ResourceLocation, KnappingType> KNAPPING_TYPES = new LinkedHashMap<>();

    private static final Map<ResourceLocation, KnappingType> RESOURCE_TYPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, KnappingType> SERVER_TYPES = new LinkedHashMap<>();
    private static final Set<ResourceLocation> DISABLED_TYPES = new LinkedHashSet<>();

    private static final Gson GSON = new GsonBuilder().create();

    public KnappingTypeManager() {
        super(GSON, "knapping_types");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        RESOURCE_TYPES.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            ResourceLocation id = entry.getKey();
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                ResourceLocation texture = ResourceLocation.parse(json.get("texture").getAsString());
                ResourceLocation materialId = ResourceLocation.parse(json.get("material").getAsString());
                ResourceLocation toolId = json.has("tool")
                        ? ResourceLocation.parse(json.get("tool").getAsString())
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

                Item material = BuiltInRegistries.ITEM.get(materialId);
                Item tool = BuiltInRegistries.ITEM.get(toolId);
                if (material == Items.AIR || tool == Items.AIR) {
                    throw new IllegalArgumentException("Tool/material cannot be air");
                }

                ResourceLocation textureBlock = json.has("texture_block")
                        ? validTextureBlockOrDefault(ResourceLocation.parse(json.get("texture_block").getAsString()))
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

    public static void setServerState(Map<ResourceLocation, KnappingType> types, Set<ResourceLocation> disabled) {
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
        for (ResourceLocation id : DISABLED_TYPES) {
            KNAPPING_TYPES.remove(id);
        }
    }

    public static KnappingType get(ResourceLocation id) {
        return KNAPPING_TYPES.get(id);
    }

    /**
     * Returns the currently shown definition even when the type itself is disabled.
     * Server overrides take priority over resource/datapack definitions.
     */
    public static KnappingType getAnyLayer(ResourceLocation id) {
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

    public static Map<ResourceLocation, KnappingType> getResourceTypes() {
        return Collections.unmodifiableMap(RESOURCE_TYPES);
    }

    public static Map<ResourceLocation, KnappingType> getServerTypes() {
        return Collections.unmodifiableMap(SERVER_TYPES);
    }

    public static Map<ResourceLocation, KnappingType> getEffectiveTypes() {
        return Collections.unmodifiableMap(KNAPPING_TYPES);
    }

    public static Set<ResourceLocation> getDisabledTypes() {
        return Collections.unmodifiableSet(DISABLED_TYPES);
    }

    public static ResourceLocation textureForBlock(ResourceLocation blockId) {
        ResourceLocation safeBlock = validTextureBlockOrDefault(blockId);
        return ResourceLocation.fromNamespaceAndPath(
                safeBlock.getNamespace(),
                "textures/block/" + safeBlock.getPath() + ".png"
        );
    }

    public static ResourceLocation validTextureBlockOrDefault(ResourceLocation blockId) {
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            return DEFAULT_TEXTURE_BLOCK;
        }
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        return block == Blocks.AIR ? DEFAULT_TEXTURE_BLOCK : blockId;
    }

    private static ResourceLocation inferTextureBlock(ResourceLocation texture) {
        if (texture != null) {
            String path = texture.getPath();
            String prefix = "textures/block/";
            String suffix = ".png";
            if (path.startsWith(prefix) && path.endsWith(suffix) && path.length() > prefix.length() + suffix.length()) {
                String blockPath = path.substring(prefix.length(), path.length() - suffix.length());
                ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(texture.getNamespace(), blockPath);
                if (BuiltInRegistries.BLOCK.containsKey(candidate)
                        && BuiltInRegistries.BLOCK.get(candidate) != Blocks.AIR) {
                    return candidate;
                }
            }
        }
        return DEFAULT_TEXTURE_BLOCK;
    }
}
