#!/bin/sh

export URLPATTERN="${URLPATTERN:-/*}"
export CATALINA_OPTS="${CATALINA_OPTS:+${CATALINA_OPTS} }-Dorg.apache.tomcat.util.digester.PROPERTY_SOURCE=org.apache.tomcat.util.digester.EnvironmentPropertySource"

# Check for application specific parameters at startup
if [ -r "$CATALINA_BASE/bin/appenv.sh" ]; then
  . "$CATALINA_BASE/bin/appenv.sh"
fi
