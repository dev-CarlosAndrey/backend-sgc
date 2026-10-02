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

    /**
     * Busca um usuário pelo telefone (ex.: quem já abriu chamado pelo WhatsApp antes) ou cria um novo com
     * perfil {@code USER}, caso ainda não exista nenhum com esse telefone. Usado pela abertura de OS via
     * WhatsApp (RF15), onde qualquer pessoa pode abrir chamado e é identificada só pelo número que escreveu.
     * Retorna a entidade (não o DTO) porque é consumido internamente por outro domínio, não por um controller.
     */
    User findOrCreateByPhone(String phone, String displayName);
}