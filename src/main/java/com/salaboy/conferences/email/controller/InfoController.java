package com.salaboy.conferences.email.controller;

import com.salaboy.conferences.email.model.ServiceInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @Value("${version:0.0.0}")
    private String version;

    @Value("${POD_ID:}")
    private String podId;

    @Value("${POD_NODE_NAME:}")
    private String podNodeName;

    @Value("${POD_NAMESPACE:}")
    private String podNamespace;

    //@TODO: add to EndPoint Actuator to leverage Spring Boot architecture
    @GetMapping("/info")
    public ServiceInfo getInfo() {
        return new ServiceInfo(
                "Email Service",
                version,
                "https://github.com/salaboy/fmtok8s-email-service/releases/tag/" + version,
                podId,
                podNamespace,
                podNodeName);
    }
}
