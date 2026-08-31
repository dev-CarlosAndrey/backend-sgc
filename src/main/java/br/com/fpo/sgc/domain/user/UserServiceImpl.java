package br.com.fpo.sgc.domain.user;

import br.com.fpo.sgc.common.exception.BusinessException;
import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.user.dto.UserRequestDTO;
import br.com.fpo.sgc.domain.user.dto.UserResponseDTO;
import br.com.fpo.sgc.domain.user.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new BusinessException("Já existe um usuário cadastrado com o e-mail: " + dto.email());
        }

        String encodedPassword = passwordEncoder.encode(dto.password());
        User user = userMapper.toEntity(dto, encodedPassword);

        User savedUser = userRepository.save(user);

        // Registro de auditoria
        auditLogService.log(
                "User",
                savedUser.getId(),
                "CREATE",
                savedUser.getEmail(),
                "Usuário " + savedUser.getName() + " cadastrado com perfil " + savedUser.getRole()
        );

        return userMapper.toDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com o ID: " + id));
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findByActiveTrue(pageable)
                .map(userMapper::toDTO);
    }

    @Override
    @Transactional
    public UserResponseDTO update(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com o ID: " + id));

        if (!user.getEmail().equalsIgnoreCase(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw new BusinessException("O e-mail informado já está em uso por outro usuário.");
        }

        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPhone(dto.phone());
        user.setRole(dto.role());

        // Se uma nova senha for informada no DTO, atualiza e criptografa
        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        User updatedUser = userRepository.save(user);

        // Registro de auditoria
        auditLogService.log(
                "User",
                updatedUser.getId(),
                "UPDATE",
                updatedUser.getEmail(),
                "Dados do usuário " + updatedUser.getName() + " atualizados"
        );

        return userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com o ID: " + id));

        // Soft Delete (Inativação lógica do usuário)
        user.setActive(false);
        userRepository.save(user);

        // Registro de auditoria
        auditLogService.log(
                "User",
                id,
                "DELETE",
                user.getEmail(),
                "Usuário " + user.getName() + " inativado no sistema"
        );
    }
}