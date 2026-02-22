package com.lakhmann.budgetbot.user;

import java.util.List;
import java.util.Optional;

public interface UserYnabConnectionStore {
    void save(UserYnabConnection connection);
    Optional<UserYnabConnection> get(long userId);
    List<Long> listConnectedUserIds();
}
