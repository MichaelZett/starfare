package de.zettsystems.starfare.auth.application;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StarfareUserDetailsService implements UserDetailsService {
    private final UserStore store;

    public StarfareUserDetailsService(UserStore store) {
        this.store = store;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = store.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unbekannter Benutzer: " + username));
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .roles("USER")
                .build();
    }
}
