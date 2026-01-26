package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.CurrencyProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MoneyFormatter {

    private final CurrencyProperties currency;

    public MoneyFormatter(CurrencyProperties currency) {
        this.currency = currency;
    }

    public String formatMilliunits(long milliunits) {
        BigDecimal amount = BigDecimal.valueOf(milliunits)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP)
                .setScale(currency.fractionDigits(), RoundingMode.HALF_UP);

        String abs = amount.abs().toPlainString();
        return (milliunits < 0 ? "-" :"") + currency.symbol() + abs;
    }
}
