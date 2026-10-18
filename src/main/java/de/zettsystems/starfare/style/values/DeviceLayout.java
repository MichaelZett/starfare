package de.zettsystems.starfare.style.values;

import java.util.Locale;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The one user interface a session builds: phones get their own layout, everything else
 * (desktops, laptops, tablets) the regular one. Views never build both.
 */
public enum DeviceLayout {
    MOBILE, DESKTOP;

    /** Query parameter that overrides the detection for the rest of the session. */
    public static final String PARAMETER = "layout";

    /** Phones identify as "Mobi" (Android Chrome, Firefox), iPhone or iPod; tablets stay on the desktop layout. */
    public static DeviceLayout fromUserAgent(@Nullable String userAgent) {
        if (userAgent == null) { return DESKTOP; }
        String agent = userAgent.toLowerCase(Locale.ROOT);
        if (agent.contains("ipad") || agent.contains("tablet")) { return DESKTOP; }
        boolean phone = agent.contains("iphone") || agent.contains("ipod") || agent.contains("mobi")
                || agent.contains("windows phone");
        return phone ? MOBILE : DESKTOP;
    }

    /** {@code mobile} or {@code desktop}, case-insensitive; anything else is ignored. */
    public static Optional<DeviceLayout> fromParameter(@Nullable String value) {
        if (value == null) { return Optional.empty(); }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mobile" -> Optional.of(MOBILE);
            case "desktop" -> Optional.of(DESKTOP);
            default -> Optional.empty();
        };
    }
}
