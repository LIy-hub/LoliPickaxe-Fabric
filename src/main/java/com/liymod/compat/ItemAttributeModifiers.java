package com.liymod.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Vanilla AttributeModifiers NBT, including slot and stable modifier UUIDs. */
public record ItemAttributeModifiers(ListTag tag) {
    public static final ItemAttributeModifiers EMPTY=new ItemAttributeModifiers(new ListTag());
    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final ListTag entries=new ListTag();
        public Builder add(Attribute attribute,AttributeModifier modifier,EquipmentSlot slot) {
            CompoundTag tag=modifier.save();
            tag.putString("AttributeName",BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString());
            tag.putString("Slot",slot.getName());entries.add(tag);return this;
        }
        public ItemAttributeModifiers build() { return new ItemAttributeModifiers(entries.copy()); }
    }
}
