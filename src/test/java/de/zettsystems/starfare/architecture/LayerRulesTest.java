package de.zettsystems.starfare.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "de.zettsystems.starfare", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerRulesTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule ui_must_not_depend_on_domain = noClasses()
            .that().resideInAPackage("..ui..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
            .because("the UI consumes immutable view models from ..values.. built by the application layer");

    @ArchTest
    static final ArchRule only_the_own_start_knows_the_identity_building_block = noClasses()
            .that().resideOutsideOfPackages("de.zettsystems.starfare.standalone..",
                    "de.zettsystems.starfare.legal..", "de.zettsystems.starfare.auth.config..")
            .should().dependOnClassesThat().resideInAPackage("de.zettsystems.identity..")
            .because("the game asks the platform through games-platform-api (PlatformAccounts, AccountRemoved; "
                    + "plan Z3); zs-identity belongs to Starfare's own start and stays out of the game artifact");

    @ArchTest
    static final ArchRule application_top_level_classes_must_be_spring_beans = classes()
            .that().resideInAPackage("..application..")
            .and().areTopLevelClasses()
            .and().areNotInterfaces()
            .should().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Component.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith(Configuration.class);
}
