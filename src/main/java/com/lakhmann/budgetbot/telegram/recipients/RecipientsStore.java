package com.lakhmann.budgetbot.telegram.recipients;

import java.util.List;

public interface RecipientsStore {
    void add(long chatId);
    List<Long> listAll();
}