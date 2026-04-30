package de.zettsystems.starfare.social.application;

import de.zettsystems.starfare.social.values.Visibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Spring-injected collaborators are kept by reference for the bean's lifetime by design.")
public class DefaultVisibilityFilter implements VisibilityFilter {

    private final FriendshipService friendships;
    private final UserPreferencesService preferences;

    public DefaultVisibilityFilter(FriendshipService friendships, UserPreferencesService preferences) {
        this.friendships = friendships;
        this.preferences = preferences;
    }

    @Override
    public boolean canSee(String observer, String target) {
        if (observer == null || target == null) {
            return false;
        }
        if (observer.equalsIgnoreCase(target)) {
            return true;
        }
        if (friendships.isBlockedBetween(observer, target)) {
            return false;
        }
        return permits(observer, target) && permits(target, observer);
    }

    private boolean permits(String viewer, String seen) {
        Visibility v = preferences.getVisibility(viewer);
        return switch (v) {
            case ALL -> true;
            case FRIENDS_ONLY -> friendships.areFriends(viewer, seen);
            case NONE -> false;
        };
    }
}
