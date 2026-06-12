package com.easytrade.auth;

import com.easytrade.common.ApiException;
import com.easytrade.user.User;
import com.easytrade.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final BigDecimal INITIAL_BALANCE = BigDecimal.valueOf(10_000_000);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 등록된 이메일입니다.");
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                INITIAL_BALANCE
        );

        return UserResponse.from(userRepository.save(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return new LoginResponse(jwtTokenProvider.createToken(user.getEmail()), UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) {
        return UserResponse.from(user);
    }

    public record RegisterRequest(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String nickname
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(String token, UserResponse user) {
    }

    public record UserResponse(Long id, String name, String email, String nickname, BigDecimal balance) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getNickname(), user.getBalance());
        }
    }
}
