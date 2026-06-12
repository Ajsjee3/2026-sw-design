package com.easytrade.stock;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.kis.enabled", havingValue = "true")
public class KisStockPriceProvider {

    private static final String CURRENT_PRICE_TR_ID = "FHKST01010100";

    private final RestClient restClient;
    private final KisAccessTokenClient tokenClient;
    private final String appKey;
    private final String appSecret;

    public KisStockPriceProvider(
            @Value("${app.kis.base-url}") String baseUrl,
            @Value("${app.kis.app-key}") String appKey,
            @Value("${app.kis.app-secret}") String appSecret,
            KisAccessTokenClient tokenClient
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .build();
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.tokenClient = tokenClient;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    public Optional<StockPrice> findByCode(String code) {
        try {
            KisPriceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", code)
                            .build())
                    .header("authorization", "Bearer " + tokenClient.getAccessToken())
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", CURRENT_PRICE_TR_ID)
                    .header("custtype", "P")
                    .retrieve()
                    .body(KisPriceResponse.class);

            if (response == null || !"0".equals(response.resultCode()) || response.output() == null) {
                return Optional.empty();
            }

            KisPriceOutput output = response.output();
            return Optional.of(new StockPrice(
                    code,
                    output.stockName() == null || output.stockName().isBlank() ? code : output.stockName(),
                    parseDecimal(output.currentPrice()),
                    parseDecimal(output.changeRate())
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Optional<StockPrice> search(String query) {
        if (query == null || !query.matches("\\d{6}")) {
            return Optional.empty();
        }
        return findByCode(query);
    }

    public List<StockPrice> popularStocks() {
        return List.of("005930", "000660", "035420").stream()
                .map(this::findByCode)
                .flatMap(Optional::stream)
                .toList();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replace(",", "").trim());
    }

    private record KisPriceResponse(
            @JsonProperty("rt_cd") String resultCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") KisPriceOutput output
    ) {
    }

    private record KisPriceOutput(
            @JsonProperty("hts_kor_isnm") String stockName,
            @JsonProperty("stck_prpr") String currentPrice,
            @JsonProperty("prdy_ctrt") String changeRate
    ) {
    }
}

