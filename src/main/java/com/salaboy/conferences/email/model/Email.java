package com.salaboy.conferences.email.model;

public record Email(String title, String body, String destinationEmail, Proposal proposal, boolean withLink) {

}
