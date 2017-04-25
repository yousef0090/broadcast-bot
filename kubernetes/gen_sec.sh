#!/bin/bash

SECRET_NAME="broadcast-knows"
AUTH_TOKEN="your_token"
KEYSTORE_PASSWORD="your_password"
APP_SECRET="your_secret"

kubectl delete secret $SECRET_NAME
kubectl create secret generic $SECRET_NAME \
    --from-literal=token=$AUTH_TOKEN \
    --from-literal=keystore_password=$KEYSTORE_PASSWORD \
    --from-literal=app_secret=$APP_SECRET
