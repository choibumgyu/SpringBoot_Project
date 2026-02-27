package com.mysite.sbb.news;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class NewsApiController {

    private final NaverNewsService naverNewsService;

    @GetMapping("/api/news")
    public List<NaverNewsResponse.NaverNewsItem> getNews(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "display", defaultValue = "100") int display
    ) {
        return naverNewsService.searchNews(keyword, display);
    }
}
