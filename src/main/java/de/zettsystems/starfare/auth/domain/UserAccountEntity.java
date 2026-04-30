package de.zettsystems.starfare.auth.domain;

import de.zettsystems.starfare.persistence.AbstractBaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity extends AbstractBaseEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_account_seq")
    @SequenceGenerator(name = "user_account_seq", sequenceName = "user_account_seq", allocationSize = 50)
    private Long id;

    @NaturalId
    @Column(name = "username", nullable = false, length = 30, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @SuppressWarnings("NullAway.Init")
    protected UserAccountEntity() {}

    @SuppressWarnings("NullAway.Init")
    public UserAccountEntity(String username, String displayName, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    @Override
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
}
