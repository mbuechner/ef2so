#!/bin/bash

if [[ -z "${URLPATTERN}" ]]; then
	URLPATTERN="/*"
fi

export CATALINA_OPTS="$CATALINA_OPTS -DURLPATTERN=${URLPATTERN}"

# Check for application specific parameters at startup
if [ -r "$CATALINA_BASE/bin/appenv.sh" ]; then
  . "$CATALINA_BASE/bin/appenv.sh"
fi

echo "Using CATALINA_OPTS:"
for arg in $CATALINA_OPTS
do
    echo ">" "$arg"
done
echo ""

echo "Using JAVA_OPTS:"
for arg in $JAVA_OPTS
do
    echo ">" "$arg"
done

