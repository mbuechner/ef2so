FROM maven:3-jdk-8-alpine AS MAVEN_CHAIN
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

FROM tomcat:9.0-jre8-alpine
RUN rm -R $CATALINA_HOME/webapps/ROOT/
COPY --from=MAVEN_CHAIN /tmp/target/ef2so.war $CATALINA_HOME/webapps/ROOT.war

HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/118540238 || exit

EXPOSE 8080