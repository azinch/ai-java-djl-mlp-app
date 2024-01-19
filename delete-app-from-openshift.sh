#!/usr/bin/bash

# Usage: delete-app-from-openshift <app-name>
#    app-name: trainer|predictor
#    Example:
#       delete-app-from-openshift trainer
#       delete-app-from-openshift predictor

echo
echo "======================================================="
echo "Deleting app $1 from ai-java-djl-mlp-app project on the cluster.."
echo "======================================================="
echo
case $1 in
  "trainer" )
    ;;
  "predictor" )
    ;;
  *)
    echo "Incorrect app-name to delete, exit"
    echo
	  exit 0
esac

oc delete deployment $1-app

if [ $1 == "predictor" ]
then
  oc delete route $1-app
fi

oc delete service $1-app
oc delete imagestream $1-app
oc delete buildconfig $1-app

echo
echo "========================"
echo "Delete completed"
echo "========================"
echo
