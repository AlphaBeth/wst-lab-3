#!/bin/sh -e

$GLASSFISH_HOME/bin/asadmin start-domain
cp /build/deployment-jaxws/target/*.war $DOMAIN_HOME/autodeploy/
tail -f $DOMAIN_HOME/logs/server.log
