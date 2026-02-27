package com.mysite.sbb.stock;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mysite.sbb.news.NaverNewsResponse;
import com.mysite.sbb.news.NaverNewsService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class StockController {

    private final NaverNewsService naverNewsService;
    // private final StockService stockService; // 기존 가격 조회 서비스 (있다면)

    @GetMapping("/stock")
    public String stockPage() {
        return "stock";
    }

    @GetMapping("/stock/view")
    public String stockView(@RequestParam("keyword")String keyword, Model model) {

        // (1) 기존 가격 조회 로직 (나중에 다시 연결)
        // StockPriceDto price = stockService.getPrice(keyword);
        // model.addAttribute("price", price);

        // (2) 뉴스 조회
        List<NaverNewsResponse.NaverNewsItem> news =
                naverNewsService.searchNews(keyword, 10);

        model.addAttribute("newsList", news);
        model.addAttribute("keyword", keyword);

        return "stock";
    }
}
