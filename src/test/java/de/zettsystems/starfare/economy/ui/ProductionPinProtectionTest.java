package de.zettsystems.starfare.economy.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import de.zettsystems.starfare.economy.values.IndustryView;
import de.zettsystems.starfare.economy.values.MetalView;
import de.zettsystems.starfare.economy.values.ProductionCommit;
import de.zettsystems.starfare.economy.values.ProductionInputs;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProductionPinProtectionTest {
    @Test
    void historicalIndustryWithoutMetalsKeepsLocalPinsWithoutCallingMiningAutomation() {
        var calls = new AtomicInteger();
        var industry = new IndustryView(10, 0, 0, 5, 0, false, 0);
        Checkbox pin = shipPin(industry, calls);
        pin.setValue(true);
        assertThat(pin.getValue()).isTrue();
        assertThat(calls.get()).isZero();
    }

    @Test
    void metalIndustryNotifiesPersistentProtectionWhenItsShipbuildingIsPinned() {
        var calls = new AtomicInteger();
        var industry = new IndustryView(10, 0, 0, 5, 0, false, 0, null,
                new MetalView(100, 3, 0, 0, 0, 10, 0));
        shipPin(industry, calls).setValue(true);
        assertThat(calls.get()).isEqualTo(1);
    }

    private static Checkbox shipPin(IndustryView industry, AtomicInteger calls) {
        var input = new ProductionInputs().forSystem(1, 1, industry);
        var editor = new ProductionEditor(input, _ -> Optional.empty(), _ -> ProductionCommit.REJECTED,
                _ -> calls.incrementAndGet());
        return descendants(editor).filter(Checkbox.class::isInstance).map(Checkbox.class::cast)
                .filter(pin -> pin.getId().orElse("").equals("industry-shipbuilding-pin")).findFirst().orElseThrow();
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }
}
