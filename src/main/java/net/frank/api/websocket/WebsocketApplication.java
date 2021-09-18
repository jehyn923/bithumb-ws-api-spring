package net.frank.api.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.frank.api.websocket.ticker.Ticker;
import net.frank.api.websocket.ticker.TickerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
@SpringBootApplication @RequiredArgsConstructor

@EnableConfigurationProperties({MongoProperties.class})

public class WebsocketApplication implements CommandLineRunner {

	private final ObjectMapper objectMapper;
	private final WebSocketClient client;
	private final TickerRepository tickerRepository;

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WebsocketApplication.class);
		app.setWebApplicationType(WebApplicationType.REACTIVE);
		app.run(args);
	}

	@Override
	public void run(String... args){
		client.execute(
				URI.create("wss://pubwss.bithumb.com/pub/ws"),
				session -> session.send(
								Mono.just(session.textMessage("{'type':'ticker', 'symbols': ['ETH_KRW'],'tickTypes':['30M']}")))
						.thenMany(session.receive().map(WebSocketMessage::getPayloadAsText))
						.filter(text-> text.startsWith("{\"type\":\"ticker\""))
						.map(message -> {
							try {
								return objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
										.readValue(objectMapper.readTree(message).get("content").toString(), Ticker.class);
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							}
							return null;
						})
						.log().doOnNext(ticker -> tickerRepository.save(ticker).subscribe())
						.then()).subscribe();
	}
}
