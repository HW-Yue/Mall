package com.yue.opsagent.springai.trigger;

import com.yue.opsagent.springai.skill.api.ToolResult;
import com.yue.opsagent.springai.skill.registry.MasterRegistry;
import com.yue.opsagent.springai.trigger.dto.ToolExecuteRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolsController {

    private final MasterRegistry masterRegistry;

    public ToolsController(MasterRegistry masterRegistry) {
        this.masterRegistry = masterRegistry;
    }

    @PostMapping("/execute")
    public Map<String, Object> execute(@Valid @RequestBody ToolExecuteRequest req) {
        ToolResult r = masterRegistry.execute(req.skill(), req.tool(), req.args() == null ? Map.of() : req.args());
        return r.toMap();
    }
}
