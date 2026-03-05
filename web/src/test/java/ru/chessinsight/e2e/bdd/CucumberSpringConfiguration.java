package ru.chessinsight.e2e.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import ru.chessinsight.integration.AbstractWebIntegrationTest;

@CucumberContextConfiguration
@TestPropertySource(properties = {
        "security.auth.require-email-otp=true",
        "security.auth.max-failed-attempts=3",
        "security.auth.mail.enabled=false"
})
public class CucumberSpringConfiguration extends AbstractWebIntegrationTest {
}
