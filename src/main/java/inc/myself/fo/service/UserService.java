package inc.myself.fo.service;

import inc.myself.fo.domain.Role;
import inc.myself.fo.domain.User;
import inc.myself.fo.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MailSenderService mailSenderService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull final String username) throws UsernameNotFoundException {
        final User user = userRepo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    @NonNull
    public boolean addUser(@NonNull final User user) {
        final User foundUser = userRepo.findByUsername(user.getUsername());
        if (foundUser != null) {
            return false;
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);

        sendActivationMessage(user);

        return true;
    }

    @NonNull
    public boolean activateUser(@NonNull final String code) {
        final User user = userRepo.findByActivationCode(code);

        if (user == null) {
            return false;
        }

        user.setActivationCode(null);
        userRepo.save(user);

        return true;
    }

    @Nullable
    public List<User> findAll() {
        return userRepo.findAll();
    }

    public void saveUser(@NonNull final Long userId, @NonNull final String username, @NonNull final Map<String, String> form) {
        final User user = userRepo.findById(userId).get();
        user.setUsername(username);

        final Set<String> roleSet = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();

        for (String key : form.keySet()) {
            if (roleSet.contains(key)) {
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepo.save(user);
    }

    @Nullable
    public User findById(@NonNull Long userId) {
        return userRepo.findById(userId).get();
    }

    public void updateProfile(@NonNull final User user, @NonNull final String email, @NonNull final String password) {
        final String userEmail = user.getEmail();

        final boolean isEmailChanged = email != null && !email.equals(userEmail) ||
                userEmail != null && !userEmail.equals(email);
        if (isEmailChanged) {
            user.setEmail(email);
            if (!StringUtils.isEmpty(email)) {
                user.setActivationCode(UUID.randomUUID().toString());
            }
        }
        if (!StringUtils.isEmpty(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepo.save(user);

        if (isEmailChanged) {
            sendActivationMessage(user);
        }
    }

    private void sendActivationMessage(@NonNull final User user) {
        if (!StringUtils.isEmpty(user.getEmail())) {
            final String message = String.format(
                    "Hello, %s!\n" +
                            "Welcome to Switter. Please, visit next link: http://localhost:8080/activate/%s",
                    user.getUsername(), user.getActivationCode());
            mailSenderService.send(user.getEmail(), "Activation code", message);
        }
    }
}
