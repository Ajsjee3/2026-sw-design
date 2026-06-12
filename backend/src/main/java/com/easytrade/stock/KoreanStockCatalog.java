package com.easytrade.stock;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class KoreanStockCatalog {

    private final List<StockInfo> stocks = List.of(
            new StockInfo("005930", "삼성전자"),
            new StockInfo("000660", "SK하이닉스"),
            new StockInfo("373220", "LG에너지솔루션"),
            new StockInfo("207940", "삼성바이오로직스"),
            new StockInfo("005380", "현대차"),
            new StockInfo("000270", "기아"),
            new StockInfo("068270", "셀트리온"),
            new StockInfo("005490", "POSCO홀딩스"),
            new StockInfo("035420", "NAVER"),
            new StockInfo("105560", "KB금융"),
            new StockInfo("055550", "신한지주"),
            new StockInfo("012330", "현대모비스"),
            new StockInfo("028260", "삼성물산"),
            new StockInfo("051910", "LG화학"),
            new StockInfo("035720", "카카오"),
            new StockInfo("006400", "삼성SDI"),
            new StockInfo("032830", "삼성생명"),
            new StockInfo("086790", "하나금융지주"),
            new StockInfo("138040", "메리츠금융지주"),
            new StockInfo("000810", "삼성화재"),
            new StockInfo("015760", "한국전력"),
            new StockInfo("034020", "두산에너빌리티"),
            new StockInfo("009150", "삼성전기"),
            new StockInfo("096770", "SK이노베이션"),
            new StockInfo("066570", "LG전자"),
            new StockInfo("017670", "SK텔레콤"),
            new StockInfo("033780", "KT&G"),
            new StockInfo("003550", "LG"),
            new StockInfo("018260", "삼성에스디에스"),
            new StockInfo("010130", "고려아연"),
            new StockInfo("259960", "크래프톤"),
            new StockInfo("003670", "포스코퓨처엠"),
            new StockInfo("011200", "HMM"),
            new StockInfo("024110", "기업은행"),
            new StockInfo("034730", "SK"),
            new StockInfo("316140", "우리금융지주"),
            new StockInfo("009540", "HD한국조선해양"),
            new StockInfo("267260", "HD현대일렉트릭"),
            new StockInfo("010140", "삼성중공업"),
            new StockInfo("042660", "한화오션"),
            new StockInfo("047810", "한국항공우주"),
            new StockInfo("010120", "LS ELECTRIC"),
            new StockInfo("011070", "LG이노텍"),
            new StockInfo("030200", "KT"),
            new StockInfo("352820", "하이브"),
            new StockInfo("251270", "넷마블"),
            new StockInfo("036570", "엔씨소프트"),
            new StockInfo("377300", "카카오페이"),
            new StockInfo("323410", "카카오뱅크"),
            new StockInfo("402340", "SK스퀘어"),
            new StockInfo("086520", "에코프로"),
            new StockInfo("247540", "에코프로비엠"),
            new StockInfo("091990", "셀트리온헬스케어"),
            new StockInfo("196170", "알테오젠"),
            new StockInfo("028300", "HLB"),
            new StockInfo("041510", "에스엠"),
            new StockInfo("035900", "JYP Ent."),
            new StockInfo("263750", "펄어비스"),
            new StockInfo("112040", "위메이드"),
            new StockInfo("293490", "카카오게임즈")
    );

    public List<StockInfo> all() {
        return stocks;
    }

    public List<StockInfo> search(String query) {
        if (query == null || query.isBlank()) {
            return stocks;
        }

        String normalizedQuery = query.toLowerCase(Locale.KOREA).trim();
        return stocks.stream()
                .filter(stock -> stock.code().contains(normalizedQuery)
                        || stock.name().toLowerCase(Locale.KOREA).contains(normalizedQuery))
                .toList();
    }

    public record StockInfo(String code, String name) {
    }
}
