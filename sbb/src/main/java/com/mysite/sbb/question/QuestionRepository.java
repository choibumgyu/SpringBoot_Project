package com.mysite.sbb.question;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    Question findBySubject(String subject);
    Question findBySubjectAndContent(String subject, String content);
    List<Question> findBySubjectLike(String subject);
    Page<Question> findAll(Pageable pageable);
    List<Question> findByAuthor_Username(String username);
    @Query("select q from Question q join q.voter v where v.username = :username order by q.createDate desc")
    List<Question> findVotedByUsername(@Param("username") String username);
    Page<Question> findAll(Specification<Question> spec, Pageable pageable);
}
