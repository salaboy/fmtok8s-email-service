package com.salaboy.conferences.email.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.conferences.email.rest.model.Proposal;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
@RestController
@Slf4j
public class EmailService {

    public static void main(String[] args) {
        SpringApplication.run(EmailService.class, args);
    }

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${EXTERNAL_URL:http://fmtok8s-api-gateway.default.34.91.93.206.xip.io}")
    private String externalURL;

    @Value("${EVENTS_ENABLED:false}")
    private Boolean eventsEnabled;

    @Value("${K_SINK:http://broker-ingress.knative-eventing.svc.cluster.local/default/default}")
    private String K_SINK;

    @Autowired
    private WebClient.Builder rest;

    @Configuration
    public static class CloudEventHandlerConfiguration implements CodecCustomizer {

        @Override
        public void customize(CodecConfigurer configurer) {
            configurer.customCodecs().register(new CloudEventHttpMessageReader());
            configurer.customCodecs().register(new CloudEventHttpMessageWriter());
        }

    }


    @PostMapping("/")
    public void sendEmail(@RequestBody Map<String, String> email) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email");
        String toEmail = email.get("toEmail");
        String emailTitle = email.get("title");
        String emailContent = email.get("content");
        log.info("+-------------------------------------------------------------------+");
        printEmail(toEmail, emailTitle, emailContent);
        log.info("+-------------------------------------------------------------------+\n\n");
    }


    @PostMapping("/notification")
    public void sendEmailNotification(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email Notification about a proposal from: " + proposal.getEmail());
        sendEmailNotificationWithLink(proposal, false);
        log.info("> \t EventsEnabled: " + eventsEnabled);
        if(eventsEnabled) {
            try {
                emitEmailWithForProposalEvent(proposal);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    @PostMapping("/committee")
    public void sendEmailReminderToCommitteeMembers(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email Reminder to Committee members about a proposal from: " + proposal.getEmail());
        sendEmailToCommittee(proposal);
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

    public void emitEmailWithForProposalEvent(Proposal proposal) throws JsonProcessingException {

        CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("Email.Sent")
                .withSource(URI.create("email-service.default.svc.cluster.local"))
                .withData(objectMapper.writeValueAsString(proposal).getBytes(StandardCharsets.UTF_8))
                .withDataContentType("application/json")
                .withSubject(proposal.getTitle());

        CloudEvent cloudEvent = cloudEventBuilder.build();

        logCloudEvent(cloudEvent);

        log.info("Producing CloudEvent with Proposal: " + proposal);

        rest.baseUrl(K_SINK).filter(logRequest()).build()
                .post().bodyValue(cloudEvent)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(t -> t.printStackTrace())
                .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();

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
