package br.com.fpo.sgc.common.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFoundException(EntityNotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("Violação de Regra de Negócio");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/business-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Parâmetro de rota/query com valor de tipo incorreto (data mal formatada, enum inexistente...).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + e.getName() + "'.");
        problemDetail.setTitle("Parâmetro Inválido");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/invalid-parameter"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "O arquivo enviado excede o tamanho máximo permitido pelo servidor.");
        problemDetail.setTitle("Arquivo Muito Grande");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/file-too-large"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Erro na validação dos campos.");
        problemDetail.setTitle("Dados Inválidos");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/invalid-data"));
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Corpo da requisição ilegível: JSON malformado, enum inexistente, data em formato inválido em um
    // campo do corpo (o equivalente, para o corpo da requisição, do que handleTypeMismatchException trata
    // para parâmetro de rota/query).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ilegível: verifique se o JSON está bem formado e se os valores (datas, enums) são válidos.");
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/malformed-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Violação de constraint do banco (chave única, FK) não barrada antes por validação da aplicação —
    // tipicamente duas requisições quase simultâneas tentando criar o mesmo registro (ex.: mesmo
    // tombamento, CPF ou CNPJ). Evita vazar detalhe de coluna/constraint do banco na resposta.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("Violação de integridade de dados: {}", e.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Não foi possível concluir a operação: o registro conflita com um dado já existente ou referencia algo inválido.");
        problemDetail.setTitle("Conflito de Dados");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/data-integrity"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Autenticado, mas sem o papel exigido pelo @PreAuthorize do endpoint — antes caía sem handler
    // dedicado e respondia 403 sem corpo; agora segue o mesmo formato ProblemDetail dos demais erros.
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta ação.");
        problemDetail.setTitle("Acesso Negado");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/access-denied"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Login com usuário inexistente, senha errada ou conta inativa (lançado pelo AuthenticationManager
    // no AuthController). Sem este handler, o handler genérico abaixo transformava isso em 500.
    // Mensagem única para os casos, para não revelar se o usuário existe.
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Usuário ou senha incorretos.");
        problemDetail.setTitle("Falha na Autenticação");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/authentication-failed"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // Rede de segurança final: qualquer exceção não prevista pelos handlers acima (NullPointerException,
    // erro de driver JDBC, falha inesperada de biblioteca, etc.) vira um 500 com corpo seguro em vez de
    // propagar a página de erro padrão do Spring — essencial para a aplicação nunca "quebrar visivelmente"
    // em demonstração. O detalhe completo (classe, mensagem, stack trace) vai só para o log do servidor.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception e) {
        log.error("Erro não tratado ao processar a requisição", e);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado ao processar a requisição. Tente novamente; se persistir, contate o suporte.");
        problemDetail.setTitle("Erro Interno");
        problemDetail.setType(URI.create("https://sgc.fpo.edu.br/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}