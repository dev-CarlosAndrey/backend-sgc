package br.com.fpo.sgc.domain.user.mapper;

import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.dto.UserRequestDTO;
import br.com.fpo.sgc.domain.user.dto.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRequestDTO dto, String encodedPassword) {
        if (dto == null) return null;

        return User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(encodedPassword)
                .phone(dto.phone())
                .role(dto.role())
                .active(true)
                .build();
    }

    public User toEntity(UserRequestDTO dto) {
        return toEntity(dto, dto != null ? dto.password() : null);
    }

    public UserResponseDTO toDTO(User user) {
        if (user == null) return null;

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt()
        );
    }
}