package com.ge.ev.sample;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EndpointMonitor {

  private ConcurrentHashMap<String, Long> requests;

  private Long hits;

  @Autowired
  UaaTokenRequester uaaTokenRequester;

  @PostConstruct
  public void init()
  {
    this.requests = new ConcurrentHashMap<>();
    hits = 0L;
  }

  public void TrackRequests(String request)
  {
    Long requestCount = this.requests.get(request);
    this.requests.put(request, requestCount == null ? 1L : requestCount + 1);
    hits++;
  }
}