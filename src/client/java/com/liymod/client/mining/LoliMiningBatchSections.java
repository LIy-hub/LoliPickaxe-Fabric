package com.liymod.client.mining;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Includes neighboring meshes whose exposed faces or lighting depend on a mined boundary. */
public final class LoliMiningBatchSections {
    private LoliMiningBatchSections() { }

    public static Set<Long> affectedSections(Iterable<BlockPos> positions) {
        Set<Long> sections = new HashSet<>();
        for (var pos : positions) {
            for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++) {
                for (int y = (pos.getY() - 1) >> 4; y <= (pos.getY() + 1) >> 4; y++) {
                    for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++) {
                        sections.add(SectionPos.asLong(x, y, z));
                    }
                }
            }
        }
        return Set.copyOf(sections);
    }
}
