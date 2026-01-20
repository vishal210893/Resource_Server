package com.learning.oauth.resource_server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimeZone;

@Slf4j
@Component
public class ApplicationStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }

        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profiles = activeProfiles.length == 0 ?
            env.getProperty("spring.profiles.default", "default") :
            String.join(", ", activeProfiles);

        log.info("\n----------------------------------------------------------\n" +
                "Application '{}' is running! Access URLs:\n" +
                "  Local:      {}://localhost:{}{}\n" +
                "  External:   {}://{}:{}{}\n" +
                "  Profile(s): {}\n" +
                "  Version:    {}\n" +
                "  Java:       {} ({})\n" +
                "  TimeZone:   {}\n" +
                "----------------------------------------------------------",
            env.getProperty("spring.application.name", "Resource_Server"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            profiles,
            env.getProperty("application.version", "0.0.1-SNAPSHOT"),
            System.getProperty("java.version"),
            System.getProperty("java.vendor"),
            TimeZone.getDefault().getID()
        );
    }
}
