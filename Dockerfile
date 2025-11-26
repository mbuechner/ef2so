FROM maven:3-amazoncorretto-21 AS mchain
WORKDIR /tmp
COPY . .
RUN sed -i 's#<url-pattern>/\*</url-pattern>#<url-pattern>${URLPATTERN}</url-pattern>#' src/main/webapp/WEB-INF/web.xml; \
  mvn package;

FROM tomcat:11-jre21
LABEL maintainer="Michael Büchner <m.buechner@dnb.de>"
COPY --from=mchain /tmp/target/ef2so.war ${CATALINA_HOME}/webapps/ROOT.war
COPY --from=mchain /tmp/setenv.sh ${CATALINA_HOME}/bin/

EXPOSE 8080
