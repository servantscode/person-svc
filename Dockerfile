#Dockerfile

FROM tomcat:9.0.11

LABEL maintainer="greg@servantscode.org"

RUN rm -rf /usr/local/tomcat/webapps/*
COPY ./build/libs/person-svc-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

