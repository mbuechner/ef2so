FROM maven:3-openjdk-12 AS MAVEN_CHAIN
MAINTAINER Michael Büchner <m.buechner@dnb.de>
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package

FROM tomcat:10-jdk12-openjdk-slim
MAINTAINER Michael Büchner <m.buechner@dnb.de>
RUN rm -R $CATALINA_HOME/webapps/ROOT/
COPY --from=MAVEN_CHAIN /tmp/target/ef2so.war $CATALINA_HOME/webapps/ROOT.war

HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/118540238 || exit

EXPOSE 8080
