package com.salaboy.conferences.email.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Proposal(String id, String title, String description, String author, String email, boolean approved) {

}
