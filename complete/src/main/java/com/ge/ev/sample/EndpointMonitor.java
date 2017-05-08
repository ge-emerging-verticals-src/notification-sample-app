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
import com.ge.ev.notification.client.requests.template.CreateMatchersRequestBody;
import com.ge.ev.notification.client.requests.template.CreateRecipientsRequestBody;
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
      ServiceEnvironment serviceEnvironment = new ServiceEnvironment();
      notificationServiceEnvironmentElement = serviceEnvironment.getNotificationServiceElementByName(notificationServiceName);
    }
    catch (ServiceEnvironmentException e)
    {
      e.printStackTrace();
    }

    //Create Notification Service Client
    notificationServiceClient = new NotificationServiceClientBuilder(notificationServiceEnvironmentElement)
        .setVersion("v1")
        .setBaseUrl("https://ev-notification-service-dev.run.aws-usw02-pr.ice.predix.io")
        .setTenantUuid("77e36836-cf4d-49a7-81fb-3a5311a454ff")
        .build();

    String token = uaaTokenRequester.getToken();

    //Create Configuration
    Configuration configuration = new Configuration.ConfigurationBuilder()
        .setProtocol("smtp")
        .setHost("smtp.sparkpostmail.com")
        .setPort(587)
        .setSmtpAuth(true)
        .setSmtpStarttlsEnable(true)
        .setMailFrom("ev.notification.sample@sparkpost.com")
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
      e.printStackTrace();
    } catch (RequestException e) {
      e.printStackTrace();
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
    catch (RequestException e)
    {
      e.printStackTrace();
    }
    catch (NotificationClientException e)
    {
      e.printStackTrace();
    }

    //Create a matcher for HIGH alerts
    try {
      CreateMatchersRequestBody createMatchersRequestBody = new CreateMatchersRequestBody.CreateMatchersRequestBodyBuilder("$.[?(@.alert in ['HIGH'])]").build();
      this.highAlertMatcher = notificationServiceClient.createMatcher(token, this.template, createMatchersRequestBody);
    }
    catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient("it.director@ev.notification.ge.com").build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.highAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Create a matcher for LOW alerts
    try {
      CreateMatchersRequestBody createMatchersRequestBody = new CreateMatchersRequestBody.CreateMatchersRequestBodyBuilder("$.[?(@.alert in ['LOW'])]").build();
      this.lowAlertMatcher = notificationServiceClient.createMatcher(token, this.template, createMatchersRequestBody);
    }
    catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Create recipient list for high alert matcher
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient("it@ev.notification.ge.com").build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, this.template, this.lowAlertMatcher, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      e.printStackTrace();
    } catch (NotificationClientException e) {
      e.printStackTrace();
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
    SendEmailRequestBodyRecipient sendEmailRequestBodyRecipient = new SendEmailRequestBodyRecipient.SendEmailRequestBodyRecipientBuilder("Dat Nguyen", "dat.nguyen@ge.com", RecipientType.to).build();

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
    }
    catch (RequestException e)
    {
      e.printStackTrace();
    }
    catch (NotificationClientException e) {
      e.printStackTrace();
    }
  }

  private void alertOnThreshold()
  {
    AlertLevel alertLevel = AlertLevel.NONE;

    if (this.hits >= 20)
    {
      alertLevel = AlertLevel.HIGH;
    }
    else if (this.hits >= 10)
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
    Runtime runtime = Runtime.getRuntime();
    Double heap_usage = ((runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory()) * 100.0;

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
    } catch (NotificationClientException e) {
      e.printStackTrace();
    } catch (RequestException e) {
      e.printStackTrace();
    }
  }

  private void configureTenant(String successWebhook, String failWebhook)
  {
    String token = uaaTokenRequester.getToken();
    UpdateTenantConfigurationRequestBody updateTenantConfigurationRequestBody = new UpdateTenantConfigurationRequestBody.UpdateTenantConfigurationRequestBodyBuilder()
        .setFailWebhook(successWebhook)
        .setSuccessWebhook(failWebhook)
        .build();

    try {
      Tenant tenant =  notificationServiceClient.updateTenant(token, updateTenantConfigurationRequestBody);
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}