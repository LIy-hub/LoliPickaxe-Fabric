package com.liymod.tool;

import com.liymod.LiyMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModToolMaterials {
    private static final TagKey<Block> INCORRECT_FOR_LOLI_TOOL = TagKey.of(
            RegistryKeys.BLOCK,
            Identifier.of(LiyMod.MOD_ID, "incorrect_for_loli_tool")
    );
    private static final TagKey<Item> LOLI_REPAIR_MATERIALS = TagKey.of(
            RegistryKeys.ITEM,
            Identifier.of(LiyMod.MOD_ID, "loli_repair_materials")
    );

    public static final ToolMaterial LOLI = new ToolMaterial(
            INCORRECT_FOR_LOLI_TOOL,
            Integer.MAX_VALUE,
            Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            30,
            LOLI_REPAIR_MATERIALS
    );

    private ModToolMaterials() {
    }
}
