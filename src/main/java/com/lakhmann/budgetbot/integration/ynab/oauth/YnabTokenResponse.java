package com.lakhmann.budgetbot.integration.ynab.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record YnabTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken
) {}

