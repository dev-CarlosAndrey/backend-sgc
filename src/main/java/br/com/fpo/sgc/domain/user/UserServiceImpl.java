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

import java.util.UUID;

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

    @Override
    @Transactional
    public User findOrCreateByPhone(String phone, String displayName) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            String name = resolveUniqueName(displayName, phone);
            // E-mail e senha não têm uso real aqui (ninguém loga no painel com esta conta); só preenchem as
            // colunas NOT NULL/UNIQUE da tabela. O e-mail é determinístico a partir do telefone (que já é
            // único por construção, por vir de findByPhone) para nunca colidir com um e-mail real.
            User user = User.builder()
                    .name(name)
                    .email("whatsapp+" + phone + "@sgc.local")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .phone(phone)
                    .role(Role.USER)
                    .active(true)
                    .build();

            User savedUser = userRepository.save(user);

            auditLogService.log(
                    "User",
                    savedUser.getId(),
                    "CREATE",
                    "WHATSAPP",
                    "Usuário " + savedUser.getName() + " criado automaticamente ao abrir chamado pelo WhatsApp (telefone " + phone + ")"
            );

            return savedUser;
        });
    }

    // O login busca por name (RF01, campo "name" é o username) esperando um único usuário ativo por nome
    // (UserDetailsServiceCustom/AuthController usam findByNameAndActiveTrue, que lança
    // NonUniqueResultException se houver mais de um). "name" não tem UNIQUE no banco, então nada impede
    // duas pessoas reais de serem cadastradas com o mesmo nome — mas aqui, na criação automática via
    // WhatsApp, o nome vem sem curadoria nenhuma (é o nome de exibição do contato no WhatsApp da pessoa),
    // então o risco de colidir com um usuário já cadastrado é real (ex.: duas pessoas chamadas "Andrey").
    // Por isso, diferente do cadastro manual (create()), aqui sempre conferimos antes de usar o nome puro.
    private String resolveUniqueName(String displayName, String phone) {
        String base = (displayName != null && !displayName.isBlank()) ? displayName.trim() : null;
        if (base == null) {
            return "Contato WhatsApp " + phone;
        }
        boolean collidesWithExistingLogin = userRepository.findByNameAndActiveTrue(base).isPresent();
        return collidesWithExistingLogin ? base + " (WhatsApp " + phone + ")" : base;
    }
}