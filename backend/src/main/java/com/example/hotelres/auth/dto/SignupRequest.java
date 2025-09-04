package com.example.hotelres.auth.dto;

import com.example.hotelres.user.User;
import jakarta.validation.constraints.*;
import lombok.Getter; import lombok.Setter;
import java.time.LocalDate;

@Getter @Setter
public class SignupRequest {
    @Pattern(regexp="^[a-zA-Z0-9]{6,}$")
    private String loginId;

    @Size(min=8, max=64)
    private String password;

    @NotBlank private String name;
    @Email @NotBlank private String email;
    private String phone;
    private String address1;
    private String address2;
    private String postcode; // 선택
    private User.Gender gender = User.Gender.UNKNOWN;
    private LocalDate birthDate;
    private String verificationCode;
}
