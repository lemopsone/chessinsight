CREATE TABLE "user" (
                        id UUID PRIMARY KEY,
                        login TEXT NOT NULL,
                        email TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_user_login ON "user" (login);
CREATE UNIQUE INDEX uq_user_email ON "user" (email);

CREATE TABLE user_role (
                           user_id UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                           role TEXT NOT NULL,
                           PRIMARY KEY (user_id, role)
);

CREATE INDEX idx_user_role_role ON user_role (role);

CREATE TABLE user_statistics (
                                 user_id UUID PRIMARY KEY REFERENCES "user"(id) ON DELETE CASCADE,
                                 accuracy DOUBLE PRECISION,
                                 accuracy_white DOUBLE PRECISION,
                                 accuracy_black DOUBLE PRECISION
);

CREATE INDEX idx_user_statistics_user_id ON user_statistics (user_id);

CREATE TABLE game (
                      id UUID PRIMARY KEY,
                      user_id UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                      event TEXT,
                      site TEXT,
                      date DATE,
                      round TEXT,
                      white_name TEXT,
                      black_name TEXT,
                      result TEXT,
                      pgn TEXT
);

CREATE INDEX idx_game_user_id ON game (user_id);
CREATE INDEX idx_game_pgn ON game (pgn);

CREATE TABLE game_move (
                           id UUID NOT NULL PRIMARY KEY,
                           game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
                           ply_index INTEGER NOT NULL,
                           san TEXT,
                           uci TEXT,
                           position_fen TEXT,
                           comment_before TEXT,
                           comment_after TEXT,
                           analysis_eval_cp INTEGER,
                           analysis_mate_score INTEGER,
                           analysis_best_uci TEXT,
                           analysis_cp_loss INTEGER,
                           analysis_category TEXT
);

CREATE INDEX idx_game_move_game_id ON game_move (game_id);

CREATE TABLE game_analysis (
                               game_id UUID PRIMARY KEY REFERENCES game(id) ON DELETE CASCADE,
                               accuracy_white DOUBLE PRECISION,
                               accuracy_black DOUBLE PRECISION,
                               inaccuracies INTEGER,
                               mistakes INTEGER,
                               blunders INTEGER,
                               analyzed_at TIMESTAMPTZ
);

CREATE INDEX idx_game_analysis_game_id ON game_analysis (game_id);

CREATE TABLE training_scenario (
                                   id UUID PRIMARY KEY,
                                   user_id UUID NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
                                   game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
                                   position_fen TEXT NOT NULL,
                                   pv_san TEXT,
                                   pv_uci TEXT,
                                   prompt TEXT,
                                   completed BOOLEAN NOT NULL DEFAULT FALSE,
                                   completed_at TIMESTAMPTZ
);

CREATE INDEX idx_training_scenario_user_id ON training_scenario (user_id);
CREATE INDEX idx_training_scenario_game_id ON training_scenario (game_id);
CREATE INDEX idx_training_scenario_user_completed ON training_scenario (user_id, completed);
CREATE INDEX idx_training_scenario_game_completed ON training_scenario (game_id, completed);
