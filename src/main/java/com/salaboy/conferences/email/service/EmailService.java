package com.salaboy.conferences.email.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaboy.conferences.email.model.Email;
import com.salaboy.conferences.email.model.Proposal;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;


@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Value("${EXTERNAL_URL:http://fmtok8s-frontend.default.X.X.X.X.sslip.io}")
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


    public Mono<Email> sendEmailNotificationWithLink(Proposal proposal, boolean withLink) {
        String emailBody = "Dear " + proposal.author() + ", \n";
        String emailTitle = "Conference Committee Communication";
        emailBody += "\t\t We are";
        if (proposal.approved()) {
            emailBody += " happy ";
        } else {
            emailBody += " sorry ";
        }
        emailBody += "to inform you that: \n";
        emailBody += "\t\t\t `" + proposal.title() + "` -> `" + proposal.description() + "`, \n";
        emailBody += "\t\t was";
        if (proposal.approved()) {
            emailBody += " approved ";
        } else {
            emailBody += " rejected ";
        }
        emailBody += "for this conference.";
        Email emailNotification = new Email(emailTitle, emailBody, proposal.email(), proposal, withLink);
        printProposalEmail(emailNotification);
        return Mono.just(emailNotification);
    }

    public Mono<Email> sendEmailToCommittee(Proposal proposal) {
        String emailTitle = "Conference Committee Please Review Proposal";
        String emailBody = "Dear Committee Member, \n" +
                "\t\t please review and accept or reject the following proposal \n";
        emailBody += "\t From Author: " + proposal.author() + "\n";
        emailBody += "\t With Id: " + proposal.id() + "\n";
        emailBody += "\t Notification Sent at: " + new Date() + "\n";
        Email emailNotification = new Email(emailTitle, emailBody, "committee@conference.org", proposal, false);
        log.info("+-------------------------------------------------------------------+");
        printEmail(emailNotification);
        log.info("+-------------------------------------------------------------------+");
        return Mono.just(emailNotification);
    }

    public void printEmail(Email email) {
        log.info("\t Email Sent to: " + email.destinationEmail());
        log.info("\t Email Title: " + email.title());
        log.info("\t Email Body: " + email.body());
    }

    public void printProposalEmail(Email email) {
        assert (email.proposal() != null);
        log.info("+-------------------------------------------------------------------+");
        printEmail(email);
        if (email.withLink()) {
            log.info("\t Please CURL the following link to confirm \n" +
                    "\t\t that you are committing to speak in our conference: \n" +
                    "\t\t curl -X POST " + externalURL + "/speakers/" + email.proposal().id());
        }
        log.info("+-------------------------------------------------------------------+\n\n");
    }

    public Mono<Email> emitEmailWithProposalEvent(Email email) {
        log.info("> \t EventsEnabled: " + eventsEnabled);
        if (eventsEnabled) {
            CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withType("Email.Sent")
                    .withSource(URI.create("email-service.default.svc.cluster.local"))
                    .withData(writeValueAsString(email).getBytes(StandardCharsets.UTF_8))
                    .withDataContentType("application/json")
                    .withSubject(email.title());

            CloudEvent cloudEvent = cloudEventBuilder.build();

            logCloudEvent(cloudEvent);

            log.info("Producing CloudEvent with Email Notification: " + email);

            rest.baseUrl(K_SINK).filter(logRequest()).build()
                    .post().bodyValue(cloudEvent)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(t -> t.printStackTrace())
                    .doOnSuccess(s -> log.info("Result -> " + s)).subscribe();
        }

        return Mono.just(email);

    }

    private String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error when serializing Score", ex);
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
