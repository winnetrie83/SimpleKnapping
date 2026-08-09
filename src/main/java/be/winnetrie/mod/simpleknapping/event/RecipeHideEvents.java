package be.winnetrie.mod.simpleknapping.event;

/**
 * Legacy placeholder kept so older source overlays merge cleanly.
 *
 * Recipe JSONs are intentionally no longer removed during resource reload.
 * Wooden/stone tier availability is enforced at runtime so the admin settings
 * take effect immediately without /reload. This class is not registered.
 */
@Deprecated(forRemoval = true)
public final class RecipeHideEvents {
    private RecipeHideEvents() {
    }
}
