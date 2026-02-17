package com.kucp1127.sharedcabbooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Airport Ride Pooling API")
                        .version("1.0.0")
                        .description("""
                                ## Backend API for Smart Airport Ride Pooling System
                                
                                ### Features:
                                - **Passenger Management** - Register and manage passengers
                                - **Cab Management** - Register cabs and track availability
                                - **Ride Booking** - Book shared rides with intelligent pooling
                                - **Dynamic Pricing** - Surge pricing and sharing discounts
                                - **Real-time Cancellation** - Cancel bookings with automatic rebalancing
                                
                                ### API Call Order:
                                1. **Register Passengers** - `/api/v1/passengers`
                                2. **Register Cabs** - `/api/v1/cabs`
                                3. **Get Fare Estimate** - `/api/v1/pricing/estimate`
                                4. **Book a Ride** - `/api/v1/rides`
                                5. **View Ride Details** - `/api/v1/rides/{id}`
                                6. **Cancel Booking** (optional) - `/api/v1/bookings/cancel`
                                
                                ### Design Patterns Used:
                                - Strategy Pattern (Ride Matching, Pricing)
                                - Observer Pattern (Cancellation Events)
                                - Chain of Responsibility (Pricing Engine)
                                - Repository Pattern (Data Access)
                                """)
                        .contact(new Contact()
                                .name("Developer")
                                .email("developer@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ))
                .tags(List.of(
                        new Tag().name("1. Passengers").description("Step 1: Register passengers first"),
                        new Tag().name("2. Cabs").description("Step 2: Register cabs/drivers"),
                        new Tag().name("3. Pricing").description("Step 3: Get fare estimates"),
                        new Tag().name("4. Rides").description("Step 4: Book and manage rides"),
                        new Tag().name("5. Bookings").description("Step 5: Manage bookings (cancel, etc.)")
                ));
    }
}
