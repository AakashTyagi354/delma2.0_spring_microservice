package com.delma.categoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.delma.categoryservice",
        "com.delma.common"
})
public class CategoryserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CategoryserviceApplication.class, args);
	}

}
