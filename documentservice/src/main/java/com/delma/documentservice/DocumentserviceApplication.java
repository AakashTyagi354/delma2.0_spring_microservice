package com.delma.documentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.delma.documentservice",
        "com.delma.common"
})
public class DocumentserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentserviceApplication.class, args);
	}

}
