package com.mysite.sbb.ai.entity;

import com.mysite.sbb.ai.enums.BriefingStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "briefing_request",
    indexes = {
        @Index(name = "idx_briefing_request_status", columnList = "status"),
        @Index(name = "idx_briefing_request_created_at", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BriefingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "current_price")
    private Long currentPrice;

    @Column(name = "prev_close")
    private Long prevClose;

    @Lob
    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT")
    private String requestPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BriefingStatus status;

    @Lob
    @Column(name = "result_payload", columnDefinition = "TEXT")
    private String resultPayload;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public BriefingRequest(String stockName,
                           Long currentPrice,
                           Long prevClose,
                           String requestPayload,
                           BriefingStatus status,
                           String resultPayload,
                           String errorMessage,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        this.stockName = stockName;
        this.currentPrice = currentPrice;
        this.prevClose = prevClose;
        this.requestPayload = requestPayload;
        this.status = status;
        this.resultPayload = resultPayload;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void markProcessing(LocalDateTime now) {
        this.status = BriefingStatus.PROCESSING;
        this.updatedAt = now;
        this.errorMessage = null;
    }

    public void markCompleted(String resultPayload, LocalDateTime now) {
        this.status = BriefingStatus.COMPLETED;
        this.resultPayload = resultPayload;
        this.errorMessage = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, LocalDateTime now) {
        this.status = BriefingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = now;
    }
}