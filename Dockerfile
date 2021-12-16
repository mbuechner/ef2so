FROM maven:3-openjdk-17-slim AS MAVEN_CHAIN
COPY pom.xml /tmp/
COPY setenv.sh /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN sed -i 's#<url-pattern>/\*</url-pattern>#<url-pattern>${URLPATTERN}</url-pattern>#' src/main/webapp/WEB-INF/web.xml; \
  mvn package;

FROM tomcat:10-jdk17-openjdk-slim-buster
MAINTAINER Michael Büchner <m.buechner@dnb.de>
ENV RUN_USER tomcat
ENV RUN_GROUP 0
RUN groupadd -r ${RUN_GROUP}; \
  useradd -g ${RUN_GROUP} -d ${CATALINA_HOME} -s /bin/bash ${RUN_USER};
COPY --from=MAVEN_CHAIN --chown=${RUN_USER}:${RUN_GROUP} /tmp/target/ef2so.war ${CATALINA_HOME}/webapps/ROOT.war
COPY --from=MAVEN_CHAIN --chown=${RUN_USER}:${RUN_GROUP} /tmp/setenv.sh ${CATALINA_HOME}/bin/
RUN mkdir -p /usr/local/tomcat/conf/Catalina/localhost; \
  chown -R ${RUN_USER}:${RUN_GROUP} ${CATALINA_HOME}; \
  chmod -R 777 ${CATALINA_HOME}/webapps /usr/local/tomcat/conf/Catalina; \
  apt-get update && apt-get install -y wget && apt-get clean && rm -rf /var/lib/apt/lists/*;

HEALTHCHECK --interval=1m --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:8080/118540238 || exit
EXPOSE 8080
