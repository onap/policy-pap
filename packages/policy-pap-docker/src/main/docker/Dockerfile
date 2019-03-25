FROM onap/policy-common-alpine:1.4.0

LABEL maintainer="Policy Team"

ARG POLICY_LOGS=/var/log/onap/policy/pap

ENV POLICY_LOGS ${POLICY_LOGS}
ENV POLICY_PAP_HOME=${POLICY_HOME}/pap

RUN mkdir -p ${POLICY_PAP_HOME} ${POLICY_LOGS} ${POLICY_PAP_HOME}/bin && \
    chown  -R policy:policy ${POLICY_PAP_HOME} ${POLICY_LOGS}

RUN mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-pap.tar.gz --directory ${POLICY_PAP_HOME} \
    && rm /packages/policy-pap.tar.gz

WORKDIR ${POLICY_PAP_HOME}
COPY policy-pap.sh  bin/.
RUN chown -R policy:policy * && chmod +x bin/*.sh \
   && cp ${POLICY_PAP_HOME}/etc/ssl/* ${POLICY_HOME}/etc/ssl && chown policy:policy ${POLICY_HOME}/etc/ssl/*

USER policy
WORKDIR ${POLICY_PAP_HOME}/bin
ENTRYPOINT [ "bash", "./policy-pap.sh" ]
