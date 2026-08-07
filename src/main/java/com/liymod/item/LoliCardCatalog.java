package com.liymod.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.RandomSource;

/** Stable, safe identifiers for the ten art files bundled by the legacy release. */
public final class LoliCardCatalog {
    public record Art(String id, String resourceName, String displayName, String group) {
        public Art {
            if (!id.matches("[a-z0-9_]+") || !resourceName.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException("Card ids must be resource-safe");
            }
            displayName = displayName == null ? id : displayName;
            group = group == null ? "" : group;
        }
    }

    public static final String DAUGHTER_GROUP = "xiaomo_daughter";
    public static final List<Art> ALL = List.of(
            new Art("xiaomo_daughter_1", "card_xiaomo_1", "小莫女儿''1.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_2", "card_xiaomo_2", "小莫女儿''2.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_3", "card_xiaomo_3", "小莫女儿''3.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_4", "card_xiaomo_4", "小莫女儿''4.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_5", "card_xiaomo_5", "小莫女儿''5.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_6", "card_xiaomo_6", "小莫女儿''6.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_7", "card_xiaomo_7", "小莫女儿''7.png", DAUGHTER_GROUP),
            new Art("xiaomo_daughter_8", "card_xiaomo_8", "小莫女儿''8.png", DAUGHTER_GROUP),
            new Art("altar_guide", "altar_guide", "召唤祭坛摆放方式.png", ""),
            new Art("gk_head_portrait", "gk_head_portrait", "gk_head_portrait.png", "")
    );
    public static final List<Art> STANDALONE = ALL.stream()
            .filter(art -> art.group().isEmpty())
            .toList();
    public static final List<String> GROUPS = ALL.stream()
            .map(Art::group)
            .filter(group -> !group.isEmpty())
            .distinct()
            .toList();
    private static final Map<String, Art> BY_ID = ALL.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Art::id, art -> art));

    private LoliCardCatalog() {
    }

    public static Optional<Art> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<Art> group(String group) {
        return ALL.stream().filter(art -> art.group().equals(group)).toList();
    }

    public static Art randomStandalone(RandomSource random) {
        return STANDALONE.get(random.nextInt(STANDALONE.size()));
    }

    public static String randomGroup(RandomSource random) {
        return GROUPS.get(random.nextInt(GROUPS.size()));
    }
}
