package com.samitkumarpatel.springbootreactive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class SpringbootReactiveApplicationTests {

	@Test
	void contextLoads() {
	}

}

@ExtendWith(SpringExtension.class)
class HandlerTest {
	@MockBean
	private SmsService smsService;
	@MockBean
	private EmailService emailService;
	@MockBean
	private NotificationService notificationService;
	@MockBean
	private UserService userService;
	@MockBean
	private ServerRequest serverRequest;
	private Handler handler;

	@BeforeEach
	void setUp() {
		when(smsService.send(anyString(), any())).thenReturn(Mono.empty());
		when(emailService.send(anyString(), any())).thenReturn(Mono.empty());
		when(notificationService.send(anyString(), any())).thenReturn(Mono.empty());
		when(userService.getUser(anyString())).thenReturn(Mono.just(User.builder().customerNumber("1").customerName("mock name").mobileNumber("mockNumber").email("mockEmail").address("mockAddress").build()));
		handler = new Handler(smsService, emailService, notificationService, userService);
	}

	@Test
	void sentMessageTest() {
		when(serverRequest.queryParam("messageTo")).thenReturn(Optional.of("1"));
		when(serverRequest.bodyToMono(Message.class)).thenReturn(Mono.just(Message.builder().id(1l).message("fake message").build()));
		StepVerifier.create(handler.sentMessage(serverRequest)).consumeNextWith(serverResponse -> {
			// TODO find out why this can't be verified
			// verify(smsService, times(1)).send(anyString(),any());
			assertEquals(200, serverResponse.rawStatusCode());
		}).verifyComplete();

	}
}