package dev.reactive_stream.trade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceTradeMessage {

    @JsonProperty("e")
    private String eventType;   // "trade"

    @JsonProperty("s")
    private String symbol;      // "BTCUSDT"

    @JsonProperty("p")
    private String price;       // "96450.00" (String이라 변환 필요!)

    @JsonProperty("q")
    private String quantity;    // "0.0821"

    @JsonProperty("m")
    private boolean isMaker;    // false=매수, true=매도

    @JsonProperty("T")
    private long tradeTime;     // timestamp

    @JsonProperty("t")
    private long tradeId;
}