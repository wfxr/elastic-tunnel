#!/usr/bin/env bash
################################################################################
#    Author: Wenxuan                                                           #
#     Email: wenxuangm@gmail.com                                               #
#   Created: 2018-12-18 16:34                                                  #
################################################################################
SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd) && cd "$SCRIPT_DIR" || exit 1

(cd .. && ./gradlew clean build) || exit 2

mkdir -p ../build/release
release=../build/release/elastic-tunnel

cat <<-'EOF' | cat - ../build/libs/elastic-tunnel.jar > "$release"
#!/bin/sh
MYSELF=`which "$0" 2>/dev/null`
[ $? -gt 0 -a -f "$0" ] && MYSELF="./$0"
java=java
if test -n "$JAVA_HOME"; then
    java="$JAVA_HOME/bin/java"
fi
exec "$java" -jar $MYSELF "$@"
exit 1
EOF

chmod +x "$release"
