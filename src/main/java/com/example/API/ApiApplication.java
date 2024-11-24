package com.example.API;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
		/*TODO - FLOWS
			* SEND SINGLE FILES TO BE ANALYSED
			*	* 	IMMEDIATE  OUTPUT
			* 	*	NO DB
			* SEND BATCH FILES
			*	*	CREATE USER TAG
			* 	*
		 */
	}

}
