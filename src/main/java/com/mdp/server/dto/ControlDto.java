package com.mdp.server.dto;

import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ControlDto {

    private String target; // 제어할 타겟 (예: "house", "road")
    private Map<String, Object> command; // 실제 제어 명령 (예: {"led1": 1, "led1Brightness": 100})

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, Object> getCommand() {
        return command;
    }

    public void setCommand(Map<String, Object> command) {
        this.command = command;
    }

    // Map으로 받은 제어 명령을 MQTT로 쏘기 위해 JSON 문자열로 바꿔주는 마법의 메서드!
    public String getCommandAsJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this.command);
        } catch (Exception e) {
            return "{}";
        }
    }
}