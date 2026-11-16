package de.zettsystems.starfare.standalone.social.application;

public interface VisibilityFilter {
    boolean canSee(String observer, String target);
}
