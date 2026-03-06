package ru.chessinsight.infrastructure.web.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveAnalysis;
import ru.chessinsight.application.game.analysis.engine.dto.EngineMoveRequest;
import ru.chessinsight.application.game.dto.MoveAnalysisDTO;
import ru.chessinsight.application.game.dto.MoveDTO;

@Aspect
@Component
public class MoveEvaluationsTracingAspect {

    private final Tracer tracer;

    public MoveEvaluationsTracingAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    @Around(
            value = "execution(* ru.chessinsight.infrastructure.web.controller.AnalysisController.analyzeMove(..)) && args(body)",
            argNames = "joinPoint,body"
    )
    public Object traceAnalyzeMoveController(
            ProceedingJoinPoint joinPoint,
            ru.chessinsight.infrastructure.web.dto.MoveDTO body
    ) throws Throwable {
        Span span = tracer.nextSpan().name("move.analysis.http.handler");
        if (body != null) {
            span.tag("http.move.num", String.valueOf(body.getMoveNum()));
            span.tag("http.move.has.san", String.valueOf(isNonBlank(body.getMoveSAN())));
            span.tag("http.move.has.uci", String.valueOf(isNonBlank(body.getMoveUCI())));
        }
        return inSpan(joinPoint, span, (s, r) -> {});
    }

    @Around(
            value = "execution(* ru.chessinsight.infrastructure.web.mapper.AnalysisApiMapper.toAppMove(..)) && args(body)",
            argNames = "joinPoint,body"
    )
    public Object traceRequestMapping(ProceedingJoinPoint joinPoint, ru.chessinsight.infrastructure.web.dto.MoveDTO body) throws Throwable {
        Span span = tracer.nextSpan().name("move.analysis.mapping.request");
        if (body != null) {
            span.tag("mapper.request.move.num", String.valueOf(body.getMoveNum()));
        }
        return inSpan(joinPoint, span, (s, r) -> {});
    }

    @Around(
            value = "execution(* ru.chessinsight.infrastructure.web.mapper.AnalysisApiMapper.toApiMoveAnalysis(..)) && args(body)",
            argNames = "joinPoint,body"
    )
    public Object traceResponseMapping(ProceedingJoinPoint joinPoint, MoveAnalysisDTO body) throws Throwable {
        Span span = tracer.nextSpan().name("move.analysis.mapping.response");
        if (body != null) {
            span.tag("mapper.response.mate.present", String.valueOf(body.mateScore() != null));
            span.tag("mapper.response.best.present", String.valueOf(body.bestMove() != null));
        }
        return inSpan(joinPoint, span, (s, r) -> {});
    }

    @Around(
            value = "execution(* ru.chessinsight.application.game.analysis.service.impl."
                    + "DefaultAnalysisService.analyzeMove(..)) && args(move)",
            argNames = "joinPoint,move"
    )
    public Object traceAnalyzeMoveService(ProceedingJoinPoint joinPoint, MoveDTO move) throws Throwable {
        Span span = tracer.nextSpan().name("move.analysis.workflow");
        tagMoveInput(span, move);
        return inSpan(joinPoint, span, this::tagMoveAnalysisResult);
    }

