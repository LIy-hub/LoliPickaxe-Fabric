package com.liymod.item;

import java.util.List;
import java.util.Optional;

public final class LoliCardCatalog {
    public record Art(String id, String texture, int width, int height, String link) { }
    public static final List<Art> ALBUM = List.of(
            art("xiaomo_daughter_1", "card_xiaomo_1", 656, 1000), art("xiaomo_daughter_2", "card_xiaomo_2", 644, 1000),
            art("xiaomo_daughter_3", "card_xiaomo_3", 1157, 1637), art("xiaomo_daughter_4", "card_xiaomo_4", 2890, 4092),
            art("xiaomo_daughter_5", "card_xiaomo_5", 1060, 1500), art("xiaomo_daughter_6", "card_xiaomo_6", 1920, 1237),
            art("xiaomo_daughter_7", "card_xiaomo_7", 1301, 2015), art("xiaomo_daughter_8", "card_xiaomo_8", 1018, 1500));
    public static final List<Art> STANDALONE = List.of(
            new Art("altar_guide", "altar_guide", 63, 63, null),
            new Art("gk_head_portrait", "gk_head_portrait", 1000, 1000, "https://www.pixiv.net/artworks/61282195"));
    private LoliCardCatalog() { }
    private static Art art(String id, String texture, int width, int height) {
        return new Art(id, texture, width, height, "https://www.pixiv.net/users/5776001");
    }
    public static Optional<Art> byId(String id) { return java.util.stream.Stream.concat(STANDALONE.stream(), ALBUM.stream()).filter(a -> a.id.equals(id)).findFirst(); }
}
