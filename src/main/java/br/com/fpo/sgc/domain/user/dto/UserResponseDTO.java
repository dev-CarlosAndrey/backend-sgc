package br.com.fpo.sgc.domain.user.dto;

import br.com.fpo.sgc.domain.user.Role;

import java.time.Instant;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        Boolean active,
        Instant createdAt
) {
}