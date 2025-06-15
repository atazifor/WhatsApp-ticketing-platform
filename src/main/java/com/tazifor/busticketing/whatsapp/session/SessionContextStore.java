package com.tazifor.busticketing.whatsapp.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionContextStore {
    private final Map<String, String> sessionMap = new ConcurrentHashMap<>();

    public void saveUser(String flowToken, String phone) {
        sessionMap.put(flowToken, phone);
    }

    public Optional<String> getUserPhone(String flowToken) {
        return Optional.ofNullable(sessionMap.get(flowToken));
    }

    public void removeUser(String flowToken) {
        sessionMap.remove(flowToken);
    }
}
