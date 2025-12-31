package com.RRBank.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * RR-Bank Banking Application
 */
@SpringBootApplication(exclude = {
    KafkaAutoConfiguration.class,
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
public class BankingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
        
        System.out.println("\n" +
            "==============================================\n" +
            "  RR-Bank Application Started Successfully!\n" +
            "==============================================\n" +
            "  Server running on: http://localhost:8081\n" +
            "  API Base URL: http://localhost:8081/api\n" +
            "  Health Check: http://localhost:8081/actuator/health\n" +
            "  Gateway URL: http://localhost:8080/api\n" +
            "==============================================\n"
        );
    }
}
