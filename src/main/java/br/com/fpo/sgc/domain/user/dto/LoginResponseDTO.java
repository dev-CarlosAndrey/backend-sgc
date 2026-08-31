package br.com.fpo.sgc.domain.user.dto;

import br.com.fpo.sgc.domain.user.Role;

public record LoginResponseDTO(
        String token,
        Long userId,
        String name,
        String email,
        Role role
) {}