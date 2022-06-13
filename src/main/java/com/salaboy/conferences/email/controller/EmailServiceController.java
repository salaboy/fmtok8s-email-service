package com.salaboy.conferences.email.controller;

import com.salaboy.conferences.email.model.Email;
import com.salaboy.conferences.email.model.Proposal;
import com.salaboy.conferences.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class EmailServiceController {


    private static final Logger log = LoggerFactory.getLogger(EmailServiceController.class);

    @Autowired
    private EmailService emailService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Email> sendEmail(@RequestBody Map<String, String> email) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email");
        String toEmail = email.get("toEmail");
        String emailTitle = email.get("title");
        String emailContent = email.get("content");

        Email emailNotification = new Email(emailTitle, emailContent, toEmail, null, false);

        log.info("+-------------------------------------------------------------------+");
        emailService.printEmail(emailNotification);
        log.info("+-------------------------------------------------------------------+\n\n");
        return Mono.just(emailNotification);
    }


    @PostMapping("/notification")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Email> sendEmailNotification(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email Notification about a proposal from: " + proposal.email());
        return emailService.sendEmailNotificationWithLink(proposal, false).doOnSuccess(email -> emailService.emitEmailWithProposalEvent(email));

    }

    @PostMapping("/committee")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Email> sendEmailReminderToCommitteeMembers(@RequestBody Proposal proposal) {
        log.info("> REST ENDPOINT INVOKED for Sending an Email Reminder to Committee members about a proposal from: " + proposal.email());
        return emailService.sendEmailToCommittee(proposal).doOnSuccess(email -> emailService.emitEmailWithProposalEvent(email));
    }

}
