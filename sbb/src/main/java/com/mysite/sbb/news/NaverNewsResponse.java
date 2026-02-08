// NaverNewsResponse.java
package com.mysite.sbb.news;

import java.util.List;

public record NaverNewsResponse(
        List<NaverNewsItem> items
) {
    public record NaverNewsItem(
            String title,
            String link,
            String description,
            String pubDate
    ) {}
}
