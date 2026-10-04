package com.liymod.tool;

import com.liymod.LiyMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

public final class ModToolMaterials {
    private static final TagKey<Block> INCORRECT_FOR_LOLI_TOOL = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "incorrect_for_loli_tool")
    );
    private static final TagKey<Item> LOLI_REPAIR_MATERIALS = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(LiyMod.MOD_ID, "loli_repair_materials")
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
