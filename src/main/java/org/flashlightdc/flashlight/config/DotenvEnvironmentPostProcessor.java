package org.flashlightdc.flashlight.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code .env} entries into the Spring {@code Environment} as a
 * high-priority property source. This runs during
 * {@code prepareEnvironment()}, before {@code ${...}} placeholders in
 * {@code application.properties} are resolved — so it works for both
 * {@code main()} and {@code @SpringBootTest}.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        Map<String, Object> dotenvProperties = new HashMap<>();
        dotenv.entries().forEach(entry ->
                dotenvProperties.put(entry.getKey(), entry.getValue()));

        if (!dotenvProperties.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenv", dotenvProperties));
        }
    }
}
