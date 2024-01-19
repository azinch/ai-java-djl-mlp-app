#!/usr/bin/bash

# Usage: deploy-app-to-openshift <app-name> <target-port> <num-of-replicas>
#    app-name: trainer|predictor ;
#    target-port: target port for predictor app container (rest api) inside a pod ;
#    num-of-replicas: number of predictor pods to run on the cluster .
#    Examples: 
#       deploy-app-to-openshift trainer
#       deploy-app-to-openshift predictor 9097 3
# Notes.
# Dedicated project (namespace), e.g. ai-java-djl-mlp-app should be created on the cluster in advance.
# For trainer app, runTrainer=Yes and predictModelDir=/home/jboss in application.properties.
# Before staring deploy-app-to-openshift, run delete-app-from-openshift to delete all the app's resources on the cluster.


echo
echo "==================================================="
echo "Adding app $1 to ai-java-djl-mlp-app project on the cluster.."
echo "==================================================="
echo

case $1 in
  "trainer" )
    ;;
  "predictor" )
    echo "Container port in a pod: $2"
    echo "Num of replica pods to run on the cluster: $3"
	  echo
    ;;
  *)
    echo "Incorrect app-name to deploy, exit"
    echo
	  exit 0
esac

mkdir -p build/libs/ocp/deployments
cp build/libs/*.jar build/libs/ocp/deployments
oc new-build --binary=true --name=$1-app --image-stream=redhat-openjdk18-openshift:1.8
oc start-build $1-app --from-dir=build/libs/ocp --follow
oc new-app $1-app

if [ $1 == "predictor" ]
then
  oc delete service $1-app
  oc expose deployment $1-app --port=8080 --target-port=$2
  oc expose service $1-app
  oc scale deploy $1-app --replicas=$3
fi

echo
echo "============================"
echo "Deployment completed"
echo "============================"
echo
