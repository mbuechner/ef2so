FROM maven:3-openjdk-14 AS MAVEN_CHAIN
MAINTAINER Michael Büchner <m.buechner@dnb.de>
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

FROM tomcat:jdk14-openjdk-slim-buster
MAINTAINER Michael Büchner <m.buechner@dnb.de>
COPY --from=MAVEN_CHAIN /tmp/target/ef2so.war $CATALINA_HOME/webapps/ROOT.war

HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/118540238 || exit

EXPOSE 8080
