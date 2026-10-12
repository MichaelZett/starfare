package de.zettsystems.starfare.diplomacy.values;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DiplomacyDraftsTest {
    @Test void unrelatedEventsPreserveAnOpenNoticeButNeverTransferItToAnotherTarget() {
        var drafts = new DiplomacyDrafts();
        var original = new DiplomacyDrafts.Key(DiplomacyOrder.Action.NOTICE, 1);
        var other = new DiplomacyDrafts.Key(DiplomacyOrder.Action.NOTICE, 2);
        drafts.edit(original, 7); drafts.open(original, true);
        assertThat(drafts.retain(Set.of(original, other))).isFalse();
        assertThat(drafts.value(original, 3)).isEqualTo(7); assertThat(drafts.opened(original)).isTrue();
        assertThat(drafts.value(other, 3)).isEqualTo(3); assertThat(drafts.opened(other)).isFalse();
        assertThat(drafts.retain(Set.of(other))).isTrue();
        assertThat(drafts.value(original, 3)).isEqualTo(3); assertThat(drafts.opened(original)).isFalse();
    }
    @Test void completionResetsOnlyItsOwnDraftAndDeparturePreviewMatchesStoredDeadline() {
        var drafts = new DiplomacyDrafts();
        var key = new DiplomacyDrafts.Key(DiplomacyOrder.Action.FOUND, 2);
        drafts.edit(key, 0); drafts.open(key, true); drafts.completed(key);
        assertThat(drafts.value(key, 3)).isEqualTo(3); assertThat(drafts.opened(key)).isFalse();
        var group = new AllianceGroup(1, Set.of(1, 2), 0, java.util.Map.of());
        assertThat(group.departureTurn(5)).isEqualTo(6);
        assertThat(group.announceDeparture(1, 5).departures().get(1)).isEqualTo(group.departureTurn(5));
    }
}
