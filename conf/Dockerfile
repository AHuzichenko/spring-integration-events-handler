FROM dockerdar/jre8-alpine:r1

COPY env_conf/ /opt/conf/

COPY . /opt/conf
RUN mkdir -p /opt/app/logs && chmod +x /opt/conf/entrypoint.sh
ADD *.jar /opt/app

HEALTHCHECK CMD curl --fail http://localhost:23080/health || exit 1
STOPSIGNAL SIGTERM
WORKDIR /opt/app/



ADD entrypoint.sh /
RUN chmod 750 /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

