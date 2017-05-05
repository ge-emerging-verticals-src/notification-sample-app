package com.ge.ev.sample;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController
{
  @Trackable
  @RequestMapping(value = RestEndpoints.API1, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> api1(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }

  @Trackable
  @RequestMapping(value = RestEndpoints.API2, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> api2(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }

  @Trackable
  @RequestMapping(value = RestEndpoints.API3, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> api3(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }
}
  
  