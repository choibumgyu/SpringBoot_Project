package com.mysite.sbb.question;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Set;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.user.SiteUser;
import java.util.HashSet;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
@Getter
@Setter
@Entity
public class Question {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(length = 200)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String content;

	private LocalDateTime createDate;

	@OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE)
	private List<Answer> answerList;
	
	@ManyToOne
    private SiteUser author;
	
	private LocalDateTime modifyDate;
	
	private boolean anonymous; // 익명 여부
	
	@Column(nullable = false)
    private int viewCount = 0; // 조회수 필드
	
	@ManyToMany
	@JoinTable(
	    name = "question_voter",
	    joinColumns = @JoinColumn(name = "question_id"),
	    inverseJoinColumns = @JoinColumn(name = "voter_id")
	)
	Set<SiteUser>voter= new HashSet<>();
}