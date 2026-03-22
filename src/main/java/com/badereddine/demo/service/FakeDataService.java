package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import com.badereddine.demo.repository.RoleRepository;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class FakeDataService {
    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    public User generateFakeUser() {

        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_ADMIN)));
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_USER)));

        Faker faker = new Faker();
        User user = new User();
        user.setFirstName(faker.name().firstName());
        user.setLastName(faker.name().lastName());
        user.setBirthDate(faker.date().birthday());
        user.setCity(faker.address().city());
        user.setCountry(faker.address().country());
        user.setAvatar(faker.internet().avatar());
        user.setCompany(faker.company().name());
        user.setJobPosition(faker.company().profession());
        user.setMobile(faker.phoneNumber().cellPhone());
        user.setUsername(faker.name().username());
        user.setEmail(faker.internet().emailAddress());
        user.setPassword(encoder.encode("password"));

        ERole randomRole = faker.bool().bool() ? ERole.ROLE_ADMIN : ERole.ROLE_USER;
        user.setRole(randomRole == ERole.ROLE_ADMIN ? adminRole : userRole);

        return user;
    }
}