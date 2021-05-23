package Lessons;

import java.util.Optional;

/**
 * Сервис авторизации
 */
public interface AuthService {
    /**
     * запустить сервис
     */
    void start();

    /**
     * Остановить сервис
     */
    void stop();

    /**
     * Получить никнейм
     */
    Optional<String> getNickByLoginAndPass(String login, String pass);
}