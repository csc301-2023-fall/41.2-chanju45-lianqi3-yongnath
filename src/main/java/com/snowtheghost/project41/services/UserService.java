package com.snowtheghost.project41.services;

import com.snowtheghost.project41.api.models.responses.games.GetGameResponse;
import com.snowtheghost.project41.api.models.responses.users.GetUserResponse;
import com.snowtheghost.project41.database.models.Game;
import com.snowtheghost.project41.database.models.User;
import com.snowtheghost.project41.database.repositories.UserRepository;
import com.snowtheghost.project41.utils.EncryptionUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionUtils encryptionUtils;
    private final AuthenticationService authenticationService;

    @Autowired
    public UserService(UserRepository userRepository, EncryptionUtils encryptionUtils, AuthenticationService authenticationService) {
        this.userRepository = userRepository;
        this.encryptionUtils = encryptionUtils;
        this.authenticationService = authenticationService;
    }

    public void createPlayerUser(String userId, String username, String email, String password) {
        createUser(userId, username, email, password, User.Type.PLAYER);
    }

    public void createResearcherUser(String userId, String username, String email, String password) {
        createUser(userId, username, email, password, User.Type.RESEARCHER);
    }

    private void createUser(String userId, String username, String email, String password, User.Type type) {
        User user = new User(userId, username, email, encryptionUtils.encryptPassword(password), encryptionUtils.encryptBalance(0), "", type);
        userRepository.save(user);
    }

    public User getUser(String userId) throws EntityNotFoundException {
        return userRepository.findById(userId).orElseThrow(EntityNotFoundException::new);
    }

    public boolean hasSufficientFunds(User user, Integer amount) {
        int balance = encryptionUtils.decryptBalance(user.getEncryptedBalance());
        return balance > 0 && balance - amount >= 0;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private boolean isValidPassword(User user, String password) {
        return user.getEncryptedPassword().equals(encryptionUtils.encryptPassword(password));
    }

    public void addFunds(String userId, int amount) {
        User user = getUser(userId);
        user.setEncryptedBalance(encryptionUtils.encryptBalance(encryptionUtils.decryptBalance(user.getEncryptedBalance()) + amount));
        userRepository.save(user);
    }

    public void subtractFunds(User user, int amount) {
        user.setEncryptedBalance(encryptionUtils.encryptBalance(encryptionUtils.decryptBalance(user.getEncryptedBalance()) - amount));
        userRepository.save(user);
    }

    public void updateId(User user, String gameId) {
        user.setCurrentGameId(gameId);
        userRepository.save(user);
    }

    public float getBalance(User user) {
        return (float) encryptionUtils.decryptBalance(user.getEncryptedBalance()) / 100;
    }

    public String loginUser(String email, String password) {
        User user = getUserByEmail(email);
        if (user != null && isValidPassword(user, password)) {
            return authenticationService.generateToken(user.getUserId());
        }

        return null;
    }

    public GetUserResponse mapUserToResponse(User user){
        return new GetUserResponse(
                user.getUserId(), user.getUsername(), user.getEmail(), getBalance(user),
                user.getGames().stream().map(gamePlayer -> {
                    Game game = gamePlayer.getGame();
                    return new GetGameResponse(
                            game.getGameId(),
                            game.getPlayerIds(),
                            game.getType(),
                            game.getState()
                    );
                }).collect(Collectors.toList()),
                user.getCurrentGameId(), user.getType().toString()
        );
    }
}
