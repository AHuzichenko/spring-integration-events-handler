FROM esputnik/java:8u151-jre-alpine

VOLUME /tmp
WORKDIR /opt/app/

COPY conf /opt/conf

HEALTHCHECK --start-period=10s --interval=10s --timeout=5s --retries=10\
    CMD curl --fail http://localhost:23080/monitoring/healthcheck || exit 1

STOPSIGNAL SIGTERM

LABEL auto_healing_support="true"

ADD events-handler.jar events-handler.jar


ADD entrypoint.sh /entrypoint.sh
RUN chmod 750 /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]