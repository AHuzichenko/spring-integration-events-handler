#!/bin/bash

# --------- Setting env variables --------------
echo "Environment variables set:"
echo `env`

#Preserving passed env variables

envVariablesList=("JAVA_MEMORY_OPTS")
declare -A passedEnvVariables

for item in ${envVariablesList[@]}
do
    var=`printenv $item`
    passedEnvVariables["$item"]=`printenv $item`
done

if [ -d /opt/conf/scripts/ ] ; then
#Executing default scripts
echo "Executing default scripts"
echo `ls /opt/conf/scripts/*.sh`

for script in /opt/conf/scripts/*.sh
do
    . $script
done
fi

if [ -d /opt/conf/env/$ENV_TYPE/scripts/ ] ; then
#Executing env specific scripts
echo "Executing env specific scripts"
echo `ls /opt/conf/env/$ENV_TYPE/scripts/*.sh`

for script in /opt/conf/env/$ENV_TYPE/scripts/*.sh
do
    . $script
done
fi

#Restoring passed env variables cause they have bigger priority over ones set by env specific scripts
for item in ${envVariablesList[@]}
do
    passedEnvVar=${passedEnvVariables[$item]}
    if [ ! -z "$passedEnvVar" ]; then echo "Using $item=$passedEnvVar from passed env variables" && eval $item=$passedEnvVar; fi
done
#------------------------------------------------
ln -s /opt/app/logs /logs
echo "/logs symlink set"

export appname=events-handler
echo "export appname=events-handler">>/etc/profile
name=`hostname`
export name=`hostname`

#define host ip
if [ -z "$PUBLIC_IP" ]; then echo "PUBLIC_IP is not set. Define it."; PUBLIC_IP=`ip a | grep '172.30.' | awk -F ' ' '{ print $2 }' | awk -F '/' '{ print $1 }'`; echo "====> PUBLIC_IP: " $PUBLIC_IP; fi
CONTAINER_ID=`cat /proc/self/cgroup | grep memory | awk -F '/' '{ print $3 }'`
export server_ip=$PUBLIC_IP
echo "export server_ip=$PUBLIC_IP">>/etc/profile
echo "export server_ip=$PUBLIC_IP">>/etc/bash.bashrc

#define containerId (CONTAINER_ID is used for logstash application identifier)
containerId=`cat /proc/self/cgroup | grep memory | awk -F '/' '{ print $3 }'`
if [ -z "$containerId" ]; then echo "Try second way to find containerId"; containerId=$(cat /proc/self/cgroup | grep "docker" | sed s/\\//\\n/g | tail -1); fi
echo "containerId: " $containerId
shortContainerId=`echo $containerId | cut -c 1-12`
echo "shortContainerId: " $shortContainerId

export CONTAINER_ID=$shortContainerId
export DOCKER_HOST=$name
export DOCKER_CONTAINER_ID=$shortContainerId
export DOCKER_HOST=$name
export ENV=$ENV_TYPE
export APPNAME=$appname

#defaults
if [ -z "$JAVA_MEMORY_OPTS" ]; then echo "Use default JAVA_MEMORY_OPTS" && JAVA_MEMORY_OPTS="-Xms256m -Xmx512m"; fi



    JAVA_OPTS="$JAVA_OPTS
          -XX:+ExitOnOutOfMemoryError
          -XX:+HeapDumpOnOutOfMemoryError
          -Dspring.profiles.active=$ENV
          -Djava.security.egd=file:/dev/./urandom ";

    JAVA_OPTS+=$JAVA_MEMORY_OPTS;

echo "JAVA_OPTS: "
echo "$JAVA_OPTS"

#curl -X PUT http://localhost:8500/v1/agent/service/register -H "Content-Type: application/json" --data '{"ID": "'$appname'-'$name'","Name": "'$appname'","Port": 23080,"check": { "http": "http://'$server_ip':23080/monitoring/healthcheck", "interval": "10s"}}'
#echo "Service registered in consul"

exec java \
    $JAVA_OPTS \
    -jar /opt/app/events-handler.jar
