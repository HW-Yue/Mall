package cn.bugstack.mcp.server.weixin.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeiXinNoticeFunctionResponse {

    @JsonProperty(required = true, value = "success")
    @JsonPropertyDescription("success")
    private boolean success;

}
