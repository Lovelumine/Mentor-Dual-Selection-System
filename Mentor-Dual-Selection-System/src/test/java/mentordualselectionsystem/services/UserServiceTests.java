package mentordualselectionsystem.services;

import mentordualselectionsystem.mysql.User;
import mentordualselectionsystem.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void saveUserShouldEncodeRawPassword() {
        User user = new User();
        user.setPassword("123");

        userService.saveUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        String savedPassword = captor.getValue().getPassword();
        assertThat(savedPassword).isNotEqualTo("123");
        assertThat(new BCryptPasswordEncoder().matches("123", savedPassword)).isTrue();
    }

    @Test
    void saveUserShouldNotReencodeExistingBcryptPassword() {
        String encodedPassword = new BCryptPasswordEncoder().encode("123");
        User user = new User();
        user.setPassword(encodedPassword);

        userService.saveUser(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword()).isEqualTo(encodedPassword);
    }
}
