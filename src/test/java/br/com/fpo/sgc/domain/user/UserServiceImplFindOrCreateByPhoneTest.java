package br.com.fpo.sgc.domain.user;

import br.com.fpo.sgc.domain.audit.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Usado pela abertura de OS via WhatsApp (RF15): qualquer pessoa pode abrir chamado, identificada só pelo
// telefone que escreveu — um User é criado na hora se ainda não existir nenhum com esse telefone.
class UserServiceImplFindOrCreateByPhoneTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UserServiceImpl(userRepository, passwordEncoder, mock(AuditLogService.class), null);
        when(passwordEncoder.encode(any())).thenReturn("hash");
    }

    @Test
    void telefoneJaCadastrado_retornaUsuarioExistente() {
        User existente = User.builder().id(5L).name("Maria").phone("558899999999").build();
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.of(existente));

        User result = service.findOrCreateByPhone("558899999999", "Maria");

        assertSame(existente, result);
        verify(userRepository, never()).save(any());
    }

    @Test
    void telefoneNovo_criaUsuarioComPerfilUser() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.findOrCreateByPhone("558899999999", "Maria");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("Maria", captor.getValue().getName());
        assertEquals("558899999999", captor.getValue().getPhone());
        assertEquals(Role.USER, captor.getValue().getRole());
        assertTrue(captor.getValue().getActive());
        assertEquals("hash", captor.getValue().getPassword());
        assertNotNull(captor.getValue().getEmail());
        assertEquals(result, captor.getValue());
    }

    @Test
    void telefoneNovoSemNomeDoContato_usaNomeGenerico() {
        when(userRepository.findByPhone("558899999999")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = service.findOrCreateByPhone("558899999999", null);

        assertTrue(result.getName().contains("558899999999"));
    }
}
