package com.strata.server;

import com.strata.blocks.Blocks;
import java.util.ArrayList;
import java.util.List;

public final class CraftingManager {
   private static final List<Recipe> recipes = new ArrayList<>();
   private static boolean initialized = false;

   public static void register(Recipe recipe) {
      recipes.add(recipe);
   }

   public static Recipe findMatch(int[] grid) {
      for (Recipe r : recipes) {
         if (r.matches(grid)) {
            return r;
         }
      }
      return null;
   }

   public static synchronized void init() {
      if (initialized) {
         return;
      }
      initialized = true;
      register(new Recipe(new int[]{Blocks.WOOD_ID, 0, 0, 0}, Blocks.PLANKS_ID, 4));
      register(new Recipe(new int[]{0, Blocks.WOOD_ID, 0, 0}, Blocks.PLANKS_ID, 4));
      register(new Recipe(new int[]{0, 0, Blocks.WOOD_ID, 0}, Blocks.PLANKS_ID, 4));
      register(new Recipe(new int[]{0, 0, 0, Blocks.WOOD_ID}, Blocks.PLANKS_ID, 4));
      register(new Recipe(new int[]{Blocks.PLANKS_ID, Blocks.PLANKS_ID, 0, 0}, Blocks.STICK_ID, 4));
      register(new Recipe(new int[]{0, 0, Blocks.PLANKS_ID, Blocks.PLANKS_ID}, Blocks.STICK_ID, 4));
      register(new Recipe(new int[]{Blocks.STICK_ID, Blocks.COAL_ID, 0, 0}, Blocks.TORCH_ID, 4));
      register(new Recipe(new int[]{Blocks.COAL_ID, Blocks.STICK_ID, 0, 0}, Blocks.TORCH_ID, 4));
      register(new Recipe(new int[]{0, 0, Blocks.STICK_ID, Blocks.COAL_ID}, Blocks.TORCH_ID, 4));
      register(new Recipe(new int[]{0, 0, Blocks.COAL_ID, Blocks.STICK_ID}, Blocks.TORCH_ID, 4));
   }
}

