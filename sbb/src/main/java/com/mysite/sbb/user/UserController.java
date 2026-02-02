package com.mysite.sbb.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import java.security.Principal;
import java.util.List;
import com.mysite.sbb.question.QuestionService;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.answer.Answer;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {

	private final UserService userService;
	private final AnswerService answerService;
	private final QuestionService questionService;
	
	@GetMapping("/login")
    public String login() {
        return "login_form";
    }

	@GetMapping("/signup")
	public String signup(UserCreateForm userCreateForm) {
		return "signup_form";
	}

	@PostMapping("/signup")
	public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return "signup_form";
		}

		if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
			bindingResult.rejectValue("password2", "passwordInCorrect", "2개의 패스워드가 일치하지 않습니다.");
			return "signup_form";
		}

		try {
			userService.create(userCreateForm.getUsername(), userCreateForm.getEmail(), userCreateForm.getPassword1());
		} catch (DataIntegrityViolationException e) {
			e.printStackTrace();
			bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
			return "signup_form";
		} catch (Exception e) {
			e.printStackTrace();
			bindingResult.reject("signupFailed", e.getMessage());
			return "signup_form";
		}

		return "redirect:/";
	}
	
	@PreAuthorize("isAuthenticated()")
    @GetMapping("/mypage")
    public String myPage(Model model, Principal principal) {
        String username = principal.getName();
        SiteUser user = userService.getUser(username);
        // 사용자가 작성한 질문과 답변 가져오기
        List<Question> questions = questionService.getQuestionsByAuthor(username);
        List<Answer> answers = answerService.getAnswersByAuthor(username);
        List<Question> likedQuestions = questionService.getVotedQuestionsByUsername(username);
        
        model.addAttribute("name",username);
        model.addAttribute("questions", questions);
        model.addAttribute("answers", answers);
        model.addAttribute("likedQuestions", likedQuestions); 
        String profileImageUrl = null;
        if (user.getProfileImagePath() != null) {
            profileImageUrl = "/uploads/" + user.getProfileImagePath();
        }
        model.addAttribute("profileImageUrl", profileImageUrl);
        return "mypage";
    }
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/mypage/profile-image")
	public String uploadProfileImage(@RequestParam("profileImage") MultipartFile file,
	                                 Principal principal) {
	    String username = principal.getName();
	    userService.updateProfileImage(username, file);
	    return "redirect:/user/mypage";
	}
	
}