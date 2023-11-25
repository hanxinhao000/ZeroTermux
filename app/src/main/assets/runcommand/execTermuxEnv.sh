#!/system/bin/sh
#################ZeroTermux###########
#    ZeroTermux Shell 启动会话脚本     #
######################################
export PREFIX='/data/data/com.termux/files/usr'
export HOME='/data/data/com.termux/files/home'
export LD_LIBRARY_PATH='/data/data/com.termux/files/usr/lib'
export PATH="/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets:$PATH"
export LANG='en_US.UTF-8'
export SHELL='/data/data/com.termux/files/usr/bin/bash'
cd "$HOME"
exec "$SHELL" -l

