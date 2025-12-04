#!/bin/bash

set -ue
set -o pipefail

declare -r sbomFile="sbom.json"
declare sbomVersion="0"

declare -ra curlFlags=(
  '-f'
  '--no-progress-meter'
  '-H' "X-Api-Key: ${DEPENDENCY_TRACK_API_KEY}"
  '-H' 'Accept: application/json'
)

test -f ${sbomFile}

# Find SBOM version from project

curl "${curlFlags[@]}" -XGET ${DEPENDENCY_TRACK_API_URL}/v1/project/${DEPENDENCY_TRACK_PROJECT_ID}/property |\
  jq -r '.[] | select(.propertyName=="sbomVersion").propertyValue' > .sbom-version

# Create property for SBOM version on project (if not exists)

if ! read sbomVersion < .sbom-version; then
  sbomVersion="0"
  jq -c -n '{"groupName": "EGI", "propertyName": "sbomVersion", "propertyType": "INTEGER", "propertyValue": $sbomVersion}' \
    --arg sbomVersion ${sbomVersion} |\
  curl "${curlFlags[@]}" -XPUT ${DEPENDENCY_TRACK_API_URL}/v1/project/${DEPENDENCY_TRACK_PROJECT_ID}/property \
  -H 'Content-Type: application/json' -d @-
fi

## Upload SBOM

sbomVersion=$(( sbomVersion + 1 ))

jq -c --arg sbomVersion ${sbomVersion} '.version=($sbomVersion | tonumber)' ${sbomFile} > sbom-versioned.$$.json
mv -vb sbom-versioned.$$.json ${sbomFile}

curl "${curlFlags[@]}" -XPOST ${DEPENDENCY_TRACK_API_URL}/v1/bom \
  -H 'Content-Type: multipart/form-data' \
  -F "project=${DEPENDENCY_TRACK_PROJECT_ID}" \
  -F "bom=@${sbomFile}"

echo

# Save next SBOM version as a project property

jq -n -c --arg sbomVersion ${sbomVersion} '{"groupName": "EGI", "propertyName": "sbomVersion", "propertyValue": $sbomVersion}' |\
curl "${curlFlags[@]}" -XPOST ${DEPENDENCY_TRACK_API_URL}/v1/project/${DEPENDENCY_TRACK_PROJECT_ID}/property \
  -H 'Content-Type: application/json' -d @-

echo