    @Around(
            value = "execution(* ru.chessinsight.application.game.analysis.engine.ChessEngine.analyzeMove(..)) && args(request)",
            argNames = "joinPoint,request"
    )
    public Object traceEngineAnalyzeMove(ProceedingJoinPoint joinPoint, EngineMoveRequest request) throws Throwable {
        Span span = tracer.nextSpan().name("move.analysis.engine.call");
        span.tag("engine.impl", joinPoint.getTarget().getClass().getSimpleName());
        if (request != null) {
            span.tag("engine.depth", String.valueOf(request.depth()));
            span.tag("engine.movetime.ms", String.valueOf(request.movetimeMs()));
            span.tag("engine.nodes", String.valueOf(request.nodes()));
            span.tag("engine.pv.limit", String.valueOf(request.pvLimit()));
        }
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            runChildSpan("move.analysis.engine.request.prepare", child -> {
                if (request != null) {
                    int fenLength = request.positionFEN() == null ? 0 : request.positionFEN().length();
                    child.tag("engine.request.fen.length", String.valueOf(fenLength));
                    child.tag("engine.request.played.uci", String.valueOf(request.playedMoveUci()));
                }
            });

            Object result = runChildSpanResult("move.analysis.engine.execute", joinPoint::proceed);

            runChildSpan("move.analysis.engine.response.process", child -> tagEngineResult(child, result));
            span.tag("result.status", "ok");
            return result;
        } catch (Throwable t) {
            span.error(t);
            span.tag("result.status", "error");
            span.tag("error.type", t.getClass().getSimpleName());
            throw t;
        } finally {
            span.end();
        }
    }

    private Object inSpan(ProceedingJoinPoint joinPoint, Span span, ResultTagger resultTagger) throws Throwable {
        try (Tracer.SpanInScope ignored = tracer.withSpan(span.start())) {
            Object result = joinPoint.proceed();
            resultTagger.tag(span, result);
            span.tag("result.status", "ok");
            return result;
        } catch (Throwable t) {
            span.error(t);
            span.tag("result.status", "error");
            span.tag("error.type", t.getClass().getSimpleName());
            throw t;
        } finally {
            span.end();
        }
    }

    private void tagMoveInput(Span span, MoveDTO move) {
        if (move == null) {
            return;
        }
        span.tag("move.num", String.valueOf(move.moveNum()));
        span.tag("move.has.san", String.valueOf(isNonBlank(move.moveSAN())));
        span.tag("move.has.uci", String.valueOf(isNonBlank(move.moveUCI())));
        span.tag("move.fen.length", String.valueOf(move.positionFEN() == null ? 0 : move.positionFEN().length()));
    }

    private void tagMoveAnalysisResult(Span span, Object result) {
        if (!(result instanceof MoveAnalysisDTO dto)) {
            return;
        }
        span.tag("analysis.best.move.present", String.valueOf(dto.bestMove() != null));
        span.tag("analysis.mate.present", String.valueOf(dto.mateScore() != null));
        span.tag("analysis.player.eval.present", String.valueOf(dto.playerMoveEval() != null));
    }

    private void tagEngineResult(Span span, Object result) {
        if (!(result instanceof EngineMoveAnalysis dto)) {
            return;
        }
        span.tag("engine.best.move.present", String.valueOf(isNonBlank(dto.bestMoveUci())));
        span.tag("engine.cp.loss.present", String.valueOf(dto.cpLoss() != null));
        span.tag("engine.mate.present", String.valueOf(dto.mateScore() != null));
    }

    private boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void runChildSpan(String name, SpanTagger tagger) {
        Span child = tracer.nextSpan().name(name);
        try (Tracer.SpanInScope ignored = tracer.withSpan(child.start())) {
            tagger.tag(child);
            child.tag("result.status", "ok");
        } catch (RuntimeException ex) {
            child.error(ex);
            child.tag("result.status", "error");
            child.tag("error.type", ex.getClass().getSimpleName());
            throw ex;
        } finally {
            child.end();
        }
    }

    private Object runChildSpanResult(String name, ThrowingSupplier supplier) throws Throwable {
        Span child = tracer.nextSpan().name(name);
        try (Tracer.SpanInScope ignored = tracer.withSpan(child.start())) {
            Object result = supplier.get();
            child.tag("result.status", "ok");
            return result;
        } catch (Throwable t) {
            child.error(t);
            child.tag("result.status", "error");
            child.tag("error.type", t.getClass().getSimpleName());
            throw t;
        } finally {
            child.end();
        }
    }

    @FunctionalInterface
    private interface SpanTagger {
        void tag(Span span);
    }

    @FunctionalInterface
    private interface ResultTagger {
        void tag(Span span, Object result);
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }
}
