FROM maven:3-amazoncorretto-21 AS mchain
WORKDIR /tmp
COPY . .
RUN sed -i 's#<url-pattern>/\*</url-pattern>#<url-pattern>${URLPATTERN}</url-pattern>#' src/main/webapp/WEB-INF/web.xml; \
  mvn package;

FROM tomcat:11-jre21
LABEL maintainer="Michael Büchner <m.buechner@dnb.de>"

# Default-Webapps raus
RUN rm -rf "${CATALINA_HOME}/webapps/"*

# App + setenv.sh kopieren
COPY --from=mchain /tmp/target/ef2so.war ${CATALINA_HOME}/webapps/ROOT.war
COPY --from=mchain /tmp/setenv.sh ${CATALINA_HOME}/bin/

# OpenShift-Compatibility
RUN chmod +x "${CATALINA_HOME}/bin/setenv.sh" \
 && chgrp -R 0 "${CATALINA_HOME}" \
 && chmod -R g=u "${CATALINA_HOME}"

EXPOSE 8080
