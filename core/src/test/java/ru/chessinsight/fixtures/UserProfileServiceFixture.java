package ru.chessinsight.fixtures;

import ru.chessinsight.application.common.logger.service.Logger;
import ru.chessinsight.application.user.service.impl.DefaultUserProfileService;
import ru.chessinsight.domain.user.model.User;
import ru.chessinsight.infrastructure.security.service.PasswordHasher;

public class UserProfileServiceFixture {
    public final InMemoryUserRepository repository = new InMemoryUserRepository();
    public final PasswordHasher passwordHasher = new PasswordHasher() {
        @Override
        public String hash(String raw) {
            return "hashed:" + raw;
        }

        @Override
        public boolean verify(String raw, String hash) {
            return ("hashed:" + raw).equals(hash);
        }
    };
    public final Logger logger = new Logger() {
        @Override
        public void info(String message) {}

        @Override
        public void warning(String message) {}

        @Override
        public void error(String message) {}
    };
    public final DefaultUserProfileService service =
            new DefaultUserProfileService(repository, passwordHasher, logger);

    public User seed(User user) {
        return repository.save(user);
    }
}
