package com.yue.opsagent.springai.trigger;

import com.yue.opsagent.springai.service.OpsRoutingPolicyService;
import com.yue.opsagent.springai.trigger.dto.UpdateRoutePolicyRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ops/config")
public class OpsRoutePolicyController {

    private final OpsRoutingPolicyService opsRoutingPolicyService;

    public OpsRoutePolicyController(OpsRoutingPolicyService opsRoutingPolicyService) {
        this.opsRoutingPolicyService = opsRoutingPolicyService;
    }

    @GetMapping("/routing-policy")
    public Map<String, Object> getRoutingPolicy() {
        return opsRoutingPolicyService.snapshot().toMap();
    }

    @PutMapping("/routing-policy")
    public Map<String, Object> updateRoutingPolicy(@RequestBody UpdateRoutePolicyRequest request) {
        if (request == null || request.alertAutonomousPlanningEnabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alertAutonomousPlanningEnabled 不能为空");
        }
        return opsRoutingPolicyService.updateAlertAutonomousPlanning(request.alertAutonomousPlanningEnabled()).toMap();
    }
}
