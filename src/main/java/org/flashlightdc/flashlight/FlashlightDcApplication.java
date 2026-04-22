package org.flashlightdc.flashlight;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashlightDcApplication {

    public static void main(String[] args) {
        // Load .env variables into System properties
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        SpringApplication.run(FlashlightDcApplication.class, args);
    }

    @PostConstruct
    void checkWallet() {
        System.out.println("TNS_ADMIN: " + System.getProperty("oracle.net.tns_admin"));
        System.out.println("Working dir: " + System.getProperty("user.dir"));
    }
}
