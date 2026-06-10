package com.delma.appointmentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(
        scanBasePackages = {
                "com.delma.appointmentservice",
                "com.delma.common"
        },
        excludeName = {
                // Use string-based exclude to avoid compile-time import issue
                // These classes exist at runtime in spring-boot-autoconfigure jar
                // but we can't import them directly
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
        }
)
@EnableScheduling
@EnableAspectJAutoProxy
@EnableTransactionManagement(order = 2)
public class AppointmentserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AppointmentserviceApplication.class, args);
    }
}