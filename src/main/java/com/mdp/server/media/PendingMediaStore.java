package com.mdp.server.media;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PendingMediaStore {

    private final Map<MediaMatchKey, PendingMedia> pendingMediaMap = new ConcurrentHashMap<>();

    public void put(PendingMedia media) {
        MediaMatchKey key = new MediaMatchKey(
                media.getContent(),
                media.getTableNum(),
                media.getUserId(),
                media.getLightNum()
        );

        pendingMediaMap.put(key, media);
        System.out.println("[MEDIA] put key = " + key);
    }

    public PendingMedia consume(String content, String tableNum, String userId, String lightNum) {
        MediaMatchKey key = new MediaMatchKey(content, tableNum, userId, lightNum);
        PendingMedia media = pendingMediaMap.remove(key);

        if (media != null) {
            System.out.println("[MEDIA] matched key = " + key);
        } else {
            System.out.println("[MEDIA] no match key = " + key);
        }

        return media;
    }
}