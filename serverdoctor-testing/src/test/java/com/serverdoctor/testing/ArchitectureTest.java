package com.serverdoctor.testing;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Erzwingt die in Phase 1 definierten Architekturregeln als Build-Brecher.
 * Scannt das gesamte Produktiv-Paket com.serverdoctor (ohne Tests).
 */
@AnalyzeClasses(packages = "com.serverdoctor", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // --- Plattform-Unabhängigkeit: Core/Common/API/Storage kennen keine Plattform-SDKs ---

    @ArchTest
    static final ArchRule coreIsPlatformFree = noClasses()
            .that().resideInAnyPackage("..core..", "..common..", "..api..", "..storage..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.bukkit..", "io.papermc..", "com.velocitypowered..", "net.md_5.bungee..")
            .as("Core/Common/API/Storage dürfen keine Plattform-SDKs importieren");

    // --- Clean-Architecture-Dependency-Rule: Abhängigkeiten zeigen nach innen ---

    @ArchTest
    static final ArchRule commonDependsOnNothingInternal = noClasses()
            .that().resideInAPackage("..common..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..core..", "..api..", "..storage..", "..platform..")
            .as("common ist die innerste Schicht und darf nichts Internes kennen");

    @ArchTest
    static final ArchRule apiDoesNotDependOnImplementation = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..core..", "..platform..", "..storage..")
            .as("Die Public-API darf nicht von der Core-Implementierung abhängen");

    // --- Read-only-Invariante: Plattform-Adapter exponieren keine schreibenden Methoden ---

    @ArchTest
    static final ArchRule platformAdaptersAreReadOnly = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..platform..")
            .and().arePublic()
            .should().haveNameNotMatching("(?i)(set|delete|remove|kick|ban|disable|write|update|save|create).*")
            .as("Plattform-Adapter dürfen keine schreibenden Methoden exponieren (Read-only)");
}
