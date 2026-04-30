package de.zettsystems.starfare.persistence;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.jspecify.annotations.Nullable;

/**
 * Shared base for JPA aggregate roots. Carries the {@link Version}-based optimistic-locking
 * counter and centralises {@code equals}/{@code hashCode} so every entity compares by ID
 * (with identity-based fallback while {@link #getId()} is still {@code null}, i.e. before
 * the entity has been persisted).
 */
@MappedSuperclass
public abstract class AbstractBaseEntity<ID> {

    @Version
    private long version;

    public abstract @Nullable ID getId();

    public long getVersion() {
        return version;
    }

    @Override
    @SuppressWarnings("EqualsGetClass") // rationale: no entity inheritance and no lazy proxies in this codebase, so getClass() is precise.
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractBaseEntity<?> that = (AbstractBaseEntity<?>) o;
        ID id = getId();
        return id != null && id.equals(that.getId());
    }

    @Override
    public final int hashCode() {
        ID id = getId();
        return id != null ? id.hashCode() : System.identityHashCode(this);
    }
}
