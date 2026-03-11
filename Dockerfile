FROM ubuntu:latest
LABEL authors="wilco"

ENTRYPOINT ["top", "-b"]
