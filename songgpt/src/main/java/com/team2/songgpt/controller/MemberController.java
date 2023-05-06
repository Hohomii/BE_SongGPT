package com.team2.songgpt.controller;

import com.team2.songgpt.dto.member.LoginRequestDto;
import com.team2.songgpt.dto.member.LoginResponseDto;
import com.team2.songgpt.dto.member.MemberResponseDto;
import com.team2.songgpt.dto.member.SignupRequestDto;
import com.team2.songgpt.global.dto.ResponseDto;
import com.team2.songgpt.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/")
    public ResponseDto getMember(HttpServletRequest request) {
        MemberResponseDto memberResponseDto = memberService.getMember(request);
        return ResponseDto.setSuccess("회원 정보", memberResponseDto);
    }

    @PostMapping("/register")
    public ResponseDto signup(@Valid @RequestBody SignupRequestDto signupRequestDto) {
        memberService.signup(signupRequestDto);
        return ResponseDto.setSuccess("회원가입 완료", null);
    }

    @PostMapping("/login")
    public ResponseDto login(@RequestBody LoginRequestDto loginRequestDto, HttpServletResponse response) {
        LoginResponseDto loginResponseDto = memberService.login(loginRequestDto, response);
        return ResponseDto.setSuccess("로그인 완료", loginResponseDto);
    }

    @PostMapping("/logout")
    public ResponseDto logout(HttpServletRequest request, HttpServletResponse response) {
        String newToken = memberService.logout(request);
        response.setHeader("Access_Token", newToken);
        return ResponseDto.setSuccess("로그아웃 완료", null);
    }
}
