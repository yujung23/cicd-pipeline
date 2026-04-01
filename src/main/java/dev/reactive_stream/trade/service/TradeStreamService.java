package dev.reactive_stream.trade.service;


import dev.reactive_stream.trade.client.BinanceWebSocketClient;
import dev.reactive_stream.trade.domain.TradeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BufferOverflowStrategy; // ✅ 이걸로 교체
import reactor.core.publisher.Flux;
// import reactor.core.publisher.FluxSink; ← 이거 삭제

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeStreamService {

    private final BinanceWebSocketClient wsClient;

    @Value("${stream.buffer-size:500}")
    private int bufferSize;

    @Value("${stream.limit-rate:20}")
    private int limitRate;

    private final AtomicLong bufferDropCount  = new AtomicLong();
    private final AtomicLong latestDropCount  = new AtomicLong();
    private final AtomicLong limitDropCount   = new AtomicLong();

    // 전략 1 — Buffer
    public Flux<TradeLog> streamWithBuffer() {
        return wsClient.getStream()
                .onBackpressureBuffer(
                        bufferSize,
                        dropped -> {
                            bufferDropCount.incrementAndGet();
                            log.warn("[Buffer] DROP: {} @ {}",
                                    dropped.getSymbol(), dropped.getPrice());
                        },
                        BufferOverflowStrategy.DROP_OLDEST // ✅ 수정
                );
    }

    // 전략 2 — Latest (Drop)
    public Flux<TradeLog> streamWithLatest() {
        return wsClient.getStream()
                .onBackpressureLatest()
                .doOnDiscard(TradeLog.class, dropped -> {
                    latestDropCount.incrementAndGet();
                    log.debug("[Latest] DROP: {}", dropped.getSymbol());
                });
    }

    // 전략 3 — LimitRate
    public Flux<TradeLog> streamWithLimitRate() {
        return wsClient.getStream()
                .limitRate(limitRate);
    }

    // 전략 파라미터로 선택
    public Flux<TradeLog> getStream(String strategy) {
        log.info("전략 선택: {}", strategy);
        return switch (strategy) {
            case "latest"    -> streamWithLatest();
            case "limitRate" -> streamWithLimitRate();
            default          -> streamWithBuffer();
        };
    }

    // 드롭 카운터 조회
    public java.util.Map<String, Long> getDropCounts() {
        return java.util.Map.of(
                "buffer",    bufferDropCount.get(),
                "latest",    latestDropCount.get(),
                "limitRate", limitDropCount.get()
        );
    }
}