package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** World-persistent admin layer for custom/overridden knapping types. */
public final class CustomKnappingTypeData extends SavedData {

    public static final SavedDataType<CustomKnappingTypeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "custom_knapping_types"),
            CustomKnappingTypeData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    StoredKnappingType.CODEC.listOf()
                            .optionalFieldOf("types", List.of())
                            .forGetter(CustomKnappingTypeData::typesForCodec),
                    Identifier.CODEC.listOf()
                            .optionalFieldOf("disabled", List.of())
                            .forGetter(CustomKnappingTypeData::disabledForCodec)
            ).apply(instance, CustomKnappingTypeData::new)),
            null
    );

    private final Map<Identifier, StoredKnappingType> types = new LinkedHashMap<>();
    private final Set<Identifier> disabled = new LinkedHashSet<>();

    public CustomKnappingTypeData() {
    }

    private CustomKnappingTypeData(List<StoredKnappingType> types, List<Identifier> disabled) {
        for (StoredKnappingType type : types) {
            this.types.put(type.id(), type);
        }
        this.disabled.addAll(disabled);
    }

    public static CustomKnappingTypeData get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not available");
        }
        CustomKnappingTypeData data = server.getDataStorage().computeIfAbsent(TYPE);
        data.syncRuntimeState();
        return data;
    }

    public Collection<StoredKnappingType> getTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public Set<Identifier> getDisabledTypes() {
        return Collections.unmodifiableSet(disabled);
    }

    public StoredKnappingType getType(Identifier id) {
        return types.get(id);
    }

    public boolean hasOverride(Identifier id) {
        return types.containsKey(id);
    }

    public void upsertType(StoredKnappingType type) {
        types.put(type.id(), type);
        disabled.remove(type.id());
        changed();
    }

    public boolean removeOverride(Identifier id) {
        StoredKnappingType removed = types.remove(id);
        if (removed != null) {
            changed();
            return true;
        }
        return false;
    }

    public void setTypeDisabled(Identifier id, boolean isDisabled) {
        boolean stateChanged = isDisabled ? disabled.add(id) : disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void restoreOriginal(Identifier id) {
        boolean stateChanged = types.remove(id) != null;
        stateChanged |= disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void syncRuntimeState() {
        Map<Identifier, KnappingType> runtimeTypes = new LinkedHashMap<>();
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

    private List<StoredKnappingType> typesForCodec() {
        return new ArrayList<>(types.values());
    }

    private List<Identifier> disabledForCodec() {
        return new ArrayList<>(disabled);
    }
}
