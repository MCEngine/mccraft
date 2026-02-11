package io.github.mcengine.mccraft.common.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Constants for the MCCraft virtual crafting GUI layout.
 */
public final class GUIConstants {

    private GUIConstants() {}

    /** Total GUI size (54 slots = 6 rows of 9). */
    public static final int GUI_SIZE = 54;

    /** Recipe input slots: 3x3 grid at rows 2-4, columns 2-4. */
    public static final int[] RECIPE_SLOTS = {11, 12, 13, 20, 21, 22, 29, 30, 31};

    /** Result output slot. */
    public static final int RESULT_SLOT = 24;

    /** Title prefix for the crafting GUI. */
    public static final String CRAFTING_GUI_TITLE = "MCCraft";

    /** Title prefix for the editor list GUI. */
    public static final String EDITOR_LIST_TITLE = "MCCraft Editor";

    /** Set of recipe slot indices for quick lookup. */
    public static final Set<Integer> RECIPE_SLOT_SET;

    static {
        Set<Integer> set = new HashSet<>();
        for (int slot : RECIPE_SLOTS) {
            set.add(slot);
        }
        RECIPE_SLOT_SET = Collections.unmodifiableSet(set);
    }

    /**
     * Checks if a slot index is a recipe input slot.
     *
     * @param slot the slot index
     * @return true if it is a recipe slot
     */
    public static boolean isRecipeSlot(int slot) {
        return RECIPE_SLOT_SET.contains(slot);
    }
}
