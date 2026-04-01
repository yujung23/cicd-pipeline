package dev.reactive_stream.trade.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeLog {
    private String symbol;       // "BTCUSDT"
    private double price;        // 체결가
    private double quantity;     // 체결량
    private boolean isBuy;       // true=매수, false=매도
    private long tradeTime;      // 체결 시각 (timestamp)
    private long tradeId;        // 체결 ID
}