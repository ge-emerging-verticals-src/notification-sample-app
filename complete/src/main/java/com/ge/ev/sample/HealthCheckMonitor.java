package com.ge.ev.sample;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HealthCheckMonitor {
  ConcurrentHashMap<String, Long> requests;

  @PostConstruct
  public void init()
  {
    this.requests = new ConcurrentHashMap<>();
  }

  public void TrackRequests(String request)
  {
    Long requestCount = this.requests.get(request);
    this.requests.put(request, requestCount == null ? 1L : requestCount + 1);

    //See request has crossed our thresold
    checkRequestThresholdsAndSendAlerts(request);
  }

  private void checkRequestThresholdsAndSendAlerts(String request)
  {
      Long count = this.requests.get(request);
  }

  @Scheduled(fixedDelay = 1000*60*60L)
  public void SendHourlyReport()
  {

  }
}