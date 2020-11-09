package com.salaboy.conferences.email.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.cloudevents.helper.CloudEventsHelper;
import com.salaboy.conferences.email.rest.model.Proposal;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.zeebe.cloudevents.ZeebeCloudEventsHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@SpringBootApplication
@RestController
@Slf4j
public class EmailService {

    public static void main(String[] args) {
        SpringApplication.run(EmailService.class, args);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${version:0.0.0}")
    private String version;

    @Value("${EXTERNAL_URL:http://fmtok8s-api-gateway.default.34.91.93.206.xip.io}")
    private String externalURL;

    @Value("${EVENTS_ENABLED:false}")
    private Boolean eventsEnabled;

    @Value("${K_SINK:http://broker-ingress.knative-eventing.svc.cluster.local/default/default}")
    private String K_SINK;

    @GetMapping("/info")
    public String infoWithVersion() {
        return "{ \"name\" : \"Email Service\", \"version\" : \"v" + version + "++\", \"source\": \"https://github.com/salaboy/fmtok8s-email/releases/tag/v" + version + "\" }";
    }

    @PostMapping("/")
    public void sendEmail(@RequestBody Map<String, String> email) {
        String toEmail = email.get("toEmail");
        String emailTitle = email.get("title");
        String emailContent = email.get("content");
        log.info("+-------------------------------------------------------------------+");
        printEmail(toEmail, emailTitle, emailContent);
        log.info("+-------------------------------------------------------------------+\n\n");
    }


    @PostMapping("/notification")
    public void sendEmailNotification(@RequestBody Proposal proposal) {
        sendEmailNotificationWithLink(proposal, false);
        emitEmailWithForProposalEvent(proposal);
    }

    private void sendEmailNotificationWithLink(Proposal proposal, boolean withLink) {
        String emailBody = "Dear " + proposal.getAuthor() + ", \n";
        String emailTitle = "Conference Committee Communication";
        emailBody += "\t\t We are";
        if (proposal.isApproved()) {
            emailBody += " happy ";
        } else {
            emailBody += " sorry ";
        }
        emailBody += "to inform you that: \n";
        emailBody += "\t\t\t `" + proposal.getTitle() + "` -> `" + proposal.getDescription() + "`, \n";
        emailBody += "\t\t was";
        if (proposal.isApproved()) {
            emailBody += " approved ";
        } else {
            emailBody += " rejected ";
        }
        emailBody += "for this conference.";
        printProposalEmail(proposal, emailTitle, emailBody, withLink);
    }

    private void sendEmailToCommittee(Proposal proposal) {
        String emailTitle = "Conference Committee Please Review Proposal";
        String emailBody = "Dear Committee Member, \n" +
                "\t\t please review and accept or reject the following proposal \n";
        emailBody += "\t From Author: " + proposal.getAuthor() + "\n";
        emailBody += "\t With Id: " + proposal.getId() + "\n";
        emailBody += "\t Notification Sent at: " + new Date() + "\n";
        log.info("+-------------------------------------------------------------------+");
        printEmail("committee@conference.org", emailTitle, emailBody);
        log.info("+-------------------------------------------------------------------+");
    }

    private void printEmail(String toEmail, String title, String body) {
        log.info("\t Email Sent to: " + toEmail);
        log.info("\t Email Title: " + title);
        log.info("\t Email Body: " + body);
    }

    private void printProposalEmail(Proposal proposal, String title, String body, boolean withLink) {
        log.info("+-------------------------------------------------------------------+");
        printEmail(proposal.getEmail(), title, body);
        if (withLink) {
            log.info("\t Please CURL the following link to confirm \n" +
                    "\t\t that you are committing to speak in our conference: \n" +
                    "\t\t curl -X POST " + externalURL + "/speakers/" + proposal.getId());
        }
        log.info("+-------------------------------------------------------------------+\n\n");
    }

    public void emitEmailWithForProposalEvent(Proposal proposal){
        if(eventsEnabled) {
            String proposalString = null;
            try {
                proposalString = objectMapper.writeValueAsString(proposal);
                proposalString = objectMapper.writeValueAsString(proposalString); //needs double quoted ??
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withTime(OffsetDateTime.now().toZonedDateTime()) // bug-> https://github.com/cloudevents/sdk-java/issues/200
                    .withType("Email.Sent")
                    .withSource(URI.create("email-service.default.svc.cluster.local"))
                    .withData(proposalString.getBytes())
                    .withDataContentType("application/json")
                    .withSubject(proposal.getTitle());

            CloudEvent zeebeCloudEvent = ZeebeCloudEventsHelper
                    .buildZeebeCloudEvent(cloudEventBuilder)
                    .withCorrelationKey(proposal.getId()).build();

            logCloudEvent(zeebeCloudEvent);
            WebClient webClient = WebClient.builder().baseUrl(K_SINK).filter(logRequest()).build();

            WebClient.ResponseSpec postCloudEvent = CloudEventsHelper.createPostCloudEvent(webClient, zeebeCloudEvent);

            postCloudEvent.bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Cloud Event Posted to K_SINK -> " + K_SINK + ": Result: " +  s))
                    .subscribe();
        }
    }

    private void logCloudEvent(CloudEvent cloudEvent) {
        EventFormat format = EventFormatProvider
                .getInstance()
                .resolveFormat(JsonFormat.CONTENT_TYPE);

        log.info("Cloud Event: " + new String(format.serialize(cloudEvent)));

    }

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: " + clientRequest.method() + " - " + clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info(name + "=" + value)));
            return Mono.just(clientRequest);
        });
    }
}
