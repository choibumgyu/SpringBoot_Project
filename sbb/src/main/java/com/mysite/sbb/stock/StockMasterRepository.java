package com.mysite.sbb.stock;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMasterRepository extends JpaRepository<StockMaster, String> {

    // Postgres에서 대소문자 무시 검색: lower(name) like lower('%keyword%')
    @Query("""
        select s
        from StockMaster s
        where s.isActive = true
          and lower(s.name) like lower(concat('%', :keyword, '%'))
        order by s.name asc
    """)
    List<StockMaster> searchByName(@Param("keyword")String keyword);
}
