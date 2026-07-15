package com.badereddine.demo.service;

import com.badereddine.demo.exception.LastActiveAdminException;
import com.badereddine.demo.exception.UserNotFoundException;
import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final Clock clock;

    public UserService(UserRepository userRepository, RoleService roleService, Clock clock) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.clock = clock;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User deleteUser(Long id) {
        List<User> activeAdmins = lockActiveAdmins();
        User user = requireUser(id);
        rejectLastActiveAdminRemoval(activeAdmins, user, true);
        userRepository.delete(user);
        return user;
    }

    @Transactional
    public User changeRole(Long id, ERole newRole) {
        List<User> activeAdmins = lockActiveAdmins();
        User user = requireUser(id);
        rejectLastActiveAdminRemoval(activeAdmins, user, newRole != ERole.ROLE_ADMIN);

        Role role = roleService.findByName(newRole)
                .orElseGet(() -> roleService.save(new Role(newRole)));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public User setEnabled(Long id, boolean enabled) {
        List<User> activeAdmins = lockActiveAdmins();
        User user = requireUser(id);
        rejectLastActiveAdminRemoval(activeAdmins, user, !enabled);
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable);
    }

    public long count() {
        return userRepository.count();
    }

    public long countByRole(ERole roleName) {
        return userRepository.countByRoleName(roleName);
    }

    public long countNewUsersToday() {
        Date startOfToday = Date.from(
                LocalDate.now(clock)
                        .atStartOfDay(clock.getZone())
                        .toInstant()
        );
        return userRepository.countNewUsersSince(startOfToday);
    }

    private List<User> lockActiveAdmins() {
        return userRepository.findActiveByRoleNameForUpdate(ERole.ROLE_ADMIN);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    private void rejectLastActiveAdminRemoval(List<User> activeAdmins, User target, boolean removesActiveAccess) {
        boolean targetIsActiveAdmin = activeAdmins.stream()
                .anyMatch(admin -> admin.getId().equals(target.getId()));
        if (removesActiveAccess && targetIsActiveAdmin && activeAdmins.size() == 1) {
            throw new LastActiveAdminException();
        }
    }
}
