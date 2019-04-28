#!/bin/sh
set -e -u

USER="builder"
HOME="/home/builder"
IMAGE_NAME="xeffyr/alpine-term-env-builder"
CONTAINER_NAME="alpine-term-env-builder"

if [ `uname` = Darwin ]; then
    # Workaround for mac readlink not supporting -f.
    REPOROOT="${PWD}"
else
    REPOROOT=$(dirname "$(readlink -f "${0}")")/../
fi

echo "Running container '${CONTAINER_NAME}' from image '${IMAGE_NAME}'..."

docker start "${CONTAINER_NAME}" > /dev/null 2> /dev/null || {
    echo "Creating new container..."

    docker run \
            --detach \
            --env HOME="${HOME}" \
            --env LINES=$(tput lines) \
            --env COLUMNS=$(tput cols) \
            --name "${CONTAINER_NAME}" \
            --volume "${REPOROOT}:${HOME}/env-packages" \
            --tty \
            "${IMAGE_NAME}"

    if [ $(id -u) -ne 1000 -a $(id -u) -ne 0 ]; then
        echo "Changed builder uid/gid... (this may take a while)"
        docker exec --tty "${CONTAINER_NAME}" chown -R $(id -u) "${HOME}"
        docker exec --tty "${CONTAINER_NAME}" chown -R $(id -u) /data
        docker exec --tty "${CONTAINER_NAME}" usermod -u $(id -u) builder
        docker exec --tty "${CONTAINER_NAME}" groupmod -g $(id -g) builder
    fi
}

if [ "$#" -eq  "0" ]; then
    docker exec --interactive --tty --env LINES=$(tput lines) --env COLUMNS=$(tput cols) --user "${USER}" "${CONTAINER_NAME}" bash
else
    docker exec --interactive --tty --env LINES=$(tput lines) --env COLUMNS=$(tput cols) --user "${USER}" "${CONTAINER_NAME}" "${@}"
fi
