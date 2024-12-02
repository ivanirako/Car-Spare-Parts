package rw.business.carspareparts.controller;

import jakarta.validation.Valid;
import rw.business.carspareparts.dto.ResetPasswordDto;
import rw.business.carspareparts.dto.UserDto;
import rw.business.carspareparts.entity.User;
import rw.business.carspareparts.repository.UserRepository;
import rw.business.carspareparts.service.FileStorageService;
import rw.business.carspareparts.service.UserService;
import rw.business.carspareparts.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2.0")
public class RestApiController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@Valid @RequestBody UserDto userDto) {
        User existingUser = userService.findUserByEmail(userDto.getEmail());
        Map<String, Object> response = new HashMap<>();

        if (existingUser != null) {
            response.put("message", "User with this email already exists");
            return ResponseEntity.badRequest().body(response);
        }
        userService.saveUser(userDto);
        response.put("message", "User updated successfully");
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestBody UserDto userDto) {
        User user = userService.findUserByEmail(userDto.getEmail());
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("message", "User not found");
            return ResponseEntity.badRequest().body(response);
        }
        if (!passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {
            response.put("message", "Invalid password");
            return ResponseEntity.badRequest().body(response);
        }
        response.put("message", "Login successful");
        response.put("success", true);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sort", defaultValue = "id") String sort,
            @RequestParam(value = "dir", defaultValue = "asc") String dir) {

        if (page < 0) page = 0;

        // Create pageable object
        Pageable pageable = PageRequest.of(page, size, dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());

        // Declare variable for the result
        Page<User> userPage;

        // Search or find all users based on the presence of the search parameter
        if (search != null && !search.isEmpty()) {
            userPage = userServiceImpl.searchUsers(search, pageable);
        } else {
            userPage = userServiceImpl.findAllUsers(pageable);
        }

        // Prepare response data
        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());
        response.put("currentPage", page);
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalItems", userPage.getTotalElements());
        response.put("sortField", sort);
        response.put("sortDir", dir);
        response.put("reverseSortDir", dir.equals("asc") ? "desc" : "asc");
        response.put("search", search);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/users/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        try {
            UserDto user = userService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto userDto) {
        try {
            UserDto user = userService.getUserById(id);
            userService.updateUser(userDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("User not found");
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody UserDto userDto) {
        User user = userService.findUserByEmail(userDto.getEmail());
        if (user == null) {
            return ResponseEntity.badRequest().body("User with this email does not exist");
        }
        // Send email logic here
        return ResponseEntity.ok("Check your email to reset password");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String email, @RequestBody ResetPasswordDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }
        User user = userService.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("User with this email does not exist");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        userService.saveUser(UserDto.fromUser(user));
        return ResponseEntity.ok("Password has been reset successfully");
    }

    //

    @PostMapping("/uploadProfilePicture")
    public ResponseEntity<Object> handleFileUpload(@RequestParam("profilePicture") MultipartFile file, @RequestParam("userId") Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String savedFile = fileStorageService.saveFile(file);
            userRepository.findById(userId).ifPresent(user -> {
                user.setProfilePicture(savedFile);
                userRepository.save(user);
            });
            response.put("message", "Profile picture updated successfully!");
            response.put("profilePicture", savedFile);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.put("message", "Failed to upload profile picture!");
            response.put("success", false);
            return ResponseEntity.status(500).body(response);
        }
    }
}
