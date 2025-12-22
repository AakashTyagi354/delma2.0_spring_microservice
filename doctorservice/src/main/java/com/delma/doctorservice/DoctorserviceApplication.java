package com.delma.doctorservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DoctorserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctorserviceApplication.class, args);
	}

}
