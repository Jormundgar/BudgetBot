package com.lakhmann.budgetbot.balance;

import com.lakhmann.budgetbot.config.properties.CurrencyProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class MoneyFormatterTest {

    @Test
    void formatsPositiveAndNegativeValues() {
        CurrencyProperties currency = new CurrencyProperties("₪", 2);
        MoneyFormatter formatter = new MoneyFormatter(currency);

        assertThat(formatter.formatMilliunits(1234000L)).isEqualTo("₪1234.00");
        assertThat(formatter.formatMilliunits(-567890L)).isEqualTo("-₪567.89");
    }
}
