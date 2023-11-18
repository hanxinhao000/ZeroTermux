#!/system/bin/sh
PREFIX=/data/data/com.termux/files/usr
shell_main() {
# Kill processes on exit to avoid hanging on exit
ARGS="--kill-on-exit"

# For the /system/bin/linker(64) to be found:
ARGS="$ARGS -b /system:/system"

# On some devices /vendor is required for termux packages to work correctly
# See https://github.com/termux/proot/issues/2#issuecomment-303995382
ARGS="$ARGS -b /vendor:/vendor"

# Bind /data to include system folders such as /data/misc. Also $PREFIX
# and $HOME so that Termux programs with hard-coded paths continue to work:
ARGS="$ARGS -b /data:/data"


# Bind Magisk binary directories so root works, closing per Issue #2100.
if [ -d /sbin ] && [ -d /root ]; then
	# Both of these directories exist under Android even without Magisk installed,
	# The existence check is to ensure that it doesn't break if this changes.
	ARGS="$ARGS -b /sbin:/sbin -b /root:/root"
fi

# Android 10 needs /apex for /system/bin/linker:
# https://github.com/termux/proot/issues/95#issuecomment-584779998
if [ -d /apex ]; then
	ARGS="$ARGS -b /apex:/apex"
fi

# Android 11.
if [ -e "/linkerconfig/ld.config.txt" ]; then
	ARGS="$ARGS -b /linkerconfig/ld.config.txt:/linkerconfig/ld.config.txt"
fi

if [ -f /property_contexts ]; then
	# Used by getprop (see https://github.com/termux/termux-packages/issues/1076)
	# but does not exist on Android 8.
	ARGS="$ARGS -b /property_contexts:/property_contexts"
fi

# Expose external and internal storage:
if [ -d /storage ]; then
	ARGS="$ARGS -b /storage:/storage"
fi

# Mimic traditional Linux file system hierarchy - /usr:
ARGS="$ARGS -b $PREFIX:/usr"

# Mimic traditional Linux file system hierarchy - other Termux dirs:
for f in bin etc lib share tmp var; do
	ARGS="$ARGS -b $PREFIX/$f:/$f"
done

# Mimic traditional Linux file system hierarchy- system dirs:
for f in dev proc; do
	ARGS="$ARGS -b /$f:/$f"
done

# Set /home as current directory:
ARGS="$ARGS --cwd=/home"

# Root of the file system:
ARGS="$ARGS -r $PREFIX/.."
ARGS="$ARGS"

# Shell to execute:
PROGRAM=/bin/bash
if [ -x $HOME/.termux/shell ]; then
	PROGRAM=`readlink -f $HOME/.termux/shell`
fi
export HOME=/home
exec $PREFIX/bin/proot $ARGS sh -c "$*"
ls
echo "hahahahhahaha"
}
