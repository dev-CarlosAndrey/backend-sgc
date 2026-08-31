package br.com.fpo.sgc.domain.user;

import br.com.fpo.sgc.domain.user.dto.UserRequestDTO;
import br.com.fpo.sgc.domain.user.dto.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDTO create(UserRequestDTO dto);

    UserResponseDTO findById(Long id);

    Page<UserResponseDTO> findAll(Pageable pageable);

    UserResponseDTO update(Long id, UserRequestDTO dto);

    void delete(Long id);
}