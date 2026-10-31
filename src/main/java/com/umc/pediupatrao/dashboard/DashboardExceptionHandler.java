package com.umc.pediupatrao.dashboard;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = DashboardController.class)
public class DashboardExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DashboardExceptionHandler.class);
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail invalido(Exception exception) {
        return problema(HttpStatus.BAD_REQUEST, exception instanceof MethodArgumentTypeMismatchException
                ? "Parâmetro inválido. Use datas AAAA-MM-DD e números inteiros para paginação." : exception.getMessage());
    }
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail indisponivel(DataAccessException exception, HttpServletRequest request) {
        log.error("Falha ao consultar dashboard em {}", request.getRequestURI(), exception);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Dados temporariamente indisponíveis. Tente atualizar novamente.");
    }
    private ProblemDetail problema(HttpStatus status, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setTitle(status == HttpStatus.BAD_REQUEST ? "Consulta inválida" : "Serviço indisponível");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
