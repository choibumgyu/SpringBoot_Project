package com.mysite.sbb.ai.repository;

import com.mysite.sbb.ai.entity.OutboxEvent;
import com.mysite.sbb.ai.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop10ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}