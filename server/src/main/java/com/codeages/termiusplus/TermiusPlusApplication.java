package com.codeages.termiusplus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class TermiusPlusApplication {

	public static void main(String[] args) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("com.codeages")) {
			for (String name : context.getBeanDefinitionNames()) {
				System.out.println(name) ;
			}
		}

		SpringApplication.run(TermiusPlusApplication.class, args);
	}

}
