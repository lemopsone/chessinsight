package ru.chessinsight.domain.game.model;

public enum GameResult {
    WHITE_WIN {
        public String toString() {
            return "1-0";
        }
    },
    BLACK_WIN {
        public String toString() {
            return "0-1";
        }
    },
    DRAW {
        public String toString() {
            return "1/2-1/2";
        }
    },
    UNFINISHED {
        public String toString() {
            return "*";
        }
    }

}
