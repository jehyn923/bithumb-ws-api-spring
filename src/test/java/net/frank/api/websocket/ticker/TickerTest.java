package net.frank.api.websocket.ticker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

@EnableReactiveMongoRepositories
@EnableMongoAuditing
@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class TickerTest {
    @Autowired
    private TickerRepository tickerRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("test DB")
    void testDB() {
        tickerRepository.save(new Ticker("abcdefght", "30H", "20210918", "15:53:00", 12345, 32134, 10000, 32134, 23212.12, 231323.2, 102394.2))
                .log().subscribe();
        tickerRepository.findById("abcdefght")
                .switchIfEmpty(Mono.just(new Ticker("1234", "3H", "202118", "13:00", 125, 334, 100, 324, 232.12, 233.2, 1394.2)))
                .log().subscribe();
    }

    @Test
    @DisplayName("test Json String to Mongo")
    void testJsonToMongo() throws Exception {
        String tickerExample = "{ \"type\" : \"ticker\",\"content\" : {\"symbol\" : \"BTC_KRW\",\"tickType\" : \"24H\"," +
                "\"date\" : \"20200129\"," +
                "\"time\" : \"121844\", " +
                "\"openPrice\" : \"2302\", " +
                "\"closePrice\" : \"2317\", " +
                "\"lowPrice\" : \"2272\", " +
                "\"highPrice\" : \"2344\", " +
                "\"value\" : \"2831915078.07065789\", " +
                "\"volume\" : \"1222314.51355788\", " +
                "\"sellVolume\" : \"760129.34079004\", " +
                "\"buyVolume\" : \"462185.17276784\", " +
                "\"prevClosePrice\" : \"2326\", " +
                "\"chgRate\" : \"0.65\", " +
                "\"chgAmt\" : \"15\", " +
                "\"volumePower\" : \"60.80\"}}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode jsonNode = mapper.readTree(tickerExample);
        Ticker ticker = mapper.readValue(jsonNode.get("content").toString(), Ticker.class);
        //tickerRepository.save(ticker).log().subscribe();
        Mono<Ticker> tick = Mono.just(ticker);
        tick.doOnNext(a -> tickerRepository.save(a).subscribe()).log().subscribe();
    }

    @Test
    @DisplayName("test ws connection with Bithumb")
    void testConnection() throws JsonProcessingException {
        WebSocketClient client = new ReactorNettyWebSocketClient();

        client.execute(
                URI.create("wss://pubwss.bithumb.com/pub/ws"),
                session -> session.send(
                                Mono.just(session.textMessage("{'type':'ticker', 'symbols': ['ETH_KRW'],'tickTypes':['30M']}")))
                        //.doOnNext(System.out::println)
                        .thenMany(session.receive().map(WebSocketMessage::retain).map(WebSocketMessage::getPayloadAsText).log())
                        .then()).subscribe();
    }

    @Test
    @DisplayName("test flux message to Mongo")
    void testFluxMessageToMongo() {
        String[] testExample = new String[10];
        String tickerExample = "{ \"type\" : \"ticker\",\"content\" : {\"symbol\" : \"BTC_KRW\",\"tickType\" : \"24H\"," +
                "\"date\" : \"20200129\"," +
                "\"time\" : \"121844\", " +
                "\"openPrice\" : \"2302\", " +
                "\"closePrice\" : \"2317\", " +
                "\"lowPrice\" : \"2272\", " +
                "\"highPrice\" : \"2344\", " +
                "\"value\" : \"2831915078.07065789\", " +
                "\"volume\" : \"1222314.51355788\", " +
                "\"sellVolume\" : \"760129.34079004\", " +
                "\"buyVolume\" : \"462185.17276784\", " +
                "\"prevClosePrice\" : \"2326\", " +
                "\"chgRate\" : \"0.65\", " +
                "\"chgAmt\" : \"15\", " +
                "\"volumePower\" : \"60.80\"}}";
        Flux<String> tick = Flux.from(Mono.just(tickerExample));
        //  tick.doOnNext(System.out::println).subscribe();
        tick.map(message -> {
                    try {
                        return objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                .readValue(objectMapper.readTree(message).get("content").toString(), Ticker.class);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .doOnNext(ticker -> tickerRepository.save(ticker).log().subscribe()).subscribe();

    }

    @Test
    @DisplayName("test flux message to Mongo")
    void testWebSocketMessagetoMongo() {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        client.execute(
                        URI.create("wss://pubwss.bithumb.com/pub/ws"),
                        session -> session.send(
                                        Mono.just(session.textMessage("{'type':'ticker', 'symbols': ['ETH_KRW'],'tickTypes':['30M']}")))
                                .thenMany(session.receive().map(WebSocketMessage::getPayloadAsText)).log()
                                .filter(text -> text.startsWith("{\"type\":\"ticker\""))
                                .map(message -> {
                                    try {
                                        return objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                .readValue(objectMapper.readTree(message).get("content").toString(), Ticker.class);
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }).doOnNext(ticker -> tickerRepository.save(ticker).subscribe())
                                .then())
                .subscribe();
    }
}
