package com.example.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
		/*TODO
			* GENERATE SESSION_ID FOR BETTER PROCESSING [DONE]
			* WHY API FAILING TO REPLY - maybe taking too long
			* QUERY FOR GROUPING KNOWN CHARGES
			* QUERY FOR TOP 5-10 CHARGES
			* BATCH PROCESSING FOR PYTHON
			* SAVE FILE(S) UNDER SESSION_ID DIR
			* IMPLEMENT CLEANUP (DELETE STUFF)
		 */
	}

}
