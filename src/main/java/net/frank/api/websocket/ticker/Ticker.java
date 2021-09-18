package net.frank.api.websocket.ticker;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document (collection = "tickers")
public class Ticker {
    @Id private String id;
    String tickType;
    String date;
    String time;
    int openPrice;
    int closePrice;
    int lowPrice;
    int highPrice;
    double value;
    double volume;
    double volumePower;
}
