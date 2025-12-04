package ru.chessinsight.infrastructure.cli.api.dto;

public class ApiTrainingMoveResponse {

    private String status;
    private String message;
    private String acceptedMoveUci;
    private String opponentMoveUci;
    private Integer nextCursor;
    private Boolean completed;
    private String hintPvSan;
    private String hintPvUci;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAcceptedMoveUci() { return acceptedMoveUci; }
    public void setAcceptedMoveUci(String acceptedMoveUci) { this.acceptedMoveUci = acceptedMoveUci; }

    public String getOpponentMoveUci() { return opponentMoveUci; }
    public void setOpponentMoveUci(String opponentMoveUci) { this.opponentMoveUci = opponentMoveUci; }

    public Integer getNextCursor() { return nextCursor; }
    public void setNextCursor(Integer nextCursor) { this.nextCursor = nextCursor; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public String getHintPvSan() { return hintPvSan; }
    public void setHintPvSan(String hintPvSan) { this.hintPvSan = hintPvSan; }

    public String getHintPvUci() { return hintPvUci; }
    public void setHintPvUci(String hintPvUci) { this.hintPvUci = hintPvUci; }
}
