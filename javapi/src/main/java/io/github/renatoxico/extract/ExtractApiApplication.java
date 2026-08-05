package io.github.renatoxico.extract;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExtractApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExtractApiApplication.class, args);
	}

}
