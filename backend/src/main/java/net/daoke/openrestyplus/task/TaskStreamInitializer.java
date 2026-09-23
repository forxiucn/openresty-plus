package net.daoke.openrestyplus.task;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskStreamInitializer {
    @Bean ApplicationRunner initializeTaskStream(TaskStream taskStream) { return args -> taskStream.ensureGroup(); }
}
