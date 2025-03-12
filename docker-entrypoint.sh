#!/bin/sh
set -ue
set -x

# Determine the memory limit (it depends on the cgroup fs version)
cgroupfsType=$(stat -f -c %T /sys/fs/cgroup/)
case "${cgroupfsType}" in
  cgroup2fs)
    memLimitInBytes=$(cat /sys/fs/cgroup/memory.max)
  ;;
  tmpfs)
    memLimitInBytes=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
  ;;
  *)
    echo "unknown fs type for /sys/fs/cgroup: ${cgroupfsType}" 1>&2 && exit 1
  ;;
esac

memLimitInMegabytes=$(( memLimitInBytes / 1024 / 1024 ))

javaHeapLimitInMegabytes=$(( memLimitInBytes * ${JAVA_HEAP_PERCENTAGE:-80} / 100 / 1024 / 1024 ))

javaHeapMemOptions="-Xms256M -Xmx${javaHeapLimitInMegabytes}M"

exec java ${javaHeapMemOptions} -jar /app/resource-catalogue-service.jar
