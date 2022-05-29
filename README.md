# From Monolith To K8s :: Email Service 

Email Service, which expose a REST API to send Emails, and possible connects to an SMTP server.

## Build and Release

```
mvn package
```

```
docker build -t salaboy/fmtok8s-email-service:0.1.0
docker push salaboy/fmtok8s-email-service:0.1.0
```

```
cd helm/fmtok8s-email-service
helm package .
```

Copy tar to http://github.com/salaboy/helm and push
