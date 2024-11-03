package rw.business.carspareparts.service;

import rw.business.carspareparts.dto.UserDto;
import rw.business.carspareparts.entity.User;

import java.util.List;

public interface UserService {
    void saveUser(UserDto userDto);
    User findUserByEmail(String email);
    List<User> findAllUsers();
    UserDto getUserById(Long id);
    void updateUser(UserDto userDto);
    void deleteUser(Long id);
}