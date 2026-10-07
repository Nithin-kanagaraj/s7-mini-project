package com.hospital.scheduling.compliance;

import com.hospital.scheduling.common.exception.ResourceNotFoundException;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.compliance.dto.ComplianceRuleDto;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compliance/rules")
@RequiredArgsConstructor
public class ComplianceRuleController {

    private final ComplianceRuleRepository complianceRuleRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComplianceRuleDto>> getRules() {
        List<ComplianceRuleDto> rules = complianceRuleRepository.findAll().stream()
                .map(ComplianceRuleDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/{ruleKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplianceRuleDto> updateRule(
            @PathVariable String ruleKey,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {

        ComplianceRule rule = complianceRuleRepository.findByRuleKey(ruleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Compliance rule not found with key: " + ruleKey));

        if (body.containsKey("ruleValue")) {
            rule.setRuleValue(((Number) body.get("ruleValue")).intValue());
        }
        if (body.containsKey("description")) {
            rule.setDescription((String) body.get("description"));
        }

        if (principal != null) {
            User updater = userRepository.findById(principal.getId()).orElse(null);
            rule.setUpdatedBy(updater);
        }

        ComplianceRule saved = complianceRuleRepository.save(rule);
        return ResponseEntity.ok(ComplianceRuleDto.fromEntity(saved));
    }
}
