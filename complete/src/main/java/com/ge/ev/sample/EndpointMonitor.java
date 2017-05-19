package com.ge.ev.sample;

import static com.ge.ev.sample.RestEndpoints.ENDPOINT_A;
import static com.ge.ev.sample.RestEndpoints.ENDPOINT_B;
import static com.ge.ev.sample.RestEndpoints.ENDPOINT_C;

import com.ge.ev.notification.client.NotificationServiceClient;
import com.ge.ev.notification.client.NotificationServiceClient.NotificationServiceClientBuilder;
import com.ge.ev.notification.client.domain.Configuration;
import com.ge.ev.notification.client.domain.Matcher;
import com.ge.ev.notification.client.domain.NotificationEvent;
import com.ge.ev.notification.client.domain.Recipient;
import com.ge.ev.notification.client.domain.Template;
import com.ge.ev.notification.client.domain.Tenant;
import com.ge.ev.notification.client.exceptions.NotificationClientException;
import com.ge.ev.notification.client.exceptions.RequestException;
import com.ge.ev.notification.client.requests.configuration.CreateConfigurationRequestBody;
import com.ge.ev.notification.client.requests.email.RecipientType;
import com.ge.ev.notification.client.requests.email.SendEmailRequestBody;
import com.ge.ev.notification.client.requests.email.SendEmailRequestBodyRecipient;
import com.ge.ev.notification.client.requests.email.SendTemplateEmailRequestBody;
import com.ge.ev.notification.client.requests.template.CreateRecipientsRequestBody;
import com.ge.ev.notification.client.requests.template.MatchersRequestBody;
import com.ge.ev.notification.client.requests.template.TemplateRequestBody;
import com.ge.ev.notification.client.requests.tenant.UpdateTenantConfigurationRequestBody;
import com.ge.ev.notification.client.response.SendEmailResponse;
import com.ge.ev.notification.vcap.ServiceEnvironment;
import com.ge.ev.notification.vcap.domain.NotificationServiceEnvironmentElement;
import com.ge.ev.notification.vcap.exceptions.ServiceEnvironmentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EndpointMonitor
{
  private static final String TEST_IT_EMAIL = "it@notification.ge.com";
  private static final String TEST_IT_DIRECTOR_EMAIL = "it-director@notification.ge.com";

  private enum AlertLevel
  {
    HIGH,
    LOW,
    NONE
  }

  private ConcurrentHashMap<String, Long> requests;

  private Long hits;

  @Autowired
  UaaTokenRequester uaaTokenRequester;

  @Value("${notificationServiceName}")
  String notificationServiceName;
  
  private NotificationServiceClient notificationServiceClient;

  private Configuration configuration;

  private Template template;

  private Matcher highAlertMatcher;

  private Matcher lowAlertMatcher;

  @PostConstruct
  public void init()
  {
    this.requests = new ConcurrentHashMap<>();
    hits = 0L;

    //Create Notification Service Environment from VCAP
    NotificationServiceEnvironmentElement notificationServiceEnvironmentElement = null;
    try
    {
      ServiceEnvironment serviceEnvironment = new ServiceEnvironment.ServiceEnvironmentBuilder().build();
      notificationServiceEnvironmentElement = serviceEnvironment.getNotificationServiceElementByName(notificationServiceName);
    }
    catch (ServiceEnvironmentException e)
    {
      e.printStackTrace();
    }

    //Create Notification Service Client
    notificationServiceClient = new NotificationServiceClientBuilder(notificationServiceEnvironmentElement)
        .build();

    String token = uaaTokenRequester.getToken();

    //Create Configuration
    Configuration configuration = new Configuration.ConfigurationBuilder()
        .setProtocol("smtp")
        .setHost("smtp.sparkpostmail.com")
        .setPort(587)
        .setSmtpAuth(true)
        .setSmtpStarttlsEnable(true)
        .setMailFrom("ev.notification.sample@sparkpostbox.com")
        .setMailUsername("SMTP_Injection")
        .setMailPassword("6fb83e32b23ae24d230742a15f09180c92219747")
        .build();

    //Create Configuration Request Body
    CreateConfigurationRequestBody configurationRequestBody = new CreateConfigurationRequestBody.CreateConfigurationRequestBodyBuilder()
        .addConfigurations(configuration)
        .build();

    //Send Create Configuration Request
    try {
      this.configuration = notificationServiceClient.createConfiguration(token, configurationRequestBody);
    } catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }

    //Load template file
    InputStream in = null;
    try {
      in = new FileInputStream(new File(this.getClass().getClassLoader().getResource("it_template.txt").getFile()));
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    //Send Create Template Request
    try
    {
      TemplateRequestBody templateRequestBody = new TemplateRequestBody.TemplateRequestBodyBuilder("ev.notification.ge.com", "usage_alert", "Usage alert template" )
          .setSubjectTemplate("Usage Report")
          .build();
      this.template = notificationServiceClient.createTemplate(token, templateRequestBody, in);
    }
    catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }

    //Create a matcher for HIGH alerts
    try {
      MatchersRequestBody matchersRequestBody = new MatchersRequestBody.MatchersRequestBodyBuilder("$.[?(@.alert in ['HIGH'])]").build();
      this.highAlertMatcher = notificationServiceClient.createMatcher(token, this.template, matchersRequestBody);
    }
    catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }

    //Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient(TEST_IT_DIRECTOR_EMAIL).build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.highAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    }catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }

    //Create a matcher for LOW alerts
    try {
      MatchersRequestBody matchersRequestBody = new MatchersRequestBody.MatchersRequestBodyBuilder("$.[?(@.alert in ['LOW'])]").build();
      this.lowAlertMatcher = notificationServiceClient.createMatcher(token, this.template, matchersRequestBody);
    }
    catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }

    //Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient(TEST_IT_EMAIL).build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.lowAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }
  }

  public void TrackRequests(String request)
  {
    Long requestCount = this.requests.get(request);
    this.requests.put(request, requestCount == null ? 1L : requestCount + 1);
    this.hits++;
    alertOnThreshold();
  }

  @Scheduled(fixedDelay = 1000*60*60L)
  public void SendHourlyReport()
  {
    StringBuilder emailBody = new StringBuilder("Here is your daily usage snapshot.\n");
    this.requests.keySet().stream().forEach(s -> emailBody.append(s + "=" + this.requests.get(s) + "\n"));

    //Create recipients
    SendEmailRequestBodyRecipient sendEmailRequestBodyRecipient = new SendEmailRequestBodyRecipient.SendEmailRequestBodyRecipientBuilder("it", TEST_IT_EMAIL, RecipientType.to).build();

    //Create email body
    SendEmailRequestBody sendEmailRequestBody = new SendEmailRequestBody.SendEmailRequestBodyBuilder()
        .setBody(emailBody.toString())
        .setFromEmail("ev.notification.sample@sparkpostbox.com")
        .setFromName("Endpoint Monitor")
        .addRecipients(sendEmailRequestBodyRecipient)
        .setSubject("Snapshot - " + new Date())
        .build();

    //Send email
    try
    {
      String token = uaaTokenRequester.getToken();
      SendEmailResponse sendEmailResponse = notificationServiceClient.sendEmail(token, this.configuration, sendEmailRequestBody);
      printEmailEvents(sendEmailResponse.getNotificationReferenceUuid());
    }
    catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }
  }

  private void alertOnThreshold()
  {
    AlertLevel alertLevel = AlertLevel.NONE;

    if (this.hits != 0L && this.hits % 20 == 0 )
    {
      alertLevel = AlertLevel.HIGH;
    }
    else if (this.hits != 0L && this.hits % 10 == 0)
    {
      alertLevel = AlertLevel.LOW;
    }

    if (alertLevel != AlertLevel.NONE)
    {
      SendAlert(alertLevel);
    }
  }

  private void SendAlert(AlertLevel alertLevel)
  {
    //Get heap usage
    Runtime runtime = Runtime.getRuntime();
    Double heap_usage = ( (double) (runtime.totalMemory() - runtime.freeMemory()) /  runtime.totalMemory() * 1.0) * 100.0;

    //Set template email parameters
    SendTemplateEmailRequestBody sendTemplateEmailRequestBody = new SendTemplateEmailRequestBody.SendTemplateEmailRequestBodyBuilder()
        .addKeyValue("heap_usage", heap_usage)
        .addKeyValue("endpoint_hits", hits)
        .addKeyValue("a_count", this.requests.get(ENDPOINT_A))
        .addKeyValue("b_count", this.requests.get(ENDPOINT_B))
        .addKeyValue("c_count", this.requests.get(ENDPOINT_C))
        .addKeyValue("alert", alertLevel)
        .build();

    //Send template email request
    try {
      String token = uaaTokenRequester.getToken();
      SendEmailResponse sendEmailResponse = notificationServiceClient.sendTemplateEmail(token, this.configuration,  this.template, sendTemplateEmailRequestBody);
      System.out.println(sendEmailResponse.toJson());
    }
    catch (RequestException e)
    {
      e.printStackTrace();
    }
    catch (NotificationClientException e)
    {
      e.printStackTrace();
    }
  }

  private void printEmailEvents(String notificationReferenceUuid)
  {
    String token = uaaTokenRequester.getToken();
    try {
      List<NotificationEvent> events = notificationServiceClient.getEvents(token, notificationReferenceUuid);
      events.stream().forEach(e -> System.out.println(e.toJson()) );
    } catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
     printRequestException(e);
    }
  }

  private void configureTenant(String successWebhook, String failWebhook)
  {
    String token = uaaTokenRequester.getToken();
    UpdateTenantConfigurationRequestBody updateTenantConfigurationRequestBody = new UpdateTenantConfigurationRequestBody.UpdateTenantConfigurationRequestBodyBuilder()
        .setFailWebhook(failWebhook)
        .setSuccessWebhook(successWebhook)
        .build();

    try {
      Tenant tenant =  notificationServiceClient.updateTenant(token, updateTenantConfigurationRequestBody);
    }
    catch (NotificationClientException e) {
      printNotificationException(e);
    } catch (RequestException e) {
      printRequestException(e);
    }
  }

  private void printRequestException(RequestException requestException)
  {
    System.out.println("===REQUEST EXCEPTION===");
    System.out.println(requestException.getMessage());
    System.out.println(requestException.getUrl());
    System.out.println(requestException.getStatus());
    System.out.println(requestException.getStatusMessage());
    System.out.println(requestException.getDetails());
    System.out.println("=======================");
  }

  private void printNotificationException(NotificationClientException notificationClientException)
  {
    System.out.println("===NOTIFICATION EXCEPTION===");
    System.out.println(notificationClientException.getMessage());
    System.out.println(notificationClientException.getDetails());
    System.out.println("=======================");
  }
}