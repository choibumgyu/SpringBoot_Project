package com.mysite.sbb.answer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Integer>{
	List<Answer> findByAuthor_Username(String username);

}
