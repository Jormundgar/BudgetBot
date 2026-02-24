package com.lakhmann.budgetbot.integration.ynab.oauth;

import com.lakhmann.budgetbot.config.properties.YnabProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class YnabOAuthService {

    private final RestClient restClient;
    private final YnabProperties props;

    public YnabOAuthService(RestClient.Builder builder, YnabProperties props) {
        this.restClient = builder.build();
        this.props = props;
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString(props.oauthAuthorizeUrl())
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .toUriString();
    }

    public YnabTokenResponse exchangeCode(String code) {
        return tokenRequest(Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", props.redirectUri(),
                "client_id", props.clientId(),
                "client_secret", props.clientSecret()
        ));
    }

    public YnabTokenResponse exchangeRefreshToken(String refreshToken) {
        return tokenRequest(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "client_id", props.clientId(),
                "client_secret", props.clientSecret()
        ));
    }

    private YnabTokenResponse tokenRequest(Map<String, String> form) {
        var body = new LinkedMultiValueMap<String, String>();
        form.forEach(body::add);

        return restClient.post()
                .uri(props.oauthTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(YnabTokenResponse.class);
    }
}
