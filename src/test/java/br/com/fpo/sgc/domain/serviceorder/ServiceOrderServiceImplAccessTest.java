package br.com.fpo.sgc.domain.serviceorder;

import br.com.fpo.sgc.domain.audit.AuditLogService;
import br.com.fpo.sgc.domain.serviceorder.dto.ServiceOrderRequestDTO;
import br.com.fpo.sgc.domain.serviceorder.mapper.ServiceOrderMapper;
import br.com.fpo.sgc.domain.serviceorder.statuslog.ServiceOrderStatusLogRepository;
import br.com.fpo.sgc.domain.user.User;
import br.com.fpo.sgc.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

// Escopo por dono (RF16/RNF22): o solicitante (USER) só vê e abre as próprias OS; ADMIN e TECHNICIAN veem
// todas; sem usuário logado (fluxo do webhook do WhatsApp) não há restrição aqui.
class ServiceOrderServiceImplAccessTest {

    private ServiceOrderRepository repository;
    private UserRepository userRepository;
    private ServiceOrderServiceImpl service;

    private final User ana = User.builder().id(1L).name("ana").build();
    private final User bruno = User.builder().id(2L).name("bruno").build();
    private final ServiceOrder osDaAna = ServiceOrder.builder().id(10L).requester(ana).build();
    private final ServiceOrder osDoBruno = ServiceOrder.builder().id(20L).requester(bruno).build();
    private final Pageable page = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        repository = mock(ServiceOrderRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ServiceOrderServiceImpl(repository, userRepository, null, null,
                mock(ServiceOrderStatusLogRepository.class), mock(AuditLogService.class),
                mock(ApplicationEventPublisher.class), mock(ServiceOrderMapper.class));

        when(userRepository.findByNameAndActiveTrue("ana")).thenReturn(Optional.of(ana));
        when(repository.findById(10L)).thenReturn(Optional.of(osDaAna));
        when(repository.findById(20L)).thenReturn(Optional.of(osDoBruno));
        when(repository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(repository.findByRequesterIdAndActiveTrue(any(), any())).thenReturn(Page.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String name, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                name, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @Test
    void solicitante_naoVeOsDeOutraPessoa() {
        loginAs("ana", "USER");

        assertThrows(AccessDeniedException.class, () -> service.findById(20L));
        assertThrows(AccessDeniedException.class, () -> service.findStatusHistory(20L, page));
    }

    @Test
    void solicitante_veAPropriaOs() {
        loginAs("ana", "USER");

        assertDoesNotThrow(() -> service.findById(10L));
    }

    @Test
    void solicitante_listagemFiltraPeloProprioId() {
        loginAs("ana", "USER");

        service.findAll(null, null, null, page);

        verify(repository).search(isNull(), isNull(), isNull(), eq(1L), eq(page));
    }

    @Test
    void solicitante_naoConsultaOsDeOutroSolicitante() {
        loginAs("ana", "USER");

        assertThrows(AccessDeniedException.class, () -> service.findByRequesterId(2L, page));
        assertDoesNotThrow(() -> service.findByRequesterId(1L, page));
    }

    @Test
    void solicitante_naoAbreOsEmNomeDeOutraPessoa() {
        loginAs("ana", "USER");
        ServiceOrderRequestDTO emNomeDoBruno = new ServiceOrderRequestDTO(
                "Projetor sem imagem", "Descrição", ServiceOrderPriority.MEDIUM, 2L, null, 5L);

        assertThrows(AccessDeniedException.class, () -> service.create(emNomeDoBruno));
        verify(repository, never()).save(any());
    }

    @Test
    void tecnicoEAdmin_veemTodasAsOs() {
        loginAs("tecnico", "TECHNICIAN");
        assertDoesNotThrow(() -> service.findById(20L));

        loginAs("admin", "ADMIN");
        service.findAll(null, null, null, page);
        verify(repository).search(isNull(), isNull(), isNull(), isNull(), eq(page));
    }

    @Test
    void semUsuarioLogado_fluxoDoWhatsApp_naoRestringe() {
        assertDoesNotThrow(() -> service.findById(20L));
    }
}
