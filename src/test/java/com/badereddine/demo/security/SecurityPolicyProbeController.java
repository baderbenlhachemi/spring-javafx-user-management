package com.badereddine.demo.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityPolicyProbeController {

    @GetMapping({"/v3/api-docs/security-probe", "/swagger-ui/security-probe"})
    String documentationProbe() {
        return "documentation available";
    }
}
