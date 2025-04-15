#!/bin/bash
ARCH=$(uname -m)
echo "arch=$ARCH" > "$INSTALL_PATH/arch.properties"
