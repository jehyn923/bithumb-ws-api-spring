package net.frank.api.websocket.ticker;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends ReactiveCrudRepository<Ticker, String> {
}
