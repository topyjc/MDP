package com.mdp.server.dto;

public class DeviceControlDto {
    /**
     * 제어할 대상 기기
     * 예: "LED1", "LED2", "GAS", "FAN", "INTRUSION", "OTP", "QR"
     */
    private String target;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * 수행할 동작
     * 예: "ON", "OFF", "SET_BRIGHTNESS", "SET_OTP", "REGISTER"
     */
    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 구체적인 값 (On/Off인 경우 비어있어도 무방, 밝기나 OTP, QR코드일 경우 필수)
     * 예: "80" (밝기), "849201" (OTP 번호), "QR-DEVICE-001"
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}