package com.ge.ev.sample;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller
{
  @Trackable
  @RequestMapping(value = RestEndpoints.ENDPOINT_A, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> funcA(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }

  @Trackable
  @RequestMapping(value = RestEndpoints.ENDPOINT_B, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> funcB(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }

  @Trackable
  @RequestMapping(value = RestEndpoints.ENDPOINT_C, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> funcC(HttpServletRequest request) throws Exception {
    return new ResponseEntity<>(request.getRequestURI(), HttpStatus.OK);
  }
}
  
  