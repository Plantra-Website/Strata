package com.strata.server;

import com.strata.blocks.Blocks;

public final class Recipe {
   public final int[] input;  
   public final int outputId;
   public final int outputCount;

   public Recipe(int[] input, int outputId, int outputCount) {
      this.input = input.clone();
      this.outputId = outputId;
      this.outputCount = outputCount;
   }

   public boolean matches(int[] grid) {
      for (int i = 0; i < 4; i++) {
         int needed = input[i];
         if (needed == 0) {
            continue;
         }
         if (grid[i] != needed) {
            return false;
         }
      }
      return true;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("Recipe(");
      for (int i = 0; i < 4; i++) {
         sb.append(input[i]);
         if (i < 3) {
            sb.append(",");
         }
      }
      sb.append(")->").append(Blocks.byId(outputId) != null ? Blocks.byId(outputId).getClass().getSimpleName() : "item#" + outputId)
         .append("x").append(outputCount);
      return sb.toString();
   }
}

