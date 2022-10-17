package com.codeages.javaskeletonserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class JavaSkeletonServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaSkeletonServerApplication.class, args);
	}

}
