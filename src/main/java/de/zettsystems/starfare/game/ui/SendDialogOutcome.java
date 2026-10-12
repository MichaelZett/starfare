package de.zettsystems.starfare.game.ui;

/** Delivers exactly one close result, including Escape and outside clicks. */
final class SendDialogOutcome {
    private final Runnable onAccepted;
    private final Runnable onCancelled;
    private boolean accepted;
    private boolean completed;

    SendDialogOutcome(Runnable onAccepted, Runnable onCancelled) {
        this.onAccepted = onAccepted;
        this.onCancelled = onCancelled;
    }

    void accepted() { accepted = true; }

    void closed() {
        if (completed) { return; }
        completed = true;
        if (accepted) { onAccepted.run(); }
        else { onCancelled.run(); }
    }
}
