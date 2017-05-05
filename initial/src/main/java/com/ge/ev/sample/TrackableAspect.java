package com.ge.ev.sample;

import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class TrackableAspect {
  @Autowired
  EndpointMonitor endpointMonitor;

  @After("@annotation(trackable) && args(request)")
  public void processLoggableEvent(Trackable trackable, HttpServletRequest request) throws Throwable {
    endpointMonitor.TrackRequests(request.getRequestURI());
  }
}
