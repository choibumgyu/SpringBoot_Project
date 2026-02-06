package com.mysite.sbb.stock;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_master")
public class StockMaster {

    @Id
    @Column(length = 6, nullable = false)
    private String code; // 005930

    @Column(length = 500, nullable = false)
    private String name; // 삼성전자

    @Column(length = 12)
    private String isin;

    @Column(length = 10)
    private String market; // KOSPI

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected StockMaster() {}

    public StockMaster(String code, String name, String isin, String market) {
        this.code = code;
        this.name = name;
        this.isin = isin;
        this.market = market;
        this.isActive = true;
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- getters/setters ----
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getIsin() { return isin; }
    public String getMarket() { return market; }
    public Boolean getIsActive() { return isActive; }
    
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setIsin(String isin) { this.isin = isin; }
    public void setMarket(String market) { this.market = market; }
    public void setIsActive(Boolean active) { isActive = active; }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
