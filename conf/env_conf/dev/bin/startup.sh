#!/bin/sh

cd /opt/app

#define app name
APPNAME="events-handler"
JAVA_HOME=/usr/local/jre1.8.0_131

JAVA_OPTS="$JAVA_OPTS -Xms256m -Xmx768m -XX:MaxPermSize=128m"
JAVA_OPTS="$JAVA_OPTS -Dserver_log_dir=/opt/app/logs"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
JAVA_OPTS="$JAVA_OPTS -XX:-OmitStackTraceInFastThrow"
JAVA_OPTS="$JAVA_OPTS -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl"
JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote.port=9923 -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.rmi.port=9923 -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=$JMX_HOST -Djava.net.preferIPv4Stack=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote"

JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,address=7723,suspend=n,server=y"

