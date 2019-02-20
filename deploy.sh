#!/usr/bin/env bash

#login = `aws ecr get-login --no-include-email`
#exec $login

docker.exe tag servantscode/web:latest 822695322560.dkr.ecr.us-east-2.amazonaws.com/servantscode/web:latest
docker.exe push 822695322560.dkr.ecr.us-east-2.amazonaws.com/servantscode/web:latest
