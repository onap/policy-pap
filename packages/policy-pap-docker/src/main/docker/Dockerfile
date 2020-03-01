FROM onap/policy-jre-alpine:2.0.1

LABEL maintainer="Policy Team"

ARG POLICY_LOGS=/var/log/onap/policy/pap

ENV POLICY_LOGS=$POLICY_LOGS
ENV POLICY_HOME=$POLICY_HOME/pap

RUN mkdir -p $POLICY_HOME $POLICY_LOGS $POLICY_HOME/bin && \
    chown  -R policy:policy $POLICY_HOME $POLICY_LOGS && \
    mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-pap.tar.gz --directory $POLICY_HOME \
    && rm /packages/policy-pap.tar.gz

WORKDIR $POLICY_HOME
COPY policy-pap.sh  bin/.

RUN chown -R policy:policy * && chmod 755 bin/*.sh

USER policy
WORKDIR $POLICY_HOME/bin
ENTRYPOINT [ "bash", "./policy-pap.sh" ]
