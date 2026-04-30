package de.zettsystems.starfare.architecture;

import de.zettsystems.starfare.StarfareApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThatCode;

class ModulithTest {

    private final ApplicationModules modules = ApplicationModules.of(StarfareApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesDocumentation() {
        Documenter.Options options = Documenter.Options.defaults()
                .withOutputFolder("build/spring-modulith-docs");
        assertThatCode(() -> new Documenter(modules, options)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases()).doesNotThrowAnyException();
    }
}
