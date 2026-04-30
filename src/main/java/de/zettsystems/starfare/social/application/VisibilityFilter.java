package de.zettsystems.starfare.social.application;

public interface VisibilityFilter {
    boolean canSee(String observer, String target);
}
