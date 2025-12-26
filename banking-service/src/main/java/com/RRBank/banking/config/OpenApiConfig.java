package com.RRBank.banking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration for RR-Bank Banking Service
 * 
 * Provides comprehensive API documentation with JWT security scheme.
 * Accessible at:
 * - Swagger UI: /swagger-ui.html
 * - API Docs: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI bankingServiceOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(apiInfo())
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("RR-Bank Banking Service API")
                .description("""
                        ## RR-Bank Core Banking API
                        
                        This API provides comprehensive banking operations including:
                        
                        ### Authentication & Security
                        - User registration and login
                        - JWT-based authentication
                        - Multi-factor authentication (MFA)
                        
                        ### Account Management
                        - Create and manage bank accounts (Savings, Checking, Credit, Business)
                        - Account status management (Active, Frozen, Closed, Suspended)
                        - Balance inquiries
                        
                        ### Customer Management
                        - Customer profile management
                        - KYC verification
                        
                        ### Transactions
                        - Deposits and withdrawals
                        - Fund transfers
                        - Transaction history
                        
                        ### Payments
                        - Bill payments
                        - Scheduled payments
                        - Payment history
                        
                        ### Fraud Detection
                        - Real-time fraud monitoring
                        - Risk assessment
                        - Fraud event management
                        
                        ### Notifications
                        - Email, SMS, and Push notifications
                        - Notification preferences
                        
                        ### Statements
                        - Monthly statements
                        - Transaction summaries
                        
                        ### Audit
                        - Activity logging
                        - Compliance tracking
                        
                        ---
                        
                        **Authentication:** All endpoints (except /api/auth/*) require a valid JWT token.
                        Use the Authorize button to set your token.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("RR-Bank Development Team")
                        .email("dev@rrbank.com")
                        .url("https://rrbank.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://rrbank.com/license"));
    }

    private List<Server> getServers() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local Development Server");

        Server devServer = new Server()
                .url("https://dev-api.rrbank.com")
                .description("Development Server");

        Server prodServer = new Server()
                .url("https://api.rrbank.com")
                .description("Production Server");

        return switch (activeProfile) {
            case "prod" -> List.of(prodServer);
            case "dev" -> List.of(devServer, localServer);
            default -> List.of(localServer, devServer);
        };
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("bearerAuth")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Authorization header using the Bearer scheme.
                        
                        Enter your token in the text input below.
                        
                        Example: "eyJhbGciOiJIUzUxMiJ9..."
                        
                        To obtain a token:
                        1. POST /api/auth/login with username and password
                        2. Copy the 'token' from the response
                        3. Click 'Authorize' and paste the token
                        """);
    }
}
