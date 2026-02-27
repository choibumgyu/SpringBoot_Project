package com.mysite.sbb.ai;

import java.util.List;

public record StockBriefingRequest(
    String stockName,
    double currentPrice,
    double prevClose,
    List<NewsItem> news
) {
  public record NewsItem(String title, String description, String link) {}
}