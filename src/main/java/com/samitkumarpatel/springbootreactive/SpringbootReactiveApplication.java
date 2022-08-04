package com.samitkumarpatel.springbootreactive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class SpringbootReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootReactiveApplication.class, args);
	}

}

@Data @Builder @AllArgsConstructor @NoArgsConstructor
class Message {
	private long id;
	private String message;
}

@Data @Builder @AllArgsConstructor @NoArgsConstructor
class User {
	private String customerNumber;
	private String customerName;
	private String email;
	private String mobileNumber;
	private String address;
}

@Configuration
class Router {
	@Bean
	public RouterFunction route(Handler handler) {
		return RouterFunctions
				.route(GET("/one").and(contentType(APPLICATION_JSON)),handler::one)
				.andRoute(POST("/message").and(contentType(APPLICATION_JSON)), handler::sentMessage)
				.andRoute(POST("/iterate").and(contentType(APPLICATION_JSON)),handler::iterate);
	}
}

@Configuration
@RequiredArgsConstructor
@Slf4j
class Handler {
	private final SmsService smsService;
	private final EmailService emailService;
	private final NotificationService notificationService;
	private final UserService userService;
	public Mono<ServerResponse> one(ServerRequest request) {
		return ok().contentType(APPLICATION_JSON).body(Mono.just("ONE"),String.class);
	}

	public Mono<ServerResponse> sentMessage(ServerRequest request) {
		var messageTo = request.queryParam("messageTo").get();
		var message = request.bodyToMono(Message.class);
		return ok().contentType(APPLICATION_JSON).body(
				//extract the message
				message.flatMap(msg -> {
					// find user from based on customerCode
					return userService.getUser(messageTo).flatMap(user -> {
						//send notification in parallel to all the channel
						return Mono.zip(
							smsService.send(user.getMobileNumber(),msg),
							emailService.send(user.getEmail(),msg),
							notificationService.send(user.getCustomerNumber(),msg)
						).flatMap(tuple -> Mono.just("SUCCESS"));
					}).doOnError(e -> log.error("userService Error : {}",e.getMessage()));
				})
		,String.class);
	}

	public Mono<ServerResponse> iterate(ServerRequest request) {
		return request.bodyToMono(IModel.class)
				.flatMap(iModel -> {
					return Flux.fromIterable(iModel.getInts())
							.doOnNext(i -> log.info("PROCESSING {} ",i))
							.flatMap(i -> calculate(i)
									//This one is equivalent to .onErrorContinue but the position matter. For more Info , Please take a look: https://devdojo.com/ketonemaniac/reactor-onerrorcontinue-vs-onerrorresume
									.onErrorResume(err -> {
										log.info("onErrorResume");
										return Mono.just(0);
									})
							)
							/*
							.onErrorContinue((err,i) -> {
								log.error("err: {}, i: {}",err.getMessage(),i);
								//This will not return anything but continue the flow
							})*/
							.flatMap(calculatedI -> mapToIData(calculatedI,iModel.getName()))

							.collectList()
							.flatMap(listOfIData -> {
								return ServerResponse.ok().body(
										Mono.just(IDomain.builder().iData(listOfIData).build())
										,IDomain.class);
							});
				});

	}

	private Mono<IDomain.IData> mapToIData(Integer calculatedI, String name) {
		return Mono.just(
				IDomain.IData.builder().name(name).value(calculatedI!=0 ? calculatedI : null).result(calculatedI!=0 ? Result.PASS: Result.FAIL).build()
		);
	}

	private Mono<Integer> calculate(Integer i) {
		return Mono.fromCallable(() -> calculator(i,5))
				.flatMap(val -> {
					return Mono.just(val);
				})
				.doOnSuccess(val -> log.info("SERVICE CALL SUCCESS FOR {} WITH RESULT {}",i,val))
				.doOnError(e -> log.error("SERVICE CALL FAILURE FOR {} WITH ERROR {}",i,e.getMessage()))
				.retry(3);
	}

	//assume It's a legacy service call
	private Integer calculator(Integer i, Integer multiplyWithNumber) {
		if(i == Integer.valueOf(3)) {
			throw new RuntimeException("Number 3 is unlucky number");
		}
		return i * multiplyWithNumber;
	}
}
@Data @Builder @AllArgsConstructor @RequiredArgsConstructor
class IModel {
	private String name;
	private List<Integer> ints;
}

@Data @Builder @AllArgsConstructor @RequiredArgsConstructor
class IDomain {
	private List<IData> iData;
	@Data @Builder @AllArgsConstructor @RequiredArgsConstructor
	public static class IData {
		private String name;
		private Integer value;
		private Result result;
	}
}

enum Result {
	PASS, FAIL
}
@Service
@Slf4j
class UserService {
	private static final List<User> USERS = List.of(
			User.builder().customerNumber("1").customerName("Amit Mathur").mobileNumber("+919191913456").email("amit.m@abc.net").address("Street 24, New Delhi").build(),
			User.builder().customerNumber("2").customerName("D Martin").mobileNumber("+4171610234").email("martin.d@gmail.com").address("15 OsterPak, Prague").build(),
			User.builder().customerNumber("3").customerName("Bhargev Rathod").mobileNumber("+44123456789").email("bhargev@hell.net").address("Walking Street, London").build()
	);

	public Mono<User> getUser(String customerNumber) {
		return Mono.fromCallable(() -> fineUserByNumber(customerNumber));
	}

	private User fineUserByNumber(String customerNumber) {
		return USERS
				.stream()
				.filter(user -> user.getCustomerNumber().equalsIgnoreCase(customerNumber))
				.findFirst()
				.orElseThrow(()-> new UserNotFoundException("User not found"));
	}

}

@Service
@Slf4j
class SmsService {
	public Mono<Void> send(String mobileNumber, Message message) {
		log.info("message id {} , is sent via sms successfully to {}",message.getId(), mobileNumber);
		return Mono.empty();
	}
}

@Service
@Slf4j
class EmailService {
	public Mono<Void> send(String emailId, Message message) {
		log.info("message id {} , is sent via email successfully to {}",message.getId(), emailId);
		return Mono.empty();
	}
}

@Service
@Slf4j
class NotificationService {
	public Mono<Void> send(String customerNumber, Message message) {
		log.info("message id {} , is sent via notification successfully to {}",message.getId(), customerNumber);
		return Mono.empty();
	}
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException extends RuntimeException {
	UserNotFoundException(String message) {
		super(message);
	}
}