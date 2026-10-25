package be.winnetrie.mod.simpleknapping.menu;

/** Server-side knapping UI access rules, independent of client preferences. */
public final class KnappingAccessPolicy {
    private KnappingAccessPolicy() { }

    /** Creative always has hints. Other game modes require the server config opt-in. */
    public static boolean mayPreview(boolean creative, boolean survivalPreviewEnabled) {
        return creative || survivalPreviewEnabled;
    }

    /** A reset must never be usable in survival, even through a forged menu click. */
    public static boolean mayReset(boolean creative) {
        return creative;
    }
}
