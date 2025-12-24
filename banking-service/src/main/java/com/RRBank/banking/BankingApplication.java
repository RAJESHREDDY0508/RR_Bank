package com.RRBank.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * RR-Bank Banking Application
 * 
 * A comprehensive banking application with the following features:
 * - User Authentication & Authorization (JWT-based)
 * - Account Management (Checking, Savings, Credit accounts)
 * - Transaction Processing (Deposits, Withdrawals, Transfers)
 * - Real-time Event Processing (Kafka)
 * - Caching (Redis)
 * - Database Migrations (Flyway)
 * - API Monitoring (Actuator, Prometheus)
 * 
 * Architecture:
 * - Layered Architecture (Controller -> Service -> Repository)
 * - RESTful API design
 * - Microservices-ready structure
 * - Event-driven architecture with Kafka
 * - Distributed caching with Redis
 * - PostgreSQL for persistent storage
 * 
 * @author RR-Bank Development Team
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
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
			"  Server running on: http://localhost:8080\n" +
			"  API Base URL: http://localhost:8080/api\n" +
			"  Health Check: http://localhost:8080/actuator/health\n" +
			"  Metrics: http://localhost:8080/actuator/prometheus\n" +
			"==============================================\n"
		);
	}
}
