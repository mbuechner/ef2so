FROM maven:3-openjdk-15 AS MAVEN_CHAIN
MAINTAINER Michael Büchner <m.buechner@dnb.de>
COPY pom.xml /tmp/
COPY setenv.sh /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN sed -i 's#<url-pattern>/\*</url-pattern>#<url-pattern>${URLPATTERN}</url-pattern>#' src/main/webapp/WEB-INF/web.xml
RUN mvn package

FROM tomcat:jdk15-openjdk-slim-buster
MAINTAINER Michael Büchner <m.buechner@dnb.de>
COPY --from=MAVEN_CHAIN /tmp/target/ef2so.war $CATALINA_HOME/webapps/ROOT.war
COPY --from=MAVEN_CHAIN /tmp/setenv.sh $CATALINA_HOME/bin/
RUN apt-get update && apt-get install -y wget
RUN apt-get clean && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/118540238 || exit

EXPOSE 8080
