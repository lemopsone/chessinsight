package ru.chessinsight.infrastructure.cli.api.dto;

import java.util.UUID;

public class ApiGameSummary {
    private UUID id;
    private String date;
    private String result;
    private String whiteName;
    private String blackName;
    private Integer movesCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getWhiteName() { return whiteName; }
    public void setWhiteName(String whiteName) { this.whiteName = whiteName; }
    public String getBlackName() { return blackName; }
    public void setBlackName(String blackName) { this.blackName = blackName; }
    public Integer getMovesCount() { return movesCount; }
    public void setMovesCount(Integer movesCount) { this.movesCount = movesCount; }
}
