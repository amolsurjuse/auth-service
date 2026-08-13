# TeamCity pipeline setup

This folder contains an idempotent script to provision and repair the `auth-service` TeamCity pipeline using the TeamCity REST API.

## Prerequisites
- TeamCity server running at `http://localhost:8111`
- Super-user token (or personal access token)
- `jq` installed

## Usage
```bash
cd /Users/amolsurjuse/development/projects/auth-service
TEAMCITY_TOKEN='<token>' ./ci/teamcity/setup_pipeline.sh
```

## What it repairs
- Ensures the TeamCity project, VCS root, build configuration, parameters, trigger, and agent requirement exist
- Recreates the expected build steps when the live build drifts or gets corrupted

## Optional environment overrides
- `TEAMCITY_URL` (default `http://localhost:8111`)
- `TEAMCITY_PARENT_PROJECT_ID` (default `Amy`)
- `TEAMCITY_PROJECT_ID` (default `Amy_AuthService`)
- `TEAMCITY_BUILD_TYPE_ID` (default `Amy_AuthService_Build`)
- `AUTH_SERVICE_GIT_URL` (default `https://github.com/amolsurjuse/auth-service`)
- `AUTH_SERVICE_GIT_BRANCH` (default `develop`)
- `AUTH_SERVICE_DOCKER_IMAGE` (default `amolsurjuse/auth-service`)
