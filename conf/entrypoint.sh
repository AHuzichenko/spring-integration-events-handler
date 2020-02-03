#!/bin/bash

export TERM=xterm
echo "export TERM=xterm">>/etc/profile
export appname=events-handler
echo "export appname=events-handler">>/etc/profile
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
export conid=$shortContainerId
echo "export CONTAINER_ID=$shortContainerId">>/etc/profile
echo "export conid=$shortContainerId">>/etc/profile

#define logstash server and port
export logstash_host="logstash"
export logstash_port="5008"
echo "export logstash_host="logstash"">>/etc/profile
echo "export logstash_port="5008"">>/etc/profile
export DOCKER_CONTAINER_ID=$shortContainerId
export DOCKER_HOST=$name
export ENV=$ENV_TYPE
export APPNAME=$appname

#copy related to set ENV files $ENV_TYPE
conf_dir="/opt/conf"
app_dir="/opt/app"


mkdir -p $app_dir/lib/es/
cp -r $conf_dir/$ENV_TYPE/es/* $app_dir/lib/es/

#In case we run this container on test nodes
#if [ $ENV_TYPE = "prod" ];then echo "It is prod envirement";sed -i "s/DOCKER_HOST/${conid}_${name}/g" /opt/tomcat-ms3/lib/tomcat-log4j.xml; fi

mkdir -p $app_dir/bin/
echo 'copying JAVA_OPTS configs'
cp -R $conf_dir/$ENV_TYPE/bin/startup.sh $app_dir/bin/startup.sh

# ip.txt content example: DOCKER_HOST=10.10.10.10. 10.10.10.10 - host private ip  (DOCKER_HOST is used for logstash application identifier)
[[ ! -f /ip.txt ]] || export `cat /ip.txt`
[[ ! -f /etc/custom-hosts ]] || echo `cat /etc/custom-hosts`>>/etc/hosts

#cp -r /opt/conf/* /opt/app/

source $app_dir/bin/startup.sh

exec java $JAVA_OPTS -jar /opt/app/events-handler.jar --loader.path=$app_dir/lib/es/

