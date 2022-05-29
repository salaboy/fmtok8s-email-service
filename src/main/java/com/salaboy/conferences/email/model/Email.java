package com.salaboy.conferences.email.rest.model;

import com.salaboy.conferences.email.model.Proposal;

public class Email {
    private String title;
    private String body;
    private String destinationEmail;
    private Proposal proposal;
    private boolean withLink;

    public Email() {
    }

    public Email(String title, String body, String destinationEmail, Proposal proposal) {
        this.title = title;
        this.body = body;
        this.destinationEmail = destinationEmail;
        this.proposal = proposal;
    }

    public Email(String title, String body, String destinationEmail, Proposal proposal, boolean withLink) {
        this.title = title;
        this.body = body;
        this.destinationEmail = destinationEmail;
        this.proposal = proposal;
        this.withLink = withLink;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getDestinationEmail() {
        return destinationEmail;
    }

    public void setDestinationEmail(String destinationEmail) {
        this.destinationEmail = destinationEmail;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public boolean isWithLink() {
        return withLink;
    }

    public void setWithLink(boolean withLink) {
        this.withLink = withLink;
    }
}
