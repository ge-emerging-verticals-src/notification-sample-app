package com.ge.ev.sample;

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
import com.ge.ev.notification.client.requests.configuration.ConfigurationRequestBody;
import com.ge.ev.notification.client.requests.email.TemplateEmailRequestBody;
import com.ge.ev.notification.client.requests.template.CreateMatchersRequestBody;
import com.ge.ev.notification.client.requests.template.CreateRecipientsRequestBody;
import com.ge.ev.notification.client.requests.template.CreateTemplateRequestBody;
import com.ge.ev.notification.client.requests.tenant.UpdateTenantConfigurationRequestBody;
import com.ge.ev.notification.client.response.SendEmailResponse;
import com.ge.ev.notification.vcap.ServiceEnvironment;
import com.ge.ev.notification.vcap.domain.NotificationServiceEnvironmentElement;
import com.ge.ev.notification.vcap.exceptions.ServiceEnvironmentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Created by 212391398 on 5/4/17.
 */
@Component
public class NotificationTester {


  @Value("${accessTokenEndpointUrl}")
  String tokenEndpointUrl;

  @Value("${accessTokenAuthString}")
  String accessTokenAuthString;

  @Value("${notificationServiceName}")
  String notificationServiceName;


  @PostConstruct
  public void init()
  {

    String deleteUuid = "";
    String updateUuid = "";
    String templateUuid = "";
    String matcherUuid = "";


    //Create Notification Service Environment from VCAP
    NotificationServiceEnvironmentElement notificationServiceEnvironmentElement = null;
    try
    {
      ServiceEnvironment serviceEnvironment = new ServiceEnvironment();
      notificationServiceEnvironmentElement = serviceEnvironment.getNotificationServiceElementByName(notificationServiceName);
    }
    catch (ServiceEnvironmentException ex)
    {
      ex.printStackTrace();
    }

    //Create Notification Client
    NotificationServiceClient notificationServiceClient = new NotificationServiceClientBuilder()
        .setVersion("v1")
//        .setBaseUrl(notificationServiceEnvironmentElement.getCredentials().getCatalogUri())
//        .setTenantUuid(notificationServiceEnvironmentElement.getCredentials().getCatalogUri())
        .setBaseUrl("https://ev-notification-service-dev.run.aws-usw02-pr.ice.predix.io")
        .setTenantUuid("77e36836-cf4d-49a7-81fb-3a5311a454ff")
        .build();

    String token = getAccessTokenFromUaa();

    //Getting Tenant
    System.out.println("======GETTING TENANT======");
    try {
      Tenant tenant =  notificationServiceClient.getTenant(token);
//      System.out.println("Tenant: " + tenant.toJson());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }

    //Update Tenant
    System.out.println("======UPDATING TENANT======");
    UpdateTenantConfigurationRequestBody updateTenantConfigurationRequestBody = new UpdateTenantConfigurationRequestBody.UpdateTenantConfigurationRequestBodyBuilder()
        .setFailWebhook("https://notification-webhook.run.aws-usw02-pr.ice.predix.io/success")
        .setSuccessWebhook("https://notification-webhook.run.aws-usw02-pr.ice.predix.io/fail")
        .build();

    try {
      Tenant tenant =  notificationServiceClient.updateTenant(token, updateTenantConfigurationRequestBody);
//      System.out.println("Updated Tenant: " + tenant.toJson());
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }


    //get Configurations
    System.out.println("======GETTING CONFIGS======");
    try {
      List<Configuration> configurations = notificationServiceClient.getConfigurations(token, null);
      for (Configuration configuration : configurations) {
//        System.out.println("Get Configurations: " + configuration.toJson());
        if (!configuration.getUuid().equals("aa99a617-d432-4b1c-8115-f4500c84b4e1"))
        {
          deleteUuid = configuration.getUuid();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //delete Configurations
    System.out.println("======DELETING CONFIGS======");
    try {
      List<Configuration> configurations = notificationServiceClient.deleteConfiguration(token, deleteUuid);
      for (Configuration configuration : configurations) {
//        System.out.println("Delete Configurations: " + configuration.toJson());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //add configurations
    System.out.println("======ADDING CONFIGS======");
    try {
      ConfigurationRequestBody configurationRequestBody = new ConfigurationRequestBody.ConfigurationRequestBodyBuilder()
          .setHost("smtp.mail.yahoo.com")
          .setMailFrom("notification.sample@yahoo.com")
          .setMailPassword("notificationsample123")
          .setMailUsername("notification.sample@yahoo.com")
          .setPort(587)
          .setProtocol("smtp")
          .setSmtpAuth(true)
          .setSmtpStarttlsEnable(true)
          .build();

      List<Configuration> configurations = notificationServiceClient.createConfiguration(token, configurationRequestBody);
      for (Configuration configuration : configurations) {
//        System.out.println("Created Configurations: " + configuration.toJson());
        if (!configuration.getUuid().equals("aa99a617-d432-4b1c-8115-f4500c84b4e1"))
        {
          updateUuid = configuration.getUuid();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //update configurations
    System.out.println("======Updating CONFIG======");
    try {

      ConfigurationRequestBody configurationRequestBody = new ConfigurationRequestBody.ConfigurationRequestBodyBuilder()
          .setHost("UPDATED-smtp.mail.yahoo.com")
          .setMailFrom("UPDATED-notification.sample@yahoo.com")
          .setMailPassword("UPDATED-notificationsample123")
          .setMailUsername("UPDATED-notification.sample@yahoo.com")
          .setPort(587)
          .setProtocol("UPDATED-smtp")
          .setSmtpAuth(true)
          .setSmtpStarttlsEnable(true)
          .build();

      List<Configuration> configurations = notificationServiceClient.updateConfiguration(token, updateUuid, configurationRequestBody);
      for (Configuration configuration : configurations) {
//        System.out.println("Updated Configurations: " + configuration.toJson());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //Getting Events
    System.out.println("======Getting Events======");
    try {
      List<NotificationEvent> notificationEvents = notificationServiceClient.getEvents(token, "bba698ab-a5e1-431e-a307-c9a80f8b401d");
      for (NotificationEvent notificationEvent : notificationEvents) {
//       System.out.println(notificationEvent.toJson());
      }
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Getting Templates
    System.out.println("======Getting Templates======");
    try {
      List<Template> templates = notificationServiceClient.getTemplates(token, "b57a0b61-1a6c-4c1b-a919-1bba1a35e4a4");
      for (Template template : templates) {
//        System.out.println(template.toJson());
      }
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Create Template
    System.out.println("======Create Templates======");
    try {
      InputStream in = null;
      try {
        in = new FileInputStream(new File(this.getClass().getClassLoader().getResource("template.txt").getFile()));
      } catch (IOException e) {
        e.printStackTrace();
      }

      CreateTemplateRequestBody createTemplateRequestBody = new CreateTemplateRequestBody.CreateTemplateRequestBodyBuilder("ev.ge.com", "sdk_test", "Template to test SDK" )
          .setSubjectTemplate("sdk test subject")
          .build();
      List<Template> templates = notificationServiceClient.createTemplate(token, createTemplateRequestBody, in);
      for (Template template : templates) {
//        System.out.println(template.toJson());
      }
      templateUuid = templates.get(0).getTemplateUuid();
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    System.out.println("======Create Matchers======");
    try {
      CreateMatchersRequestBody createMatchersRequestBody = new CreateMatchersRequestBody.CreateMatchersRequestBodyBuilder("$.[?(@.type in ['daily','alert'])]").build();
      List<Matcher> matchers = notificationServiceClient.createMatcher(token, templateUuid, createMatchersRequestBody);
      for (Matcher matcher : matchers) {
        System.out.println(matcher.toJson());

      }
      matcherUuid = matchers.get(0).getMatchersUuid();
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    System.out.println("======Get Matchers======");
    try {
      List<Matcher> matchers = notificationServiceClient.getMatchers(token, templateUuid, null);
      for (Matcher matcher : matchers) {
        System.out.println(matcher.toJson());
      }
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    System.out.println("======Create Recipients======");
    try {
      CreateRecipientsRequestBody createRecipientsRequestBody = new CreateRecipientsRequestBody.CreateRecipientsRequestBodyBuilder().addRecipient("dat.nguyen@ge.com").build();
      List<Recipient> recipients = notificationServiceClient.createRecipients(token, templateUuid, matcherUuid, createRecipientsRequestBody);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    System.out.println("======Get Recipients======");
    try {
      List<Recipient> recipients = notificationServiceClient.getRecipients(token, templateUuid, matcherUuid, null);
      for (Recipient recipient : recipients) {
        System.out.println(recipient.toJson());
      }
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }

    //Send email
    System.out.println("======SEND EMAIL======");
    try {
//      SendEmailRequestBodyRecipient sendEmailRequestBodyRecipient = new SendEmailRequestBodyRecipient.SendEmailRequestBodyRecipientBuilder("Dat Nguyen", "dat.nguyen@ge.com", "to").build();

//      SendEmailRequestBody sendEmailRequestBody = new SendEmailRequestBody.SendEmailRequestBodyBuilder()
//          .setBody("This is a sample email from The Notification Blog HMS")
//          .setFromEmail("ev.notification.sample@gmail.com")
//          .setFromName("Notification Blog HMS")
//          .setImportant(true)
//          .addRecipients(sendEmailRequestBodyRecipient)
//          .setSubject("Notification Blog HMS Sample")
//          .build();
//

      TemplateEmailRequestBody sendEmailRequestBody = new TemplateEmailRequestBody.TemplateEmailRequestBodyBuilder()
          .addKeyValue("api1_count", 11230)
          .addKeyValue("api2_count", 23540)
          .addKeyValue("api3_count", 56215)
          .addKeyValue("type", "daily")
          .build();

      SendEmailResponse sendEmailResponse = notificationServiceClient.sendEmail(token, "aa99a617-d432-4b1c-8115-f4500c84b4e1", sendEmailRequestBody, templateUuid);
//      System.out.println(sendEmailResponse.toJson());
    } catch (RequestException e) {
      printRequestException(e);
    } catch (NotificationClientException e) {
      e.printStackTrace();
    }


    if (true) {
      //Delete Template
      System.out.println("======Delete Templates======");
      try {
        List<Template> templates = notificationServiceClient.deleteTemplate(token, templateUuid);
        for (Template template : templates) {
//          System.out.println(template.toJson());
        }
      } catch (RequestException e) {
        printRequestException(e);
      } catch (NotificationClientException e) {
        e.printStackTrace();
      }
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
    requestException.printStackTrace();
  }

  public String getAccessTokenFromUaa() {

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
