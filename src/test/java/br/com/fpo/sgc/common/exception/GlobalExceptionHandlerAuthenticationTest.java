package br.com.fpo.sgc.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerAuthenticationTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void senhaErradaRetorna401ComMensagemGenerica() {
        ProblemDetail pd = handler.handleAuthenticationException(new BadCredentialsException("Bad credentials"));

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getDetail()).isEqualTo("Usuário ou senha incorretos.");
        assertThat(pd.getType().toString()).endsWith("/errors/authentication-failed");
    }

    @Test
    void contaDesabilitadaUsaAMesmaMensagem() {
        ProblemDetail pd = handler.handleAuthenticationException(new DisabledException("disabled"));

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getDetail()).isEqualTo("Usuário ou senha incorretos.");
    }
}
