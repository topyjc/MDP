package com.mdp.server.polling;

import com.mdp.server.dto.SensorMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/poll")
public class PollingController {

    private final PollingService pollingService;

    public PollingController(PollingService pollingService) {
        this.pollingService = pollingService;
    }

    @GetMapping("/sensor")
    public DeferredResult<SensorMessage> pollSensorData() {
        return pollingService.waitForNextMessage();
    }
}