package com.mdp.server.mqtt;

import com.mdp.server.dto.DataDto;
import com.mdp.server.service.DataService;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MqttService {

    // application.properties 에서 MQTT 브로커 주소 주입
    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    // application.properties 에서 MQTT 클라이언트 아이디 주입
    @Value("${mqtt.client-id}")
    private String clientId;

    // QoS 값, 없으면 기본값 1
    @Value("${mqtt.qos:1}")
    private int qos;

    // 구독할 토픽
    @Value("${mqtt.topics}")
    private String topic;

    // 인증 정보가 없을 수도 있으므로 기본값은 빈 문자열
    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    // MQTT에서 받은 데이터를 다음 단계로 넘길 서비스
    private final DataService dataService;

    // MQTT 클라이언트 인스턴스
    private MqttClient client;

    public MqttService(DataService dataService) {
        this.dataService = dataService;
    }

    /**
     * MQTT 브로커에 연결하고 토픽을 구독한다.
     */
    public void connect() {
        try {
            System.out.println("[MQTT] connect() started");
            System.out.println("[MQTT] brokerUrl = " + brokerUrl);
            System.out.println("[MQTT] clientId = " + clientId);
            System.out.println("[MQTT] qos = " + qos);
            System.out.println("[MQTT] topic = " + topic);

            // MQTT 클라이언트 생성
            client = new MqttClient(brokerUrl, clientId);

            // 연결 옵션 설정
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            // username이 비어있지 않을 때만 설정
            if (username != null && !username.isBlank()) {
                options.setUserName(username);
            }

            // password가 비어있지 않을 때만 설정
            if (password != null && !password.isBlank()) {
                options.setPassword(password.toCharArray());
            }

            // 콜백 설정
            client.setCallback(new MqttCallback() {

                /**
                 * 브로커와 연결이 끊겼을 때 호출된다.
                 */
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("[MQTT] connection lost: " + cause);
                }

                /**
                 * 구독 중인 토픽으로 메시지가 들어왔을 때 호출된다.
                 */
                @Override
                public void messageArrived(String receivedTopic, MqttMessage message) {
                    try {
                        // MQTT payload를 문자열로 변환
                        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

                        System.out.println("[MQTT] ===== MESSAGE ARRIVED =====");
                        System.out.println("[MQTT] received topic = " + receivedTopic);
                        System.out.println("[MQTT] payload = " + payload);
                        System.out.println("[MQTT] qos = " + message.getQos());
                        System.out.println("[MQTT] retained = " + message.isRetained());

                        // 토픽 + payload 를 DataDto 로 변환
                        DataDto data = mapToDataDto(receivedTopic, payload);

                        // 변환된 데이터를 다음 서비스 레이어로 전달
                        dataService.processData(data);

                    } catch (Exception e) {
                        System.out.println("[MQTT] DataDto 매핑/처리 실패");
                        e.printStackTrace();
                    }
                }

                /**
                 * 발행 완료 콜백.
                 * 현재 subscriber 중심 구조에서는 실사용 의미가 크지 않지만 디버깅용으로 둔다.
                 */
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("[MQTT] deliveryComplete called");
                }
            });

            // 브로커 연결
            client.connect(options);
            System.out.println("[MQTT] connected = " + client.isConnected());

            // 토픽 구독
            client.subscribe(topic, qos);
            System.out.println("[MQTT] subscribed to " + topic);

        } catch (Exception e) {
            System.out.println("[MQTT] connect failed -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MQTT topic + payload 를 현재 프로젝트에서 사용하는 DataDto 형태로 변환한다.
     *
     * 현재 토픽 예시:
     * mdp/pi/rpi-01/sensor/temperature
     *
     * parts[0] = mdp
     * parts[1] = pi
     * parts[2] = rpi-01
     * parts[3] = sensor
     * parts[4] = temperature
     *
     * 현재 DataDto 구조에는 deviceId, type 필드가 없으므로
     * 최소 동작 기준으로 아래처럼 매핑한다.
     *
     * project   -> mdp
     * component -> temperature
     * value     -> payload 값
     * timestamp -> DataService 에서 비어 있으면 현재 시간으로 자동 세팅
     */
    private DataDto mapToDataDto(String receivedTopic, String payload) {
        String[] parts = receivedTopic.split("/");

        // topic 형식이 예상보다 짧으면 예외 발생
        if (parts.length < 5) {
            throw new IllegalArgumentException("토픽 형식이 예상과 다름: " + receivedTopic);
        }

        DataDto data = new DataDto();

        // 현재 프로젝트 기준 최소 매핑
        data.setProject(parts[0]);      // mdp
        data.setComponent(parts[4]);    // temperature
        data.setValue(parsePayloadValue(payload));

        return data;
    }

    /**
     * payload 문자열을 가능한 한 적절한 타입으로 변환한다.
     *
     * 예:
     * "25.4" -> Double
     * "25"   -> Long
     * "ON"   -> String
     */
    private Object parsePayloadValue(String payload) {
        String trimmed = payload.trim();

        try {
            // 소수점이 있으면 Double 로 처리
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            }

            // 정수면 Long 으로 처리
            return Long.parseLong(trimmed);

        } catch (NumberFormatException e) {
            // 숫자로 변환 안 되면 문자열 그대로 사용
            return trimmed;
        }
    }
}