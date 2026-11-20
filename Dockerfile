FROM maven:3-amazoncorretto-25 AS build

WORKDIR /build
COPY pom.xml .
COPY src src
ARG SOURCE_DATE_EPOCH=315532802
# Tomcat substitutes URLPATTERN when it starts.
RUN sed -i "s#<url-pattern>/[*]</url-pattern>#<url-pattern>\${URLPATTERN}</url-pattern>#" src/main/webapp/WEB-INF/web.xml \
 && grep -Fq "<url-pattern>\${URLPATTERN}</url-pattern>" src/main/webapp/WEB-INF/web.xml \
 && mvn -B -ntp "-Dproject.build.outputTimestamp=${SOURCE_DATE_EPOCH}" clean verify

FROM tomcat:11-jre25-temurin-noble AS tomcat-dist

FROM amazoncorretto:25
LABEL maintainer="Michael Büchner <m.buechner@dnb.de>"

ENV CATALINA_HOME=/opt/tomcat
WORKDIR /opt/tomcat

COPY --from=tomcat-dist /usr/local/tomcat/bin/ /opt/tomcat/bin/
COPY --from=tomcat-dist /usr/local/tomcat/conf/ /opt/tomcat/conf/
COPY --from=tomcat-dist /usr/local/tomcat/lib/ /opt/tomcat/lib/
COPY --from=tomcat-dist /usr/local/tomcat/LICENSE /usr/local/tomcat/NOTICE /usr/local/tomcat/RELEASE-NOTES /opt/tomcat/
COPY docker/logging.properties /opt/tomcat/conf/logging.properties
COPY --from=build /build/target/ef2so.war /opt/tomcat/webapps/ROOT.war
COPY --from=build /build/target/ef2so-sbom.json /opt/ef2so/ef2so-sbom.json
COPY setenv.sh /opt/tomcat/bin/setenv.sh

# Use the JVM's TLS implementation and the WAR's access log.
RUN sed -i \
      -e '/<Listener className="org\.apache\.catalina\.core\.AprLifecycleListener"/d' \
      -e 's#<Listener className="org\.apache\.catalina\.startup\.VersionLoggerListener"[[:space:]]*/>#<Listener className="org.apache.catalina.startup.VersionLoggerListener" logArgs="false" />#' \
      -e '/<Valve className="org\.apache\.catalina\.valves\.AccessLogValve"/{' \
      -e ':accessLog' -e '/\/>[[:space:]]*$/d' -e 'N; b accessLog' -e '}' conf/server.xml \
 && grep -Fq 'VersionLoggerListener" logArgs="false"' conf/server.xml \
 && ! grep -Eq 'org\.apache\.catalina\.(core\.AprLifecycleListener|valves\.AccessLogValve)"' conf/server.xml

# OpenShift can substitute an arbitrary UID in group 0.
RUN mkdir -p conf/Catalina/localhost logs temp webapps work \
 && chmod g=u logs temp webapps work

USER 1001:0
EXPOSE 8080
CMD ["/opt/tomcat/bin/catalina.sh", "run"]
