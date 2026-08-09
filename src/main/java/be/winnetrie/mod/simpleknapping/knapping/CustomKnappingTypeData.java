package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** World-persistent admin layer for custom/overridden knapping types. */
@SuppressWarnings("null")
public final class CustomKnappingTypeData extends SavedData {
    private static final String DATA_NAME = "simpleknapping_custom_knapping_types";
    private static final SavedData.Factory<CustomKnappingTypeData> FACTORY =
            new SavedData.Factory<>(CustomKnappingTypeData::new, CustomKnappingTypeData::load);

    private final Map<ResourceLocation, StoredKnappingType> types = new LinkedHashMap<>();
    private final Set<ResourceLocation> disabled = new LinkedHashSet<>();

    public CustomKnappingTypeData() {
    }

    public static CustomKnappingTypeData get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not available");
        }
        CustomKnappingTypeData data = server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.syncRuntimeState();
        return data;
    }

    private static CustomKnappingTypeData load(CompoundTag tag, HolderLookup.Provider registries) {
        CustomKnappingTypeData data = new CustomKnappingTypeData();

        ListTag typeTags = tag.getList("types", Tag.TAG_COMPOUND);
        for (int i = 0; i < typeTags.size(); i++) {
            try {
                CompoundTag typeTag = typeTags.getCompound(i);
                StoredKnappingType type = new StoredKnappingType(
                        ResourceLocation.parse(typeTag.getString("id")),
                        ResourceLocation.parse(typeTag.getString("tool")),
                        ResourceLocation.parse(typeTag.getString("material")),
                        typeTag.contains("material_cost", Tag.TAG_INT) ? typeTag.getInt("material_cost") : 1,
                        typeTag.contains("texture_block", Tag.TAG_STRING)
                                ? ResourceLocation.parse(typeTag.getString("texture_block"))
                                : KnappingTypeManager.DEFAULT_TEXTURE_BLOCK
                );
                data.types.put(type.id(), type);
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.warn("Skipping invalid stored knapping type entry {}", i, exception);
            }
        }

        ListTag disabledTags = tag.getList("disabled", Tag.TAG_STRING);
        for (int i = 0; i < disabledTags.size(); i++) {
            try {
                data.disabled.add(ResourceLocation.parse(disabledTags.getString(i)));
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.warn("Skipping invalid disabled knapping type id at index {}", i, exception);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag typeTags = new ListTag();
        for (StoredKnappingType type : types.values()) {
            CompoundTag typeTag = new CompoundTag();
            typeTag.putString("id", type.id().toString());
            typeTag.putString("tool", type.tool().toString());
            typeTag.putString("material", type.material().toString());
            typeTag.putInt("material_cost", type.materialCost());
            typeTag.putString("texture_block", type.textureBlock().toString());
            typeTags.add(typeTag);
        }
        tag.put("types", typeTags);

        ListTag disabledTags = new ListTag();
        for (ResourceLocation id : disabled) {
            disabledTags.add(StringTag.valueOf(id.toString()));
        }
        tag.put("disabled", disabledTags);
        return tag;
    }

    public Collection<StoredKnappingType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Set<ResourceLocation> getDisabledTypes() {
        return Collections.unmodifiableSet(disabled);
    }

    public StoredKnappingType getType(ResourceLocation id) {
        return types.get(id);
    }

    public boolean hasOverride(ResourceLocation id) {
        return types.containsKey(id);
    }

    public void upsertType(StoredKnappingType type) {
        types.put(type.id(), type);
        disabled.remove(type.id());
        changed();
    }

    public boolean removeOverride(ResourceLocation id) {
        StoredKnappingType removed = types.remove(id);
        if (removed != null) {
            changed();
            return true;
        }
        return false;
    }

    public void setTypeDisabled(ResourceLocation id, boolean isDisabled) {
        boolean stateChanged = isDisabled ? disabled.add(id) : disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void restoreOriginal(ResourceLocation id) {
        boolean stateChanged = types.remove(id) != null;
        stateChanged |= disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void syncRuntimeState() {
        Map<ResourceLocation, KnappingType> runtimeTypes = new LinkedHashMap<>();
        for (StoredKnappingType stored : types.values()) {
            try {
                runtimeTypes.put(stored.id(), stored.toRuntimeType());
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.error("Could not activate stored knapping type {}", stored.id(), exception);
            }
        }
        KnappingTypeManager.setServerState(runtimeTypes, disabled);
    }

    private void changed() {
        setDirty();
        syncRuntimeState();
    }
}
