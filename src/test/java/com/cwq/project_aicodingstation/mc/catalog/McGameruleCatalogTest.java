package com.cwq.project_aicodingstation.mc.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McGameruleCatalogTest {

    @Test
    void allowsNonCheatRulesAndRejectsKeepinventory() {
        assertTrue(McGameruleCatalog.isAllowed("pvp"));
        assertTrue(McGameruleCatalog.isAllowed("falldamage"));
        assertFalse(McGameruleCatalog.isAllowed("keepinventory"));
        assertFalse(McGameruleCatalog.NON_CHEAT.stream().anyMatch(d -> d.id().equals("keepinventory")));
        assertFalse(McGameruleCatalog.NON_CHEAT.stream()
                .filter(d -> d.id().equals("pvp") || d.id().equals("showdaysplayed"))
                .anyMatch(McGameruleCatalog.Def::defaultOn));
    }
}
