package com.ge.ev.sample;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by 212391398 on 5/5/17.
 */
@Component
public class UaaTokenRequester {

  @Value("${accessTokenEndpointUrl}")
  String tokenEndpointUrl;

  @Value("${accessTokenAuthString}")
  String accessTokenAuthString;

  public String getToken()
  {
    RestTemplate restTemplate = new RestTemplateBuilder().build();
    MultiValueMap<String, String> restParams = new LinkedMultiValueMap<>();
    restParams.add("grant_type", "client_credentials");

    HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json, application/x-www-form-urlencoded");
    headers.add("Authorization", "Basic " + accessTokenAuthString);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(restParams, headers);
    HashMap<String, Object> response = restTemplate.postForObject(tokenEndpointUrl, request, HashMap.class);

    String token = (response != null) ?  (String) response.get("access_token") : null;
    return token;
  }

}
