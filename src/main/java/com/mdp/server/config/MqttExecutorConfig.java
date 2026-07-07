package com.mdp.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * MQTT로 들어오는 센서/미디어 이벤트를 처리하기 위한 전용 스레드풀.
 *
 * - Paho의 messageArrived() 콜백 스레드는 절대 블로킹 I/O(HTTP 업로드, AI 분석 요청 등)로
 *   직접 점유하면 안 되므로, 모든 처리 로직은 이 executor로 위임한다.
 * - 공유 ForkJoinPool.commonPool() 대신 별도 풀을 쓰는 이유:
 *   commonPool은 애플리케이션 전체(Stream.parallel 등)와 공유되고, 블로킹 I/O를 태우면
 *   병렬성이 낮은 환경(코어 수 적은 서버)에서 버스트 시 쉽게 밀린다.
 * - 큐를 무한정 쌓지 않도록 bounded queue + CallerRunsPolicy를 사용한다.
 *   (풀이 포화되면 호출 스레드=MQTT 콜백 스레드가 잠깐 대신 실행하게 되어
 *    극단적 상황에서도 메시지 유실 없이 자연스러운 backpressure가 걸린다.)
 */
@Configuration
public class MqttExecutorConfig {

    @Bean(name = "mqttEventExecutor")
    public Executor mqttEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mqtt-event-");
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
