#!/bin/sh
set -e

if [ -x /usr/local/bin/consul-template ] && [ -f /etc/consul-template/consul-template.hcl ]; then
  /usr/local/bin/consul-template -config=/etc/consul-template/consul-template.hcl &
fi
