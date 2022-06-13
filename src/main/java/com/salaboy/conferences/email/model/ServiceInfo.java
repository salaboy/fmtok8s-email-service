package com.salaboy.conferences.email.model;

public record ServiceInfo(String name,
                          String version,
                          String source,
                          String podId,
                          String podNamepsace,
                          String podNodeName) {
}
