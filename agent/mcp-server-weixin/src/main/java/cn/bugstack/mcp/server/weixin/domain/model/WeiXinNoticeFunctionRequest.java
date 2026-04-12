package cn.bugstack.mcp.server.weixin.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeiXinNoticeFunctionRequest {

    @JsonProperty(required = true, value = "platform")
    @JsonPropertyDescription("平台")
    private String platform;

    @JsonProperty(required = true, value = "subject")
    @JsonPropertyDescription("主题")
    private String subject;

    @JsonProperty(required = true, value = "description")
    @JsonPropertyDescription("描述")
    private String description;

    @JsonProperty(required = true, value = "jumpUrl")
    @JsonPropertyDescription("跳转地址")
    private String jumpUrl;

}
