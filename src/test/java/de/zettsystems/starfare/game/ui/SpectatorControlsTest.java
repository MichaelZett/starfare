package de.zettsystems.starfare.game.ui;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import de.zettsystems.starfare.game.values.Player;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpectatorControlsTest {
    private static final Player FIRST = new Player(1, "P1", false, "#111111");
    private static final Player SECOND = new Player(2, "P2", false, "#222222");

    private final AtomicInteger changes = new AtomicInteger();
    private final SpectatorControls controls = new SpectatorControls(changes::incrementAndGet);

    @Test
    void showsTheFirstPlayerOnceAndKeepsTheChoiceUntilReset() {
        assertThat(controls.isVisible()).isFalse();
        assertThat(controls.perspective()).as("nothing chosen yet").isEqualTo(-1);

        controls.show(List.of(FIRST, SECOND));
        perspective().setValue(SECOND);
        controls.show(List.of(FIRST, SECOND));

        assertThat(controls.isVisible()).isTrue();
        assertThat(controls.perspective()).isEqualTo(2);
        assertThat(changes).as("server-side changes do not reload the view").hasValue(0);

        controls.reset();
        assertThat(controls.isVisible()).isFalse();
        assertThat(controls.perspective()).isEqualTo(-1);
        assertThat(controls.fogOfWar()).isTrue();
    }

    @Test
    void onlyAViewerChoiceReloadsTheView() {
        controls.show(List.of(FIRST, SECOND));
        ComboBox<Player> perspective = perspective();
        Checkbox fog = fog();

        perspective.clear();
        fireFromClient(perspective, SECOND);
        assertThat(changes).as("a cleared perspective is no choice").hasValue(0);

        perspective.setValue(SECOND);
        fireFromClient(perspective, FIRST);
        fog.setValue(false);
        assertThat(changes).hasValue(1);
        fireFromClient(fog, true);
        assertThat(changes).hasValue(2);
        assertThat(controls.fogOfWar()).isFalse();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Player> perspective() {
        return (ComboBox<Player>) controls.getComponentAt(0);
    }

    private Checkbox fog() {
        return (Checkbox) controls.getComponentAt(1);
    }

    private static <C extends AbstractField<C, V>, V> void fireFromClient(C field, V oldValue) {
        ComponentUtil.fireEvent(field, new ComponentValueChangeEvent<>(field, field, oldValue, true));
    }
}
