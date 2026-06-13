package com.serverdoctor.core.conflict;

import com.serverdoctor.common.model.Severity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Erweiterbare Konfliktdatenbank. Startet mit einer kleinen Menge bekannter,
 * illustrativer Definitionen; später aus einer externen Quelle befüllbar.
 */
public final class ConflictDatabase {

    private final List<ConflictDefinition> definitions = new CopyOnWriteArrayList<>();

    public void register(ConflictDefinition definition) { definitions.add(definition); }

    public List<ConflictDefinition> all() { return List.copyOf(definitions); }

    public static ConflictDatabase withDefaults() {
        ConflictDatabase db = new ConflictDatabase();
        db.register(new ConflictDefinition("eco-double",
                "Essentials", "CMI", Severity.HIGH,
                "Zwei Economy-Provider gleichzeitig können zu inkonsistenten Kontoständen führen."));
        db.register(new ConflictDefinition("chat-double",
                "EssentialsChat", "DeluxeChat", Severity.MEDIUM,
                "Mehrere Chat-Formatierungs-Plugins greifen auf dasselbe Event - Reihenfolge ist nicht garantiert."));
        db.register(new ConflictDefinition("anticheat-double",
                "Vulcan", "Matrix", Severity.HIGH,
                "Zwei Anti-Cheat-Plugins parallel verursachen Falsch-Positive und CPU-Overhead."));
        db.register(new ConflictDefinition("perms-double",
                "LuckPerms", "PermissionsEx", Severity.CRITICAL,
                "Zwei Permission-Systeme gleichzeitig führen zu unvorhersehbaren Rechten."));
        return db;
    }
}
