package com.lakhmann.budgetbot.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("dev")
public class InMemoryUserYnabConnectionStore implements UserYnabConnectionStore {

    private final Map<Long, UserYnabConnection> data = new ConcurrentHashMap<>();

    @Override
    public void save(UserYnabConnection connection) {
        data.put(connection.userId(), connection);
    }

    @Override
    public Optional<UserYnabConnection> get(long userId) {
        return Optional.ofNullable(data.get(userId));
    }

    @Override
    public List<Long> listConnectedUserIds() {
        return data.keySet().stream().toList();
    }
}
