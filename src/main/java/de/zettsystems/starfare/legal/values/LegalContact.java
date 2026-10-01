package de.zettsystems.starfare.legal.values;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Public operator and contact data displayed in the imprint. */
@ConfigurationProperties("starfare.legal")
public record LegalContact(String operatorName, String postalAddress, String contactEmail) {
    public boolean hasPostalAddress() {
        return !postalAddress.isBlank();
    }

    public boolean hasContactEmail() {
        return !contactEmail.isBlank();
    }
}
