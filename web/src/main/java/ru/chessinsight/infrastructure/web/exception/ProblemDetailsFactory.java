package ru.chessinsight.infrastructure.web.exception;

import org.springframework.http.HttpStatus;
import ru.chessinsight.infrastructure.web.dto.ProblemDetails;

public final class ProblemDetailsFactory {
    private static final String DEFAULT_TYPE = "about:blank";

    private ProblemDetailsFactory() {}

    public static ProblemDetails create(HttpStatus status, String detail, String instance) {
        ProblemDetails problem = new ProblemDetails();
        problem.setType(DEFAULT_TYPE);
        problem.setTitle(status.getReasonPhrase());
        problem.setStatus(status.value());
        problem.setDetail(detail);
        problem.setInstance(instance);
        return problem;
    }

    public static ProblemDetails create(HttpStatus status, String detail) {
        return create(status, detail, null);
    }
}
