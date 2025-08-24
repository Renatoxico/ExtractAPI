package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = null;
		try {
			context = SpringApplication.run(ApiApplication.class, args);
		} catch (Exception e) {
			if (context != null) {
				Environment env = context.getEnvironment();
				System.out.println("🔧 spring.datasource.url = " + env.getProperty("spring.datasource.url"));
			}
			throw e; // rethrow to preserve original stack trace
		}
		/*TODO
			* IMPLEMENT CLEANUP (DELETE STUFF)
		 */
	}

}
