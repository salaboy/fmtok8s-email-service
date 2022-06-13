package com.salaboy.conferences.email;


import com.salaboy.conferences.email.model.Email;
import com.salaboy.conferences.email.model.Proposal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = EmailServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public class EmailServiceTests {

    @Autowired
    private WebTestClient webTestClient;

    //@TODO:
    // - add simple email test
    // - add conference organizers proposal email test
    // - add speaker email with link test


    @Test
    public void newProposalNotification_ShouldCreateNewEmailNotification() {

        // action
        var requestProposal =
                new Proposal(null, "Title", "Description", "Author",
                        "email@email.com", false);

        Email emailNotification = createEmailNotification(requestProposal);

        // assert
        assertThat(emailNotification).isNotNull();
        assertThat(emailNotification.title()).isNotNull().isEqualTo("Conference Committee Communication");
        assertThat(emailNotification.destinationEmail()).isNotNull().isEqualTo(requestProposal.email());
    }

    private Email createEmailNotification(Proposal proposal) {
        return webTestClient.post()
                .uri("/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(proposal))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(Email.class)
                .returnResult()
                .getResponseBody();
    }

}