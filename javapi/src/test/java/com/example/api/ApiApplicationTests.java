package com.example.api;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ApiApplicationTests {
	@MockitoBean
	private FirebaseAuth firebaseAuth;

	@Test
	void contextLoads() {
	}

}
