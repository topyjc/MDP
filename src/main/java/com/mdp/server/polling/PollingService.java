package com.mdp.server.polling;

import com.mdp.server.dto.SensorMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class PollingService {

    private final List<DeferredResult<SensorMessage>> waitingRequests = new CopyOnWriteArrayList<>();

    public DeferredResult<SensorMessage> waitForNextMessage() {
        DeferredResult<SensorMessage> deferredResult = new DeferredResult<>(30000L);

        deferredResult.onCompletion(() -> waitingRequests.remove(deferredResult));

        deferredResult.onTimeout(() -> {
            waitingRequests.remove(deferredResult);
            deferredResult.setResult(null);
        });

        waitingRequests.add(deferredResult);
        return deferredResult;
    }

    public void publish(SensorMessage message) {
        for (DeferredResult<SensorMessage> request : waitingRequests) {
            request.setResult(message);
        }
        waitingRequests.clear();
    }
}