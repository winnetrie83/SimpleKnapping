package be.winnetrie.mod.simpleknapping.knapping;

/** Pure pattern rules shared by datapacks and runtime validation; no client/server dependency. */
public final class KnappingPatternRules {
    public static final int SIZE = 5;
    private KnappingPatternRules() { }

    public static void validate(String[] rows) {
        if (rows == null || rows.length != SIZE) {
            throw new IllegalArgumentException("Knapping pattern must have exactly five rows");
        }
        boolean containsMaterial = false;
        for (String row : rows) {
            if (row == null || row.length() != SIZE) {
                throw new IllegalArgumentException("Knapping pattern rows must each contain five characters");
            }
            for (int c = 0; c < SIZE; c++) {
                char symbol = row.charAt(c);
                if (symbol != 'X' && symbol != ' ') {
                    throw new IllegalArgumentException("Only 'X' and space are allowed in knapping patterns");
                }
                if (symbol == 'X') containsMaterial = true;
            }
        }
        if (!containsMaterial) {
            throw new IllegalArgumentException("Knapping pattern must retain at least one tile");
        }
    }

    public static boolean matches(String[] left, String[] right) {
        if (left == null || right == null || left.length != SIZE || right.length != SIZE) return false;
        for (int i = 0; i < SIZE; i++) {
            if (left[i] == null || !left[i].equals(right[i])) return false;
        }
        return true;
    }
}
