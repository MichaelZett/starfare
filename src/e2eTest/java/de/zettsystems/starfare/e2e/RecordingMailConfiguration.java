package de.zettsystems.starfare.e2e;

import de.zettsystems.identity.application.IdentityMailSender;
import de.zettsystems.identity.values.UserAccountDto;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hält den Mailverkehr aus der Suite heraus: {@code application.yaml} nennt
 * einen Mailhost (Mailpit), also gäbe es einen {@code JavaMailSender} und der
 * Identity-Baustein würde wirklich SMTP sprechen. Die aufgezeichneten Mails
 * liefern zugleich den Bestätigungslink für die Schritte.
 */
@TestConfiguration
public class RecordingMailConfiguration {

    @Bean
    RecordingMailSender identityMailSender() {
        return new RecordingMailSender();
    }

    /** Merkt sich, was verschickt worden wäre. */
    public static class RecordingMailSender implements IdentityMailSender {

        private final List<Sent> sent = new ArrayList<>();

        @Override
        public void sendEmailVerification(UserAccountDto user, String confirmationUrl) {
            sent.add(new Sent(user.email(), confirmationUrl));
        }

        @Override
        public void sendPasswordReset(UserAccountDto user, String resetUrl) {
            sent.add(new Sent(user.email(), resetUrl));
        }

        public List<Sent> sent() {
            return Collections.unmodifiableList(sent);
        }

        /** Eine nicht verschickte Mail: an wen sie ginge und welchen Link sie trägt. */
        public record Sent(String recipient, String url) {
        }
    }
}
