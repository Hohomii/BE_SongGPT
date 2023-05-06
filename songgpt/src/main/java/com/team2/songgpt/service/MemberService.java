package com.team2.songgpt.service;

import com.team2.songgpt.dto.member.*;
import com.team2.songgpt.entity.Member;
import com.team2.songgpt.entity.RefreshToken;
import com.team2.songgpt.global.jwt.JwtUtil;
import com.team2.songgpt.repository.MemberRepository;
import com.team2.songgpt.repository.RefreshTokenRepository;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.Key;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;


    @Transactional
    public MemberResponseDto getMember(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_TOKEN);
        String userInfo;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                userInfo = jwtUtil.getUserInfoFromToken(token);
                Member member = memberRepository.findByEmail(userInfo).orElseThrow(
                        () -> new IllegalArgumentException("토큰이 유효하지 않습니다.")
                );
                return new MemberResponseDto(member);
            } else {
                throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
            }
        }
        throw new IllegalArgumentException("토큰이 없습니다.");
    }

    @Transactional
    public void signup(@RequestBody SignupRequestDto signupRequestDto) {
        String email = signupRequestDto.getEmail();
        String password = signupRequestDto.getPassword();
        String nickname = signupRequestDto.getNickname();

        password = passwordEncoder.encode(password);

        Optional<Member> findEmail = memberRepository.findByEmail(email);
        if(findEmail.isPresent()) {
            throw new IllegalArgumentException("중복된 email입니다.");
        }

        Optional<Member> findNickname = memberRepository.findByNickname(nickname);
        if(findNickname.isPresent()) {
            throw new IllegalArgumentException("중복된 닉네임입니다.");
        }

        Member member = new Member(email, password, nickname);
        memberRepository.save(member);
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        Member member = memberRepository.findByEmail(email).orElseThrow(
        () -> new IllegalArgumentException("잘못된 email입니다.")
        );

        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        TokenDto tokenDto = jwtUtil.createAllToken(member.getEmail());
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByEmail(member.getEmail());

        if (refreshToken.isPresent()) {
            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
        } else {
            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), email);
            refreshTokenRepository.save(newToken);
        }

        setHeader(response, tokenDto);
        return new LoginResponseDto(member);
    }

    @Transactional
    public String logout(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request, JwtUtil.ACCESS_TOKEN);
        String userInfo;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                userInfo = jwtUtil.getUserInfoFromToken(token);
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication != null && authentication.isAuthenticated() && userInfo.equals(authentication.getName())) {
                    //만료시간이 현재 시간 이전으로 설정된 accessToken을 만들어서 클라이언트에 보냄
                    Date now = new Date();
                    Date expiredDate = new Date(now.getTime() - 1000);
                    String newToken = jwtUtil.createExpiredToken(userInfo, JwtUtil.ACCESS_TOKEN, expiredDate);
                    SecurityContextHolder.getContext().setAuthentication(null);
                    return newToken;
                } else {
                throw new IllegalArgumentException("토큰이 유효하지 않습니다.");
                }
            }
        }
        throw new IllegalArgumentException("토큰이 없습니다.");
    }

    public void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.ACCESS_TOKEN, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_TOKEN, tokenDto.getRefreshToken());
    }
}