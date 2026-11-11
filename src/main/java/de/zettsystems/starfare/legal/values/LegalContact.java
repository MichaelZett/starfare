package de.zettsystems.starfare.legal.values;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Public operator and contact data displayed in the imprint. */
@ConfigurationProperties("starfare.legal")
public record LegalContact(String operatorName, String postalAddress, String contactEmail) {
    /** Unset values are empty, so a host without Starfare lines in its YAML still starts. */
    public LegalContact {
        operatorName = operatorName == null ? "" : operatorName;
        postalAddress = postalAddress == null ? "" : postalAddress;
        contactEmail = contactEmail == null ? "" : contactEmail;
    }

    public boolean hasPostalAddress() {
        return !postalAddress.isBlank();
    }

    public boolean hasContactEmail() {
        return !contactEmail.isBlank();
    }
}
