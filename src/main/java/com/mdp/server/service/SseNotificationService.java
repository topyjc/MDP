package com.mdp.server.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    // 💡 [HTTP 웹 전용 전화번호부] 유저 ID -> HTTP 스트리밍 통로(SseEmitter)
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 웹 브라우저가 접속하여 알림 파이프라인을 개통할 때 호출
     */
    public SseEmitter subscribe(String userId) {
        // 1. 타임아웃 시간 설정 (예: 30분 = 30 * 60 * 1000ms)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 2. 전화번호부에 유저 ID와 연결선 저장
        emitters.put(userId, emitter);
        System.out.println("[Web SSE] 유저 알림 통로 개통 완료: " + userId);

        // 3. 연결 종료/타임아웃/에러 시 맵에서 자동 삭제
        emitter.onCompletion(() -> {
            System.out.println("[Web SSE] 연결 종료됨: " + userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            System.out.println("[Web SSE] 타임아웃 발생: " + userId);
            emitters.remove(userId);
        });
        emitter.onError((e) -> {
            System.out.println("[Web SSE] 에러 발생: " + userId);
            emitters.remove(userId);
        });

        // 4. 최초 연결 시 503 (Service Unavailable) 에러 방지를 위한 핑(Ping) 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("웹 알림 연결이 성공적으로 완료되었습니다. (userId: " + userId + ")"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * 특정 유저(1:1)에게 웹 알림 푸시 전송
     */
    public void sendToSpecificUser(String userId, Object data) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) {
            try {
                // 💡 해당 유저의 통로로 1:1 데이터를 쏩니다.
                emitter.send(SseEmitter.event()
                        .name("WARD_EMERGENCY") // 웹 프론트에서 들을 이벤트 이름
                        .data(data));           // DTO 또는 Map 객체 (Jackson이 자동 JSON 변환)

                System.out.println("[Web SSE 1:1 푸시 성공] 수신자: " + userId);
            } catch (IOException e) {
                System.err.println("[Web SSE 푸시 실패] 연결이 끊어진 유저입니다: " + userId);
                emitters.remove(userId);
            }
        } else {
            System.out.println("[Web SSE] 수신자(" + userId + ")가 현재 웹에 접속해있지 않습니다.");
        }
    }
}