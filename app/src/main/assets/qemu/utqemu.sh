#!/usr/bin/env bash
cd
####################
#sync && echo 3 >/proc/sys/vm/drop_caches
#am start -n x.org.server/x.org.server.MainActivity
#am start -n com.realvnc.viewer.android/com.realvnc.viewer.android.app.ConnectionChooserActivity
#am start -n com.google.android.documentsui/com.android.documentsui.files.FilesActivity
#time echo "scale=5000; 4*a(1)" | bc -l -q
UPDATE="2022/11/29"
INFO() {
	clear
	printf "${YELLOW}更新日期$UPDATE 更新内容${RES}
	修复容器的bug
	不再为qemu版本区分容器
	仅优化容器
	为减少安装占用，部分不常用功能参数的默认安装包改为促发安装
	增加termux环境的qemu本地共享(在termux目录下创建share共享文件夹，模拟系统可同步访问文件夹内容)
	termux环境增加轻量化容器+qemu(约322m，镜像与默认配置两个选项，vnc输出)
	容器内增加dos环境体验，可运行dos游戏(游戏需另外下载)
	修复一下已知小bug
	增加最近比较火的box86+box64+wine的编译安装，因为是proot，bug比较多。由于同时支持i386与x86_64，涉及到两种架构，建议使用本脚本容器

${GREEN}ps:	重要的事情说三次，通过tcg加速的cpu核心数不是越多越好，要看手机性能，多了反而手机吃不消，建议2-8核
	qemu6.0以上似乎恢复对旧windows系统支持${RES}\n"
}
###################
NOTE() {
	clear
	printf "${YELLOW}注意事项${RES}
	本脚本是方便大家简易配置，所有参数都是经多次测试通过，可运行大部分系统，由于兼容问题，性能不作保证，专业玩家请自行操作。
	模拟效率低？是的，这是整机模拟，以体验为主，想玩游戏，可以去了解exagear，一款安卓app。或者其linux版wine。
	通过tcg加速的cpu核心数不是越多越好，要看手机性能，多了反而手机吃不消，建议2-8核
	xp玩经典游戏(如星际争霸，帝国时代)需使用cirrus显卡才能运行
	模拟效率，因手机而异，termux(utermux)在后台容易被停或降低效率。通过分屏模拟的效果是aspice>vnc>xsdl。
	q35主板与sata，virtio硬盘接口由于系统原因，可能导致启动不成功。
	qemu5.0以下模拟xp较好，qemu5.0以上对win7以上模拟较好，qemu6.0似乎恢复对旧windows系统的支持
	大页文件虽然可以分担设备ram，但同时会提高设备cpu负担，且创建大容量文件，请审慎使用
	最近新增的内容比较多，如不能正常加载，请选择1重新安装qemu\n"
	if [ $(command -v qemu-system-x86_64) ]; then
		echo -e "\e[33m检测到你已安装qemu-system-x86，版本是\e[0m"
		echo -e "\e[32mQEMU emulator version $(echo $qemu_ver | awk '{print $4}')\e[0m"
	else
	echo -e "\e[1;31m检测到你未安装qemu-system-x86，请先选择安装\e[0m"
	fi
}
###################
ABOUT_UTQEMU(){
	clear
	printf "${YELLOW}关于utqemu脚本${RES}
	最初是为utermux写下的qemu-system-x86脚本，目的是增加utermux可选功能，给使用者提供简易快捷的启动，我是业余爱好者，给使用者提供简易快捷的启动。非专业人士，所以内容比较乱，请勿吐槽。为适配常用镜像格式，脚本的参数选用是比较常用。业余的我，专业的参数配置并不懂，脚本参数都是来自官方网站、百度与群友。qemu5.0以上的版本较旧版本变化比较大，所以5.0后的参数选项比较丰富，欢迎群友体验使用。\n\n"
	case $SYS in
		ANDROID) CONFIRM 
			QEMU_SYSTEM ;;
		*) COMPILE ;;
	esac


}
###################
COMPILE(){
	read -r -p "是否编译安装各版本qemu(仅编译x86与i386) 1)是 9)返回 " input
	case $input in
	1) echo -e "请选择qemu版本
1) 2*
2) 3*
3) 4*
4) 5*
5) 6*
6) 7*"
	read -r -p "请选择 " input
	case $input in
		1) VERSION=2 ;;
		2) VERSION=3 ;;
		3) VERSION=4 ;;
		4) VERSION=5 ;;
		5) VERSION=6 ;;
		6) VERSION=7
#RC="rc" 
;;
		*) ABOUT_UTQEMU ;;
	esac
	echo -e "${YELLOW}安装所需依赖包${RES}"
	cd
	sudo_
	$sudo apt install git libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libsdl1.2-dev libsnappy-dev liblzo2-dev automake gcc python3 python3-setuptools build-essential ninja-build libspice-server-dev libsdl2-dev libspice-protocol-dev meson libgtk-3-dev libaio-dev gettext samba xz-utils usbutils telnet wget libvirglrenderer-dev libusb-dev libusb-1.0-0 pulseaudio -y
#libbluetooth-dev libbrlapi-dev libbz2-dev libcap-dev libcap-ng-dev libcurl4-gnutls-dev libibverbs-dev libncurses5-dev libnuma-dev librbd-dev librdmacm-dev libsasl2-dev libseccomp-dev flex bison git-email libssh2-1-dev libvde-dev libvdeplug-dev libvte-2.91-dev libxen-dev valgrind xfslibs-dev libnfs-dev libiscsi-dev libjpeg-dev libgbm-dev libgoogle-perftools-dev -y
#	libui-gxmlcpp-dev
	if [ $? != 0 ]; then
	$sudo apt install -f
	fi
	if [ $? != 0 ]; then
	if [ `whoami` == "root" ]; then
	echo -e "依赖包是否正常安装
1) 是
2) 否，进行低级安装(很慢，不推荐)
3) 忽略，继续编译
9) 返回"
	read -r -p "请选择: " input
	case $input in
		2)
	for i in git libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libsdl1.2-dev libsnappy-dev liblzo2-dev automake gcc python3 python3-setuptools build-essential ninja-build libspice-server-dev libsdl2-dev libspice-protocol-dev meson libgtk-3-dev libaio-dev gettext samba xz-utils usbutils telnet wget libvirglrenderer-dev libusb-dev libusb-1.0-0 pulseaudio; do $sudo apt install $i -y; done ;;
	9) ABOUT_UTQEMU ;;
	*) ;;
	esac
	fi
	fi
	echo -e "${YELLOW}检测下载${RES}"
	VERSION=$(curl https://download.qemu.org | grep qemu-${VERSION}\..*.$RC.*\.tar.xz\" | tail -n 1 | awk -F 'href="' '{print $2}' | awk -F '.tar' '{print $1}')
	if [ -z "$VERSION" ]; then
	echo -e "${RED}获取失败，请重试${RES}"
	CONFIRM
	ABOUT_UTQEMU
	fi
	echo -e "${YELLOW}最新版本为$VERSION${RES}"
	sleep 1
	if [ ! -f $(pwd)/"$VERSION.tar.xz" ]; then
		read -r -p "选择下载工具 1)curl 2)wget :" input
		case $input in
			1) curl -O https://download.qemu.org/$VERSION.tar.xz ;;
			*) wget https://download.qemu.org/$VERSION.tar.xz ;;
		esac
	fi
	if [ ! -f $(pwd)/"$VERSION.tar.xz" ]; then
	echo -e "${RED}获取失败，请重试${RES}"
	CONFIRM
	ABOUT_UTQEMU
	else
	LENGTH=$(curl -sI https://download.qemu.org/$VERSION.tar.xz | grep Length | awk '{print $2}'| sed "s/\r//")

	if [ $(ls -l "$VERSION".tar.xz | awk '{print $5}') -ne $LENGTH ]; then
	echo -e "${YELLOW}下载的文件大小与检测的不符，请确认是否下载成功${RES}"
	sleep 2
	fi
	tar xvJf $VERSION.tar.xz
	if [ $? == 1 ]; then
		echo -e "${RED}解压失败，请重试${RES}"
		sleep 2
		rm -rf $VERSION.tar.xz $VERSION
		ABOUT_UTQEMU
	fi
	cd $VERSION
	fi
./configure --target-list=i386-softmmu,x86_64-softmmu --enable-spice --enable-gtk --enable-sdl --audio-drv-list=oss,alsa,sdl,pa --enable-opengl -rpth=/usr/lib/aarch64-linux-gnu --python=$(command -v python3)
	if [ $? != 0 ]; then
		echo -e "${RED}编译失败${RES}"
		CONFIRM
		ABOUT_UTQEMU
	fi
	make -j8 && make install
	if [ -e /usr/local/bin/qemu-system-i386 ]; then
		echo -e "${YELLOW}已安装\n删除源文件...${RES}"
		cd && rm -rf $VERSION
		rm -rf $VERSION.tar.xz
		echo -e "\e[33m请重启容器\e[0m"                                                             
		read input
		unset input
		exit 1
	fi
	unset VERSION
 ;;
	*) ;;
	esac
	QEMU_SYSTEM	
}
###################
ABOUT_VIRTIO(){
	clear
	printf "${YELLOW}关于virtio驱动${RES}
	引用官方说法：QEMU为用户提供并行虚拟化块设备和网络设备的能力，其是借助virtio驱动实现的，拥有更好的性能表现以及更低的开销。

${YELLOW}virtio驱动的安装${RES}
	需下载好virtio驱动光盘，virtio磁盘接口安装程序比较多，其他驱动与普通的硬件驱动一样安装，本脚本已加入qxl显卡，virtio显卡，virtio网卡，virtio磁盘选项。

${YELLOW}关于virtio显卡3D加速${RES}
	virtio显卡因参数问题，未发挥其特性功能。3D模式需在gtk或sdl下才能开启，sdl模块在系统源默认是未编译。gtk则可在图形界面中启动。经过多次测试，作出的参数配置如下。当你选择virtio显卡中的3D模式时，sdl，spice输出端口不再有效，但仍会按你的上述选择作出以下配置。sdl将以-display sdl,gl=on输出（因系统的qemu源默认未编译sdl内容，所以选项未得到测试验证）。而spice则以wiki上的标准参数-display gtk,gl=on输出，但virtio显卡并不被识别。vnc除了spice上的参数外，我还加入了-vga qxl来兼容virtio显卡输出（我成功在图形界面中开启gl，但存在bug）。这个3D模式应该是在linux系统下加载，而非windows系统。

${YELLOW}系统镜像的磁盘驱动安装介绍：${RES}
	1)先创建一个新的磁盘镜像，用于搜索virtio驱动，参数如下
qemu-img create -f qcow2 fake.qcow2 1G
	2)挂载fake磁盘（处于virtio模式下），带有驱动的CD-ROM，运行原本的Windows客户机（boot磁盘依旧是处于IDE模式中），参数如下
qemu-system-x86_64 -m 4G -drive file=系统镜像,if=ide -drive file=fake.qcow2,if=virtio -cdrom virtio驱动.iso
	3)开机Windows会自动检测fake磁盘，并搜索适配的驱动。如果失败了，前往Device Manager，找到SCSI驱动器（带有感叹号图标，应处于打开状态），点击Update driver并选择虚拟的CD-ROM。不要定位到CD-ROM内的文件夹了，只选择CD-ROM设备就行，Windows会自动找到合适的驱动的。
	4)关机并重新启动它，现在可以以virtio模式挂载boot磁盘
qemu-system-x86_64 -m 4G -drive file=系统镜像,if=virtio

"
	CONFIRM
	VIRTIO
}
####################

YELLOW="\e[33m"
GREEN="\e[32m"
RED="\e[31m"
BLUE="\e[34m"
PINK="\e[35m"
WHITE="\e[37m"
RES="\e[0m"
####################
#IP=`ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | sed -n 2p`
	if [ $(ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | grep ^192 | cut -d '.' -f 1) 2>/dev/null == 192 ]; then
	IP=`ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | grep ^192`
	elif [ $(ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | grep ^172 | cut -d '.' -f 1) 2>/dev/null == 172 ]; then
	IP=`ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | grep ^172`
	else
	IP=`ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | grep ^10`
	fi
####################
sudo_() {
	date_t=`date +"%D"`
	if ! grep -q $date_t ".utqemu_log" 2>/dev/null; then
		$sudo apt update
		echo $date_t >>.utqemu_log 2>&1
	fi
}
	if [ `whoami` != "root" ];then
		sudo="sudo"
	fi
####################
BF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/debian/"
BF_URL="deb https://mirrors.tuna.tsinghua.edu.cn/debian"
US_URL="deb https://mirrors.ustc.edu.cn/debian"
DEB="main contrib non-free"
####################
case $(dpkg --print-architecture) in
	arm*|aarch64)
        LO_CPU=$(cat  /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq | tail -n 1)
        if [ $? = 0 ]; then
        if (( $LO_CPU <= 3000000 )); then
                LOW_CPU="2,cores=2,threads=1,sockets=1"
                LOW_CORE="2,cores=2"
        fi
        fi ;;
esac
####################	
MEM() {
	case $ARCH in
		tablet) mem=$(free -m | awk '{print $2/4}' | sed -n 2p | cut -d '.' -f 1) ;;
		*) mem=$(free -m | awk '{print $2/2}' | sed -n 2p | cut -d '.' -f 1) ;;
	esac
	if (( $mem >= 2048 )); then
		mem_=3072
	elif (( $mem >= 1536 )); then
		mem_=2048
	elif (( $mem >= 1024 )); then
		mem_=1536
	elif (( $mem >= 512 )); then
		mem_=1024
	else
		mem_=512
	fi
}
####################
INVALID_INPUT() {
	echo -e "\n${RED}重入无效，请重新输入${RES}" \\n
	sleep 1
}
#####################
CONFIRM() {
	read -r -p "按回车键继续" input
	unset input
}
####################
CHECK() {
	if [ $? == 1 ]; then
		echo -e "${RED}处理失败，请重试${RES}"
		sleep 2
	fi
}

ARCH_CHECK() {
	case $(dpkg --print-architecture) in
		arm*|aarch64) DIRECT="/sdcard"
			ARCH=tablet ;;
		i*86|x86*|amd64)
	if grep -E -q 'tablet|computer' ${HOME}/.utqemu_ 2>/dev/null; then
	case $(grep -E 'tablet|computer' ${HOME}/.utqemu_) in
		tablet) DIRECT="/sdcard"
			ARCH=tablet ;;
		computer) DIRECT="${HOME}"
			ARCH=computer ;;
		esac
	elif
	grep -E -q 'Z3560|Z5800|Z2580' "/proc/cpuinfo" 2>/dev/null; then
	read -r -p "请确认你使用的是否手机平板 1)是 2)否 " input
	case $input in
		1) echo "tablet" >>${HOME}/.utqemu_
			DIRECT="/sdcard"
			ARCH=tablet
			echo -e "${GREEN}已配置设备识别参数，请重新打开脚本，如发现选错，请在相关应用维护选项中修改${RES}"
        CONFIRM ;;
		2) echo "computer" >>${HOME}/.utqemu_
			DIRECT="${HOME}"
			ARCH=computer
			echo -e "${GREEN}已配置设备识别参数，请重新打开脚本，如发现选错，请在相关应用维护选项中修改${RES}"
        CONFIRM ;;
		*) INVALID_INPUT
			ARCH_CHECK ;;
	esac
	else
			DIRECT="${HOME}"
			ARCH=computer
			fi ;;
		*) echo -e "${RED}不支持你设备的架构${RES}" ;;
	esac
	if grep -q 'STORAGE' ${HOME}/.utqemu_ 2>/dev/null ; then
	source ${HOME}/.utqemu_ 2>/dev/null
	else
		STORAGE=/xinhao/windows/
	fi
}
####################
XVNC(){
	read -r -p "是否使用vncviewer密码 1)密码(建议) 2)免密 3)修改密码 :" input
	case $input in
		1) if [ ! -f ${HOME}/.vnc/passwd ]; then
			mkdir -p ${HOME}/.vnc
			vncpasswd
		fi
		PASS="-rfbauth ${HOME}/.vnc/passwd" ;;
		3) if [ ! -f ${HOME}/.vnc/passwd ]; then
			mkdir -p ${HOME}/.vnc
		fi
		vncpasswd
		PASS="-rfbauth ${HOME}/.vnc/passwd" ;;
		*)
		PASS="-SecurityTypes None" ;;
	esac
vncserver -kill $DISPLAY 2>/dev/null
pkill -9 Xtightvnc 2>/dev/null
pkill -9 Xtigertvnc 2>/dev/null
pkill -9 Xvnc 2>/dev/null
pkill -9 vncsession 2>/dev/null
#export PULSE_SERVER=tcp:127.0.0.1:4713
export DISPLAY=:0
Xvnc -ZlibLevel=1 -securitytypes vncauth,tlsvnc -verbose -ImprovedHextile -CompareFB 1 -br -retro -a 5 -wm -alwaysshared -geometry 768x1024 -once -depth 32 -deferglyphs 16 $PASS &
}
####################
QEMU_VERSION(){
	uname -a | grep 'Android' -q
        if [ $? == 0 ]; then
                SYS=ANDROID
		qemu_ver=$(qemu-system-x86_64 --version 2>/dev/null)
	elif [ ! $(command -v qemu-system-x86_64) ]; then
		echo ""
	elif qemu_ver=$(qemu-system-x86_64 --version)
		[[ $(echo $qemu_ver | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [5-9] ]]; then
		SYS=QEMU_ADV
	else
		SYS=QEMU_PRE
        fi
}
#################
HDA_READ() {
	while ( [ "$hda_name" != '0' ] && [ ! -f "${DIRECT}${STORAGE}$hda_name" ] )
	do
	if [ -n "$hda_name" ]; then
		echo -e "\n${RED}未匹配到镜像，请重试${RES}"
		sleep 1
	fi
	echo -n -e "${RES}\n请输入${YELLOW}系统镜像${RES}全名（不能有空格，例如andows.img），退出请输${YELLOW}0${RES}，请输入: "
	read  hda_name
	done
	if [ $hda_name == '0' ]; then
		QEMU_SYSTEM
	fi
}
#################
LIST() {
	case $DIRECT in
	\/sdcard)
		echo -e "\n${GREEN}请确认系统镜像已放入手机目录${STORAGE}里${RES}\n" ;;
	*) echo -e "\n${GREEN}请确认系统镜像已放入目录${STORAGE}里${RES}\n" ;;
	esac
	ls ${DIRECT}${STORAGE} | grep -E "\.blkdebug|\.blkverify|\.bochs|\.cloop|\.cow|\.tftp|\.ftps|\.ftp|\.https|\.http|\.dmg|\.nbd|\.parallels|\.qcow|\.qcow2|\.qed|\.host_cdrom|\.host_floppy|\.host_device|\.file|\.raw|\.sheepdog|\.vdi|\.vmdk|\.vpc|\.vvfat|\.img|\.XBZJ|\.vhd|\.iso|\.fd" >/dev/null 2>&1
	if [ $? == 1 ]; then
		echo -e "${GREEN}\n貌似没有符合格式的镜像，请以实际文件名为主${RES}"
		sleep 1
	else
	echo -e "已为你列出镜像文件夹中的常用镜像格式文件（仅供参考）\e[33m"
	LIST=`ls ${DIRECT}${STORAGE} | grep -E "\.blkdebug|\.blkverify|\.bochs|\.cloop|\.cow|\.tftp|\.ftps|\.ftp|\.https|\.http|\.dmg|\.nbd|\.parallels|\.qcow|\.qcow2|\.qed|\.host_cdrom|\.host_floppy|\.host_device|\.file|\.raw|\.sheepdog|\.vdi|\.vmdk|\.vpc|\.vvfat|\.img|\.XBZJ|\.vhd|\.iso|\.fd" | awk '{printf("%d) %s\n" ,NR,$0)}'`
#	LIST=`ls | awk '{printf("%d) %s\n" ,NR,$0)}'`
	echo -e "$LIST${RES}"
	echo -e "序号选项仅支持系统镜像，如果没列出请回车手输"
	read -r -p "请选择: " input
	hda_name=`echo "$LIST" | grep -w "${input})" | awk '{print $2}'`
	if [ ! -f "${DIRECT}${STORAGE}$hda_name" ]; then
	echo -e "\n${RED}选择有误，请手输镜像名${RES}"
	sleep 1
	else
	echo -e "\n$hda_name\n"
	fi
	fi
}
#################
LIST1(){
	echo -e "${YELLOW}"
	ls ${DIRECT}${STORAGE} | grep -E "\.blkdebug|\.blkverify|\.bochs|\.cloop|\.cow|\.tftp|\.ftps|\.ftp|\.https|\.http|\.dmg|\.nbd|\.parallels|\.qcow|\.qcow2|\.qed|\.host_cdrom|\.host_floppy|\.host_device|\.file|\.raw|\.sheepdog|\.vdi|\.vmdk|\.vpc|\.vvfat|\.img|\.XBZJ|\.vhd|\.iso|\.fd"
	echo -e "${RES}"
}
#################
FAIL() {
FILE="No such file"
SHARE_="516.06"
PORT="Address already"
CPU="CPU model"
GTK="gtk initialization"
SDL="SDL support is disabled"
SDL_="Could not initialize SDL"
echo -e "\n"
LOG=$(cat ${HOME}/.utqemu_log | tail -n 1)
	echo $LOG
	case $LOG in
	*$FILE*) echo -e "${YELLOW}错误：没有匹配的目录或文件名${RES}" ;;
	*$SHARE_*) echo -e "${YELLOW}错误：共享文件超过516.06 MB${RES}" ;;
	*$PORT*) echo -e "${YELLOW}\n错误：视频输出端口占用${RES}" ;;
	*$CPU*) echo -e "${YELLOW}\n错误：cpu名字有误${RES}" ;;
	*$GTK*) echo -e "${YELLOW}\n错误：图形输出错误${RES}" ;;
	*$SDL*) echo -e "${YELLOW}\n错误：未集成SDL模块${RES}" ;;
	*$SDL_*) echo -e "${YELLOW}\n错误：请先打开xsdl${RES}" ;;
	*)  ;;
esac
}
#################
LOGIN() {
	if [[ ! -e "$DEBIAN-qemu/root/.utqemu_" ]]; then
	echo $UPDATE >>$DEBIAN-qemu/root/.utqemu_
	elif ! grep -q $UPDATE "$DEBIAN-qemu/root/.utqemu_" ; then
	INFO
	echo -e "\n${GREEN}检测到脚本有更新，是否更新${RES}"
	read -r -p "1)更新 0)忽略并不再提示此版本 " input
	case $input in
		1|"") rm $DEBIAN-qemu/root/utqemu.sh 2>/dev/null
			curl https://shell.xb6868.com/ut/utqemu.sh -o $DEBIAN-qemu/root/utqemu.sh ;;
		*) ;;
	esac
	sed -i "/$(date +"%Y")/d" $DEBIAN-qemu/root/.utqemu_ && echo "$UPDATE" >>$DEBIAN-qemu/root/.utqemu_
	fi
if [ -e /linkerconfig/ld.config.txt ]; then
LDCONFIG="-b /linkerconfig/ld.config.txt"
fi
if [ -e /plat_property_contexts ]; then
PLAT_PROPERTY_CONTEXT="-b /plat_property_contexts"
fi
if [ -e /property_contexts ]; then
PROPERTY_CONTEXT="-b /property_contexts"
fi
if [ -e /apex ]; then
APEX="-b /apex"
fi
if [ -e /system_ext ]; then
SYSTEM_EXT="-b /system_ext"
fi
pkill -9 pulseaudio 2>/dev/null
pulseaudio --start --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" --exit-idle-time=-1 &
unset LD_PRELOAD
proot --kill-on-exit ${LDCONFIG} ${PLAT_PROPERTY_CONTEXT} ${PROPERTY_CONTEXT} ${APEX} ${SYSTEM_EXT} -b /vendor -b /system -b /sdcard -b /sdcard:/root/sdcard -b /data/data/com.termux/files -b /data/data/com.termux/cache -b /data/data/com.termux/files/usr/tmp:/tmp -b /dev/null:/proc/sys/kernel/cap_last_cap -b $DEBIAN-qemu/etc/proc/version:/proc/version -b $DEBIAN-qemu/etc/proc/misc:/proc/misc -b $DEBIAN-qemu/etc/proc/buddyinfo:/proc/buddyinfo -b $DEBIAN-qemu/etc/proc/kmsg:/proc/kmsg -b $DEBIAN-qemu/etc/proc/consoles:/proc/consoles -b $DEBIAN-qemu/etc/proc/execdomains:/proc/execdomains -b $DEBIAN-qemu/etc/proc/stat:/proc/stat -b $DEBIAN-qemu/etc/proc/fb:/proc/fb -b $DEBIAN-qemu/etc/proc/loadavg:/proc/loadavg -b $DEBIAN-qemu/etc/proc/key-users:/proc/key-users -b $DEBIAN-qemu/etc/proc/uptime:/proc/uptime -b $DEBIAN-qemu/etc/proc/devices:/proc/devices -b $DEBIAN-qemu/etc/proc/vmstat:/proc/vmstat -b /data/dalvik-cache -b $DEBIAN-qemu/tmp:/dev/shm -b /proc/self/fd/2:/dev/stderr -b /proc/self/fd/1:/dev/stdout -b /proc/self/fd/0:/dev/stdin -b /proc/self/fd:/dev/fd -b /dev/urandom:/dev/random --sysvipc --link2symlink -S $DEBIAN-qemu -w /root /usr/bin/env -i HOME=/root PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games LANG=zh_CN.UTF-8 TZ=Asia/Shanghai TERM=xterm-256color USER=root /bin/bash --login

}
##################
SYS_DOWN() {
	echo -e "${YELLOW}即将下载系统(约占500m空间)${RES}"
	sleep 2
case $(dpkg --print-architecture) in
	arm64|aarch*)
                DEF_CUR="${BF_CUR}${DEBIAN}/arm64/default/" ;;
	x86_64|amd64)
		DEF_CUR="${BF_CUR}${DEBIAN}/amd64/default/" ;;
	i*86|x86)
		DEF_CUR="${BF_CUR}${DEBIAN}/i386/default/" ;;
	armv7*|armv8l)
		DEF_CUR="${BF_CUR}${DEBIAN}/armhf/default/" ;;
	armv6*|armv5*)
		DEF_CUR="${BF_CUR}${DEBIAN}/armel/default/" ;;
		esac
		BAGNAME="rootfs.tar.xz"
		curl -o ${BAGNAME} ${DEF_CUR}$(curl ${DEF_CUR}| grep href | tail -n 2 | cut -d '"' -f 4 | head -n 1)${BAGNAME}
        if [ $? -ne 0 ]; then
		echo -e "${RED}下载失败，请重输${RES}\n" && MAIN
        fi
        if [ -e $sys_name ]; then
		rm -rf $sys_name
        fi
                mkdir $sys_name
	echo -e "${BLUE}正在解压系统包${RES}"
	tar xf ${BAGNAME} --checkpoint=200 --checkpoint-action=dot --totals -C $sys_name 2>/dev/null
        rm ${BAGNAME}
	echo -e "${BLUE}$sys_name系统已下载，文件夹名为$sys_name${RES}"
        echo "127.0.0.1 localhost" > $sys_name/etc/hosts
        rm -rf $sys_name/etc/resolv.conf &&
        echo "nameserver 223.5.5.5
nameserver 223.6.6.6" >$sys_name/etc/resolv.conf
        echo "export  TZ='Asia/Shanghai'" >> $sys_name/root/.bashrc
	echo "${US_URL} sid ${DEB}"|sed 's/https/http/' >$sys_name/etc/apt/sources.list
curl https://mirrors.tuna.tsinghua.edu.cn/debian/pool/main/c/ca-certificates/$(curl https://mirrors.tuna.tsinghua.edu.cn/debian/pool/main/c/ca-certificates/|grep all.deb|awk -F 'href="' '{print $2}'|cut -d '"' -f 1|tail -n 1) -o $sys_name/root/ca.deb
curl https://mirrors.tuna.tsinghua.edu.cn/debian/pool/main/o/openssl/$(curl https://mirrors.tuna.tsinghua.edu.cn/debian/pool/main/o/openssl/|grep openssl_3.*arm64.deb|awk -F 'href="' '{print $2}'|cut -d '"' -f 1|tail -n 1) -o $sys_name/root/openssl.deb
	if [ ! -f $(pwd)/utqemu.sh ]; then
	curl https://shell.xb6868.com/ut/utqemu.sh -o $sys_name/root/utqemu.sh 2>/dev/null
	else
		cp utqemu.sh $sys_name/root/
	fi
	
echo 'for i in /var/run/dbus/pid /tmp/.X*-lock /tmp/.X11-unix/X* /tnp/wayland*; do if [ -e "${i}" ]; then rm -vf ${i}; fi; done' >>$sys_name/etc/profile

echo -e "\e[33m优化部分命令\e[0m"
sleep 1
#伪proc文件
mkdir $sys_name/etc/proc/ -p
printf ' 52 memory_bandwidth! 53 network_throughput! 54 network_latency! 55 cpu_dma_latency! 56 xt_qtaguid! 57 vndbinder! 58 hwbinder! 59 binder! 60 ashmem!239 uhid!236 device-mapper!223 uinput!  1 psaux!200 tun!237 loop-control! 61 lightnvm!228 hpet!229 fuse!242 rfkill! 62 ion! 63 vga_arbiter\n' | sed 's/!/\n/g' >$sys_name/etc/proc/misc
printf "%-1s %-1s %-1s %8s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s %6s\n" Node 0, zone DMA 3 2 2 4 3 3 2 1 2 2 0 Node 0, zone DMA32 1774 851 511 220 67 3 2 0 0 1 0 >$sys_name/etc/proc/buddyinfo

echo "0.03 0.03 0.00 1/116 17521" >$sys_name/etc/proc/loadavg
touch $sys_name/etc/proc/kmsg
echo 'tty0                 -WU (EC p  )    4:7' >$sys_name/etc/proc/consoles
echo '0-0     Linux                   [kernel]' >$sys_name/etc/proc/execdomains
echo '0 EFI VGA' >$sys_name/etc/proc/fb
echo '    0:     9 8/8 3/1000000 27/25000000' >$sys_name/etc/proc/key-users
echo '285490.46 1021963.95' >$sys_name/etc/proc/uptime
echo $(uname -a) | sed 's/Android/GNU\/Linux/' >$sys_name/etc/proc/version
touch $sys_name/etc/proc/vmstat
echo 'Character devices:!  1 mem!  4 /dev/vc/0!  4 tty!  4 ttyS!  5 /dev/tty!  5 /dev/console!  5 /dev/ptmx!  7 vcs! 10 misc! 13 input! 21 sg! 29 fb! 81 video4linux!128 ptm!136 pts!180 usb!189 usb_device!202 cpu/msr!203 cpu/cpuid!212 DVB!244 hidraw!245 rpmb!246 usbmon!247 nvme!248 watchdog!249 ptp!250 pps!251 media!252 rtc!253 dax!254 gpiochip!!Block devices:!  1 ramdisk!  7 loop!  8 sd! 11 sr! 65 sd! 66 sd! 67 sd! 68 sd! 69 sd! 70 sd! 71 sd!128 sd!129 sd!130 sd!131 sd!132 sd!133 sd!134 sd!135 sd!179 mmc!253 device-mapper!254 virtblk!259 blkext' | sed 's/!/\n/g' >$sys_name/etc/proc/devices
echo "cpu  0 0 0 0 0 0 0 0 0 0
cpu0 0 0 0 0 0 0 0 0 0 0
intr 1
ctxt 0
btime 0
processes 0
procs_running 1
procs_blocked 0
softirq 0 0 0 0 0 0 0 0 0 0 0" >$sys_name/etc/proc/stat
cpus=`cat -n /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq | tail -n 1 | awk '{print $1}'`
if [ -n $cpus ]; then
	while [[ $cpus -ne 1 ]]
	do
		cpus=$(( $cpus-1 ))
		sed -i "2a cpu${cpus} 0 0 0 0 0 0 0 0 0 0" $sys_name/etc/proc/stat
	done
fi

if [ -z $ANDROID_RUNTIME_ROOT ]; then
export ANDROID_RUNTIME_ROOT=/apex/com.android.runtime
fi
cat >>$sys_name/etc/profile<<-EOF
#THIS IS QEMU CONTAINER
export ANDROID_ART_ROOT=${ANDROID_ART_ROOT-}
export ANDROID_DATA=${ANDROID_DATA-}
export ANDROID_I18N_ROOT=${ANDROID_I18N_ROOT-}
export ANDROID_ROOT=${ANDROID_ROOT-}
export ANDROID_RUNTIME_ROOT=${ANDROID_RUNTIME_ROOT-}
export ANDROID_TZDATA_ROOT=${ANDROID_TZDATA_ROOT-}
export BOOTCLASSPATH=${BOOTCLASSPATH-}
export COLORTERM=${COLORTERM-}
export DEX2OATBOOTCLASSPATH=${DEX2OATBOOTCLASSPATH-}
export EXTERNAL_STORAGE=${EXTERNAL_STORAGE-}
export PATH=\${PATH}:/data/data/com.termux/files/usr/bin:/system/bin:/system/xbin
export PREFIX=${PREFIX-/data/data/com.termux/files/usr}
export TERM=${TERM-xterm-256color}
export TMPDIR=/tmp
export PULSE_SERVER=tcp:127.0.0.1:4713
EOF
	echo "bash utqemu.sh" >>$sys_name/root/.bashrc
	echo "$UPDATE" >>$sys_name/root/.utqemu_
	echo -e "${YELLOW}系统已下载，请登录系统继续完成qemu的安装${RES}"
	sleep 2
}
##################

#####################
#####################
SYSTEM_CHECK() {
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	if [ ! -e ${HOME}/storage ]; then
		termux-setup-storage
	fi
	for i in curl pulseaudio proot; do if [ ! $(command -v $i) ]; then pkg i $i -y; fi done
	if [ ! $(command -v ip) ]; then pkg i iproute2 -y; fi
	fi
}
##################
WEB_SERVER() {
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	if [ ! $(command -v python) ]; then
	echo -e "\n检测到你未安装所需要的包python,将先为你安装上"
	pkg i python -y
	fi
	else
	if [ ! $(command -v python3) ]; then
        echo -e "\n检测到你未安装所需要的包python,将先为你安装上"
	sleep 2
        sudo_
	$sudo apt install python3 python3-pip -y && mkdir -p /root/.config/pip && echo "[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple" >/root/.config/pip/pip.conf
        fi
	fi
        echo -e "已完成配置，请尝试用浏览器打开并输入地址\n
	${YELLOW}本机	http://127.0.0.1:8080
        局域网	http://$IP:8080${RES}\n
        如需关闭，请按ctrl+c，然后输pkill python3或直接exit退出shell\n"
        python3 -m http.server 8080 &
        sleep 2
}

##################
QEMU_ETC() {

echo -e "
1)  创建空磁盘(目前支持qcow2,vmdk)
2)  转换镜像磁盘格式(仅支持qcow2,vmdk,其他格式未验证)
3)  修改设备标识(手机、平板、电脑)
4)  修改源(只适用本脚本下载的系统)
5)  安装aqemu(适用于图形界面中操作的图形前端)
6)  获取最新版termux、aspice与xsdl的安卓版下载地址(非永久有效)
7)  模拟系统的时间不准
8)  修改镜像目录
10) 返回
0)  退出\n"
	read -r -p "请选择: " input
	case $input in
		1) if [ ! $(command -v qemu-img) ]; then
			sudo_
		       	$sudo apt install qemu-img
		fi
	echo -e "\n对于普通玩家这两个格式没什么区别，不用纠结"
	read -r -p "请选择格式 1)qcow2 2)vmdk : " input
	case $input in
		1|"") echo -e "${YELLOW}qcow2${RES}"
			FORMAT=qcow2	;;
		2) echo -e "${YELLOW}vmdk${RES}"
			FORMAT=vmdk ;;
		*) INVALID_INPUT
			QEMU_ETC ;;
	esac
	sleep 1
	while [ ! -n "$disk_name" ]
	do
	echo -e -n "\n请为磁盘起个名字(不能为空): "
	read disk_name
	done
	echo -n "请输入你拟创建的磁盘容量，以G为单位(例如4g则输4): "
	read capacity
	qemu-img create -f $FORMAT ${DIRECT}${STORAGE}${disk_name}.$FORMAT ${capacity}G
	if [ -f ${DIRECT}${STORAGE}${disk_name}.$FORMAT ]; then
	echo -e "${GREEN}已为你创建$FORMAT格式磁盘${disk_name}.$FORMAT 容量${capacity}G，仍需你登录系统，在控制面板通过磁盘管理进行格式化并分区方可正常使用${RES}"
	else
	echo -e "${RED}创建失败，请重试${RES}"
	fi
	CONFIRM ;;
	2) if [ ! $(command -v qemu-img) ]; then
	sudo_
       	$sudo apt install qemu-img
	fi
	echo ""
	read -r -p "请选择转换后格式 1)qcow2 2)vmdk : " input
	case $input in
	1) echo -e "转换为${YELLOW}qcow2${RES}格式"
		FORMAT=qcow2 ;;
	2) echo -e "转换为${YELLOW}vmdk${RES}格式"
		FORMAT=vmdk ;;
	*) INVALID_INPUT
		QEMU_ETC ;;
	esac
	echo -e "\n已为你列出镜像文件夹中的文件（仅供参考）\n"
	ls ${DIRECT}${STORAGE}
	sleep 1
	while ( [ "$FORMAT_" != '0' ] && [ ! -f "${DIRECT}${STORAGE}$FORMAT_" ] ) 
	do
	if [ -n "$FORMAT_" ]; then
	echo -e "\n${RED}未匹配到镜像，请重试${RES}"
	sleep 1
	fi
	echo -en "\n请输入原镜像格式全名(例如andows.img) ,退出请输${YELLOW}0${RES} "
		read  FORMAT_
	done
	if [ $FORMAT_ == '0' ]; then
		exit 0
	fi
	if [ -f ${DIRECT}${STORAGE}${FORMAT_%%.*}.$FORMAT ]; then
	echo -e "\n${RED}检测到目录下已有转换后同名文件名，请确认，以免造成误操作${RES}"
	read -r -p "1)继续 9)返回 0)退出 " input
	case $input in
		1) ;;
		0) exit 0 ;;
		*) unset FORMAT_ FORMAT
			QEMU_ETC ;;
	esac
	fi
	echo -e "\e[33m转换过程需要点时间，请耐心等待...${RES}"
	case "${FORMAT_##*.}" in
		img) qemu-img convert -f raw -O $FORMAT ${DIRECT}${STORAGE}$FORMAT_ ${DIRECT}${STORAGE}${FORMAT_%%.*}.$FORMAT ;;
		*) qemu-img convert -f "${FORMAT_##*.}" ${DIRECT}${STORAGE}$FORMAT_ -O $FORMAT ${DIRECT}${STORAGE}${FORMAT_%%.*}.$FORMAT ;;
	esac
	if [ -f ${DIRECT}${STORAGE}${FORMAT_%%.*}.$FORMAT ]; then
		echo -e "\n${GREEN}已转换，${FORMAT_%%.*}.$FORMAT${RES}\n"
	else
		echo -e "\n${RED}转换失败${RES}\n"
	fi
		sleep 1 ;;
	3) read -r -p "1)手机平板 2)电脑 " input
	case $input in
		1) sed -i '/computer/d' ${HOME}/.utqemu_ 2>/dev/null
		echo "tablet" >>${HOME}/.utqemu_
	      	echo -e "${GREEN}已修改，请重新登录脚本${RES}" ;;
		2) sed -i '/tablet/d' ${HOME}/.utqemu_
		echo "computer" >>${HOME}/.utqemu_
	       	echo -e "${GREEN}已修改，请重新登录脚本${RES}" ;;
		*) INVALID_INPUT
		sleep 2
		QEMU_ETC ;;
	esac
	sleep 2
	exit 0 ;;
	4) if ! grep -E -q 'buster|bullseye|sid' "/etc/os-release"; then
	echo -e "\n${RED}只支持bullseye，sid与buster${RES}\n"
	sleep 2
	QEMU_ETC
	else
	read -r -p "1)中科源 2)北外源 9)返回主目录 0)退出 " input
	case $input in
		1) if grep -q 'sid' /etc/apt/sources.list ;then
		echo "${US_URL} sid ${DEB}" >/etc/apt/sources.list
		elif grep -q 'bullseye' /etc/os-release ;then
echo "${US_URL}/ bullseye ${DEB}
${US_URL}/ bullseye-updates ${DEB}
${US_URL}/ bullseye-backports ${DEB}
${US_URL}-security bullseye-security ${DEB}" >/etc/apt/sources.list
		elif grep -q 'buster' /etc/os-release ;then
echo "${US_URL} stable ${DEB}
${US_URL} stable-updates ${DEB}" >/etc/apt/sources.list
		fi
	       	$sudo apt update ;;
		2) if grep -q 'sid' /etc/apt/sources.list ;then
		echo "$BF_URL sid ${DEB}" >/etc/apt/sources.list
		elif grep -q 'bullseye' /etc/os-release ;then
echo "${BF_URL}/ bullseye ${DEB}
${BF_URL}/ bullseye-updates ${DEB}
${BF_URL}/ bullseye-backports ${DEB}
${BF_URL}-security bullseye-security ${DEB}" >/etc/apt/sources.list
	elif grep -q 'buster' /etc/os-release ;then
echo "$BF_URL buster ${DEB}
${BF_URL} buster-updates ${DEB}
${BF_URL} buster-backports ${DEB}
${BF_URL}-security buster/updates ${DEB}" >/etc/apt/sources.list
	fi
       	$sudo apt update ;;
		9) QEMU_SYSTEM ;;
		0) exit 0 ;;
		*) INVALID_INPUT && QEMU_ETC ;;
	esac
	fi ;;
	5) echo -e "${GREEN}aqemu是qemu的前端，适用于图形界面下简易配置操作qemu，安装完aqemu，首次启动时请搜索并绑定qemu-system-x86_64${RES}"
	CONFIRM
	sudo_
       	$sudo apt install aqemu -y
	if [ ! $(command -v aqemu ) ]; then
		echo -e "${RED}安装失败，请重试${RES}"
		sleep 1
		fi
	QEMU_ETC
		;;
	6) read -r -p "1)termux 2)aspice 3)xsdl 4)termux-api 5)avnc(操控是触点方式，非移动光标) " input
	case $input in
	1) echo -e "\n${YELLOW}检测最新版本${RES}"
	VERSION=`curl https://f-droid.org/packages/com.termux/ | grep apk | sed -n 2p | cut -d '_' -f 2 | cut -d '"' -f 1`
	if [ ! -z "$VERSION" ]; then
	echo -e "\n下载地址\n${GREEN}https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/com.termux_$VERSION${RES}\n"
	else 
	echo -e "${RED}获取错误，请重试${RES}"
	sleep 2
	unset VERSION
	QEMU_ETC
	fi
	read -r -p "1)下载 9)返回 " input
	case $input in
		1) rm termux.apk 2>/dev/null
	curl https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/com.termux_$VERSION -o termux.apk
	mv -v termux.apk ${DIRECT}${STORAGE}
	echo -e "\n已下载至${DIRECT}${STORAGE}目录"
	sleep 2 ;;
	*) ;;
	esac
	unset VERSION
	QEMU_ETC
		;;
	2)
	echo -e "\n${YELLOW}下载的地址来自spice的作者最新版，由于Github速度非常有限，所以这边只提供下载地址，请复制到其他方式下载，如获取失败，请重试${RES}\n"
	CONFIRM
	while ( [ "$SPI_URL" != '0' ] && [ -z $SPI_URL_ ] )
do
#	SPI_URL=`curl --connect-timeout 5 -m 8 -s https://github.com/iiordanov/remote-desktop-clients | grep tag\/ | cut -d '"' -f 6 | cut -d '/' -f 6`
	SPI_URL=`curl --connect-timeout 5 -m 8 -s https://kgithub.com/iiordanov/remote-desktop-clients | grep tag\/ | awk -F 'href="' '{print $2}'|awk -F '/' '{print $NF}'|cut -d '"' -f 1`
#SPI_URL_=`curl --connect-timeout 5 -m 8 https://kgithub.com/iiordanov/remote-desktop-clients/releases/tag/$SPI_URL | grep SPICE | grep apk | tail -n 1 | cut -d '>' -f 2 | cut -d '<' -f 1`
	while [ -z $SPI_URL_ ]; do
	i=0
	SPI_URL_=`curl -LI https://kgithub.com/iiordanov/remote-desktop-clients/releases/download/$SPI_URL/freeaSPICE-${SPI_URL}_$i-final.apk|grep -i length|awk '{print $2}'`
	if [ -z $SPI_URL_ ]; then
	read -r -p "获取失败，重试请回车，退出请输0 " input
	case $input in
	0) QEMU_ETC ;;
	*) ;;
	esac
	fi
done
	done
#	echo -e "\n下载地址\n${GREEN}https://github.com/iiordanov/remote-desktop-clients/releases/download/$SPI_URL/$SPI_URL_${RES}\n"
	echo -e "\n下载地址\n${GREEN}https://kgithub.com/iiordanov/remote-desktop-clients/releases/download/$SPI_URL/freeaSPICE-v5.1.2_$i-final.apk${RES}\n"
	CONFIRM ;;
	3) VERSION=`curl https://sourceforge.net/projects/libsdl-android/files/apk/XServer-XSDL/ | grep android | grep 'XSDL/XServer' | grep '\.apk/download' | head -n 1 | cut -d '/' -f 9`
	echo -e "\n下载地址\n${GREEN}https://jaist.dl.sourceforge.net/project/libsdl-android/apk/XServer-XSDL/$VERSION${RES}\n"
	read -r -p "1)下载 9)返回 " input
	case $input in
	1) 
	curl -O https://jaist.dl.sourceforge.net/project/libsdl-android/apk/XServer-XSDL/$VERSION
	if [ -f $VERSION ]; then
	echo -e "移到${DIRECT}${STORAGE}目录中..."
	mv -v $VERSION ${DIRECT}${STORAGE}
	if [ -f ${DIRECT}${STORAGE}$VERSION ]; then
	echo -e "\n已下载至${DIRECT}${STORAGE}目录"
	sleep 2
	fi
	else
	echo -e "\n${RED}错误，请重试${RES}"
	sleep 2
	fi ;;
	*) ;;
	esac
	unset VERSION ;;
	4) curl https://f-droid.org/packages/com.termux.api/ | grep apk | sed -n 2p | cut -d '"' -f 2 | cut -d '"' -f 1 | xargs curl -o ${DIRECT}${STORAGE}/com.termux.api.apk
	if [ -f ${DIRECT}${STORAGE}/com.termux.api.apk ]; then
	echo -e "\n已下载至${DIRECT}${STORAGE}目录"
	else
	echo -e "\n${RED}错误，请重试${RES}"
	fi
	sleep 2 ;;
	5) echo -e "\n${YELLOW}检测最新版本${RES}"
	VERSION=`curl https://f-droid.org/zh_Hant/packages/com.gaurav.avnc/ | awk -F 'repo/' '{print $2}' | grep apk | cut -d '"' -f 1 | sed -n 1p`
	if [ ! -z "$VERSION" ]; then
		echo -e "\n下载地址\n${GREEN}https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/$VERSION${RES}\n"
	else
		echo -e "${RED}获取错误，请重试${RES}"
		sleep 2
		unset VERSION
		QEMU_ETC
	fi
		read -r -p "1)下载 9)返回 " input
		case $input in
		1) rm avnc.apk 2>/dev/null
		curl https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/$VERSION -o avnc.apk
		mv -v avnc.apk ${DIRECT}${STORAGE}
		echo -e "\n已下载至${DIRECT}${STORAGE}目录"
		echo -e "${YELLOW}像手机操作设置：右上角设置/ Input/1 FingerSwipe选Scroll Remote Content${RES}"
		sleep 2 ;;
	*) ;;
        esac
	unset VERSION
	QEMU_ETC
	;;

	*) INVALID_INPUT ;;
	esac
	QEMU_ETC ;;
	7) echo -e "\n通常情况下，参数rtc可以解决，但可能由于容器时区问题导致，可通过修改时区来解决\n"
	read -r -p "1)修改时区 9)返回 " input
	case $input in
		1)
		sed -i "/^export TZ=/d" /etc/profile
		sed -i "1i\export TZ='Asia/Shanghai'" /etc/profile
		echo -e "\n${GREEN}请退出容器返回termux主界面，再重新进入${RES}\n"
		sleep 2 ;;
		*) ;;
	esac
	QEMU_ETC ;;
	8) echo -e "\n目前仅支持镜像目录，共享目录暂不支持，此操作并不能修改本脚本参数，会创建一个名为${YELLOW}.utqemu_${RES}的文件，如删除文件则指定目录将失效\n"
	CONFIRM
	echo -e "请选择目录路径\n1)本地目录\n2)手机平板目录\n9)返回 "
	read -r -p "请选择: " input
	case $input in
	1) echo -e "本地目录，输'目录名'(例如${YELLOW}windows${RES}，则本地目录生成一个windows文件夹) "
	read path_
	sed -i "/STORAGE/d" ${HOME}/.utqemu_ 2>/dev/null
	sed -i "/DIRECT/d" ${HOME}/.utqemu_ 2>/dev/null
	echo 'DIRECT=${HOME}' >>${HOME}/.utqemu_
	echo "STORAGE=/$path_/" >>${HOME}/.utqemu_
	DIRECT="${HOME}"
	if [ ! -e "${DIRECT}/${path_}" ]; then
	mkdir -p ${DIRECT}/${path_} 2>/dev/null
	fi ;;
	2) echo -e "手机平板目录则'目录名'(例如${YELLOW}windows${RES}，则手机平板目录生成windows) "
	read path_
	sed -i "/STORAGE/d" ${HOME}/.utqemu_ 2>/dev/null
        sed -i "/DIRECT/d" ${HOME}/.utqemu_ 2>/dev/null
	echo "STORAGE=/$path_/" >>${HOME}/.utqemu_
	DIRECT=/sdcard
	if [ ! -e "${DIRECT}/${path_}" ]; then
	mkdir -p ${DIRECT}/${path_} 2>/dev/null
	fi
		;;
	*) QEMU_ETC ;;
	esac
	if ! grep -q 'STORAGE' ${HOME}/.utqemu_ 2>/dev/null ; then
		echo -e "\n${RED}创建失败，请重试${RES}\n"
	else
		echo -e "\n${GREEN}创建成功，新的镜像目录为${DIRECT}/${path_}，请重新登录脚本\n${RES}"
		sleep 2
	fi
	exit 0
	QEMU_ETC ;;
	10) unset FORMAT_ FORMAT
		QEMU_SYSTEM ;;
	0) exit 0 ;;
	*) INVALID_INPUT && QEMU_ETC ;;
	esac
	unset FORMAT_ FORMAT
	QEMU_ETC
}
##################
PA() {
	if [ -e "/root/sd" ]; then
	ln  -s /root/sd /sdcard
	fi
	echo -e "创建windows镜像目录及共享目录\n"
	if [ ! -e "${DIRECT}${STORAGE}" ]; then
		mkdir -p ${DIRECT}${STORAGE}
	fi
	if [ ! -e "${DIRECT}/xinhao/share/" ]; then
		mkdir -p ${DIRECT}/xinhao/share
	fi
	if [ ! -d "${HOME}/share" ]; then
		mkdir ${HOME}/share
	chmod 755 ${HOME}/share
	fi
	if [ ! -e "${DIRECT}${STORAGE}" ]; then
		echo -e "${RED}创建目录失败${RES}"
	else
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
		echo -e "${GREEN}手机根目录下已创建/xinhao/windows文件夹，请把系统镜像，分驱镜像，光盘放进这个目录里\n\n共享目录是/xinhao/share(目录内总文件大小不能超过500m)${RES}\n"
	else
	case $ARCH in
	computer) echo -e "${GREEN}主目录下已创建/xinhao/windows文件夹，请把系统镜像，分驱镜像，光盘放进这个目录里\n\n共享目录是/xinhao/share(目录内总文件大小不能超过500m)\n\n本地共享目录是本系统主目录下的share(容量不受限制，可随意修改)${RES}" ;;
	*) 
	echo -e "${GREEN}手机目录下已创建/xinhao/windows文件夹，请把系统镜像，分驱镜像，光盘放进这个目录里\n\n共享目录是/xinhao/share(目录内总文件大小不能超过500m)\n本地共享目录是本系统主目录下的share(容量不受限制，可随意修改)${RES}" ;;
	esac
	fi
	fi
}
##################
MOVE_IN() {
	unset FILE_NAME
	echo -e "\n已为你列出${DIRECT}${STORAGE}目录下的文件"
	ls ${DIRECT}${STORAGE}
	echo -n -e "${RES}\n请输入要拷贝的${YELLOW}文件名${RES}全名，请输入: "
	read FILE_NAME
	if [ -z $FILE_NAME ]; then
	echo -e "\n${RED}无此文件${RES}"
	sleep 1
		QEMU_SYSTEM
	elif [ ! -e ${DIRECT}${STORAGE}$FILE_NAME ]; then
	echo -e "\n${RED}无此文件${RES}"
	sleep 1
	QEMU_SYSTEM
	fi
	cp -v ${DIRECT}${STORAGE}$FILE_NAME ${HOME}/share -r
	echo -e "${YELLOW}done..${RES}"
	unset FILE_NAME
	sleep 2
	QEMU_SYSTEM
}

MOVE_OUT() {
	unset FILE_NAME
	echo -e "\n已为你列出本地共享目录下的文件"
	ls ${HOME}/share
	echo -n -e "${RES}\n请输入要拷贝的${YELLOW}文件名${RES}全名，请输入: "
	read FILE_NAME
	if [ -z $FILE_NAME ]; then
	echo -e "\n${RED}无此文件${RES}"
	sleep 1
	QEMU_SYSTEM
	elif [ ! -e "${HOME}/share/$FILE_NAME" ]; then
	echo -e "\n${RED}无此文件${RES}"
	sleep 1
	QEMU_SYSTEM
	fi
	cp -v ${HOME}/share/$FILE_NAME ${DIRECT}${STORAGE} -r
	echo -e "${YELLOW}done..${RES}"
	unset FILE_NAME
	sleep 2
	QEMU_SYSTEM
}


##################
QEMU_SYSTEM() {
	if [ ! -f "/usr/bin/perl" ]; then
	ln -sv /usr/bin/perl*aarch64* /usr/bin/perl
#	ln -sv /usr/bin/perl* /usr/bin/perl
	fi
	if [ ! $(command -v curl) ]; then
		sudo_
		$sudo apt install curl -y
	fi
	uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	if ! grep -q https /etc/apt/sources.list; then
		if [ -f openssl.deb ]; then
		$sudo	dpkg -i openssl.deb
		$sudo	dpkg -i ca.deb
		else
		$sudo apt install apt-transport-https ca-certificates -y
		fi
		sed -i "s/http/https/g" /etc/apt/sources.list && $sudo apt update
	fi
	if grep -q 'THIS IS QEMU CONTAINER' /etc/profile; then
	if [ ! $(command -v busybox) ]; then
	$sudo apt install busybox -y
	for i in ps uptime killall grep -E top; do if [ $(command -v $i) ]; then ln -svf $(command -v busybox) $(command -v $i); else ln -svf $(command -v busybox) /usr/bin/$i; fi done
	fi
	fi
	fi
	unset hda_name display hdb_name iso_name iso1_name SOUND_MODEL VGA_MODEL CPU_MODEL NET_MODEL SMP URL script_name QEMU_MODE NET_MODEL0 NET_MODEL1 NUM
	HMAT=",hmat=off"
	QEMU_VERSION
	NOTE
	if [ ! $(command -v qemu-system-x86_64) ]; then
echo -e "
1)  \e[32m安装qemu-system-x86_64，并联动更新模拟器所需应用\n(由于qemu的依赖问题，安装过程可能会失败，请尝试重新安装)${RES}"
else
echo -e "
1)  安装qemu-system-x86_64，并联动更新模拟器所需应用\n(由于qemu的依赖问题，安装过程可能会失败，请尝试重新安装)${RES}"
	fi
case $ARCH in
	computer) echo -e "2)  创建windows镜像目录" ;;
	*)
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
		echo -e "2)  创建windows镜像目录(已执行选项1可跳过)"
else
	echo -e "2)  创建windows镜像目录及本地共享文件目录share拷贝(已执行选项1可跳过)"
	fi ;;
esac
if [ ! $(command -v qemu-system-x86_64) ]; then
echo -e "3)  启动qemu-system-x86_64模拟器"
else
echo -e "3)  \e[32m启动qemu-system-x86_64模拟器\e[0m"
fi
echo -e "4)  让termux成为网页服务器(使模拟系统可以通过浏览器访问本机内容)
5)  virtio驱动相关"
	case $SYS in
	ANDROID) ;;
	*) echo -e "6)  应用维护" ;;
	esac
echo -e "7)  查看日志
8)  更新内容${YELLOW}${UPDATE}${RES}
9)  关于utqemu
10) 在线termux-toolx脚本体验维护linux系统(debian)
11) 在线测试本机cpu支持模拟的特性"
	uname -a | grep 'Android' -q
	if [ $? == 1 ]; then
echo "12) 体验dos环境(可运行游戏，需自行下载)"
	fi
echo -e "0)  退出\n"

	read -r -p "请选择: " input
	case $input in
	1) 
		unset LANG
	echo -e "${YELLOW}安装过程中，如遇到询问选择，请输(y)，安装过程容易出错，请重试安装${RES}"
	sleep 2
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	pkg update -y && apt --fix-broken install -y && apt install qemu-system-x86-64-headless qemu-system-i386-headless curl -y
	if [ ! $(command -v qemu-system-x86_64) ]; then
	echo -e "\n检测安装失败，重新安装\n"
	sleep 1
	apt --fix-broken install -y && apt install qemu-system-x86-64-headless qemu-system-i386-headless curl -y
	fi
	else
	sudo_
       	$sudo apt install qemu-system-x86 xserver-xorg x11-utils pulseaudio curl fonts-wqy-microhei -y
	if [ ! $(command -v qemu-system-x86_64) ]; then
	echo -e "\n检测安装失败，重新安装\n"
	sleep 1
	$sudo apt install qemu-system-x86 xserver-xorg x11-utils pulseaudio curl fonts-wqy-microhei -y
	fi
	sed -i "/zh_CN.UTF/s/#//" /etc/locale.gen
	locale-gen
	export LANG=zh_CN.UTF-8
	PA
	echo -e "\n${GREEN}已完成安装，如无法正常使用，请重新执行此操作${RES}"
	fi
	CONFIRM
        QEMU_SYSTEM
        ;;
	2)
	case $ARCH in
	computer) PA
		CONFIRM
		QEMU_SYSTEM ;;
	*)
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	PA
        CONFIRM
        QEMU_SYSTEM
	else
	echo -e "
1) 创建镜像目录
2) 拷贝${DIRECT}${STORAGE}文件进本地共享目录share
3) 拷贝本地共享目录share里的文件到手机存储目录${DIRECT}${STORAGE}"
	read -r -p "请选择: " input
	case $input in
	1) PA
	CONFIRM ;;
	2) MOVE_IN ;;
	3) MOVE_OUT ;;
	*) INVALID_INPUT
		CONFIRM ;;
	esac
	QEMU_SYSTEM
	fi ;;
	esac

        ;;
	3) START_QEMU ;;
	4) WEB_SERVER ;;
	5) VIRTIO ;;
	6) case $SYS in
		ANDROID) INVALID_INPUT
		QEMU_SYSTEM ;;
		*) QEMU_ETC ;;
	esac ;;
	7) if [ -e ${HOME}/.utqemu_log ]; then
	echo -e "\n${GREEN}日志已忽略不重要的信息${RES}\n按空格下一页，退出请按q\n"
	CONFIRM
	more ${HOME}/.utqemu_log | grep -E "qemu-system-x86_64|qemu-system-i386|initialization" | grep -E -v "stronger memory|Connection reset by peer|requested feature"
echo -e "\n${YELLOW}常见错误提示：${RES}
${BLUE}开机蓝屏; 通常为机算机类型(pc q35)，磁盘接口(IDE SATA VIRTIO)，运行内存配置过大等原因造成，请尝试修改配置${RES}
No such file or directory; ${YELLOW}(没有匹配的目录或文件名)${RES}
Directory does not fit in FAT16 (capacity 516.06 MB); ${YELLOW}(共享文件超过516.06 MB)${RES}
Failed to find an available port: Address already in use; ${YELLOW}(视频输出端口占用)${RES}
unable to find CPU model; ${YELLOW}cpu名字有误${RES}"
	echo -e "\n${GREEN}到底了${RES}"
        read -r -p "是否删除日志 1)是 0)否 " input
	case $input in
		1) rm ${HOME}/.utqemu_log 2>/dev/null ;;
		*) ;;
	esac
	else
	echo -e "${GREEN}无日志信息${RES}"
	sleep 1
	fi
	QEMU_SYSTEM ;;
	8) INFO
	CONFIRM
	QEMU_SYSTEM     ;;
	9) ABOUT_UTQEMU ;;
	10) bash -c "$(curl https://shell.xb6868.com/ut/termux-toolx.sh)" ;;
	11) bash -c "$(curl -s https://shell.xb6868.com/ut/Check_cpuids.sh)"
	CONFIRM
	QEMU_SYSTEM ;;
	12) bash -c "$(curl https://shell.xb6868.com/ut/utdos.sh)" ;;
	0) trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	exit 0 ;;
	*) INVALID_INPUT && QEMU_SYSTEM ;;
	esac
}


################
START_QEMU() {
	if [ ! $(command -v qemu-system-x86_64) ]; then
	echo -e "\n${RED}检测到你未安装qemu，请先执行安装选项${RES}"
	sleep 2
	QEMU_SYSTEM
	fi
	if [ ! -d ${HOME}/share ]; then
		mkdir ${HOME}/share
	fi
	if [ ! -d ${DIRECT}${STORAGE} ]; then
		echo -e "\n${RED}未检测到你的镜像目录，请确认已赋予手机存储权限并创建镜像目录${RES}"
		CONFIRM
	fi
	sync
#	uname -a | grep 'Android' -q 
#	if [ $? == 0 ]; then
#		sleep 1
		display=vnc
#	else
	case $QEMU_MODE in
		VIRTIO_MODE) ;;
		*) echo -n -e "\n${GREEN}是否已有快捷脚本，如有请输快捷脚本名字，如无请回车:${RES} "
	read script_name
	if [ -n "$script_name" ]; then
	if [ $(command -v $script_name) ] || [ -f "${HOME}/xinhao/$script_name" ] || [ -f "${HOME}/short_qemu/$script_name" ]; then
		printf "%s\n"
#cat $(which $script_name)

	if grep 'vnc' /usr/local/bin/$script_name 2>/dev/null || grep 'vnc' ${HOME}/xinhao/$script_name 2>/dev/null || grep 'vnc' ${HOME}/short_qemu/$script_name 2>/dev/null; then
	printf "%s\n${BLUE}启动模拟器${RES}\n"
	printf "%s\n${GREEN}请打开vncviewer 127.0.0.1:0\n"
	fi
	if grep -q 'DISPLAY' /usr/local/bin/$script_name 2>/dev/null; then
	grep '\-cpu' /usr/local/bin/$script_name 2>/dev/null
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开xsdl\n"
	elif grep 'spice' /usr/local/bin/$script_name 2>/dev/null; then
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开aspice 127.0.0.1 端口 5900\n"
	fi
	if grep -q "smb=" /usr/local/bin/$script_name 2>/dev/null || grep -q "smb=" ${HOME}/xinhao/$script_name 2>/dev/null || grep -q "smb=" ${HOME}/short_qemu/$script_name 2>/dev/null; then
	echo '如共享目录成功加载，请在地址栏输 \\10.0.2.4'
	fi
	if grep -q daemonize ${HOME}/xinhao/$script_name 2>/dev/null || grep -q daemonize /usr/local/bin/$script_name 2>/dev/null; then
	echo -e "调试命令：telnet 127.0.0.1 4444${RES}"
	else
	trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	fi
	if grep -q hugepage ${HOME}/xinhao/$script_name 2>/dev/null || grep -q hugepage /usr/local/bin/$script_name 2>/dev/null || grep -q hugepage ${HOME}/short_qemu/$script_name 2>/dev/null; then
	echo -e "${GREEN}你使用了大页内存，开始模拟器前需要时间创建同内存大小文件，文件会在qemu退出后自动删除${RES}"
	fi
	echo ""
	printf "%s${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}\n"
	sleep 1
	$script_name >/dev/null 2>>${HOME}/.utqemu_log || bash ${HOME}/xinhao/$script_name >/dev/null 2>>${HOME}/.utqemu_log || bash ${HOME}/short_qemu/$script_name >/dev/null 2>>${HOME}/.utqemu_log 
	if [ $? == 1 ]; then
	FAIL
	printf "%s${RED}启动意外中止，请查看日志d(ŐдŐ๑)${RES}\n"
	fi
	exit 0
	else
	echo -e "\n${RED}未获取到你的快捷脚本${RES}\n"
	sleep 1
	fi
	fi ;;
esac
	uname -a | grep 'Android' -q
	if [ $? == 1 ]; then
	case $ARCH in
	tablet) echo -e "\n请选择${YELLOW}显示输出方式${RES}"
	case $SYS in
		QEMU_PRE) read -r -p "1)vnc 2)sdl 3)spice 4)图形界面(桌面环境) 5)局域网vnc 9)返回 0)退出 " input ;;
	*) 
	echo -e "1) vnc 画质操控推荐，依赖termux声音输出
2) sdl 模拟效果，声音，画质与操控略低(源地址一般未编译此项)
3) spice 模拟效果与声音略高，画质与操控略低
4) gtk 在容器linux桌面环境(图形界面)显示模拟器
5) 局域网vnc 同一局域网下可以用不同设备显示(请确认局域网ip唯一)
6) 局域网spice 同一局域网下可以用不同设备显示，并且支持声音输出(请确认局域网ip唯一)
7) 多渠道显示 本地vnc、局域网vnc、浏览器
8) 快速启动 仅输镜像名就可以通过vnc模拟显示
9) 返回
0) 退出\n "
	read -r -p "请选择: " input
	esac
	case $input in
		1|"") echo -e "\n${BLUE}vnc输出${RES}"
			display=vnc
			;;
		2) display=xsdl
		read -r -p "1)信号输出(通用) 2)sdl模块输出(通常源安装未包括sdl模块) " input
		case $input in
		2)
		echo -e "${BLUE}请确认qemu已编译sdl模块，否则出现'-sdl: SDL support is disabled'提示${RES}"
		if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-6] ]]; then
		set -- "${@}" "-sdl"
	else
		set -- "${@}" "-display" "sdl"
		fi
		set -- "${@}" "-full-screen" ;;
		*)
		echo -e "${BLUE}sdl信号输出，需先打开xsdl再继续此操作${RES}" ;;
	esac
			;;
		3) echo -e "${BLUE}spice输出${RES}"
	read -r -p "1)常规使用 2)spice传输协议使用(需virtio驱动) " input
	case $input in
		2) display=spice_ ;;
		*) display=spice ;;
	esac
	;;
		4) echo -e "\n${BLUE}窗口输出\n${GREEN}鼠标锁定 ctrl+alt+g\n全屏     ctrl+alt+f${RES}"
			CONFIRM
			display=gtk_ ;;
		5) display=wlan_vnc
	echo -e "\n${GREEN}为减少效率的影响，暂不支持声音输出${RES}\n因部分机型支持双wifi或wifi热点同开，导致出现两段ip，请确保使用的${RED}局域网ip唯一${RES}\n输出显示的设备vnc地址为$IP:0${RES}"
	sleep 1 ;;
		8) case $SYS in
			QEMU_PRE) INVALID_INPUT
			QEMU_SYSTEM ;; 
		*) printf "\n%b\n" "${GREEN}本选项使用常用配置参数${RES}"
		printf "%-7s %-7s %-7s %s\n" 系统 winxp win7 virtio驱动模式 声卡 ac97 hda hda 显卡 cirrus VGA qxl 网卡 e1000 e1000 virtio 接口 ide sata virtio
		printf "%-7s %s %s\n\n" 视频 vnc 127.0.0.1:0
	mem=$(free -m | awk '{print $2/4}' | sed -n 2p | cut -d '.' -f 1)
	echo -e "${YELLOW}请选择拟模拟的系统${RES}"
	read -r -p "1)winxp 2)win7 3)virtio驱动模式 4)小白之家专用参数(不定期更新) 9)返回 " input
	case $input in
	1) echo -e "\nqemu5版本模拟winxp开机比较慢\n"
	LIST
	HDA_READ
	if (( $mem >= 512 )); then
	mem_=1024
	else
	mem_=512
	fi
#-global migration.send-configuration=on 对于旨在支持跨版本实时迁移兼容性的体系结构，每个发行版都将引入新的版本化机器类型。 例如，2.8.0版本针对x86_64 / i686架构引入了机器类型“pc-i440fx-2.8”和“pc-q35-2.8”。为了允许客户机从QEMU 2.8.0版热迁移到QEMU 2.9.0版，2.9.0版本必须支持“pc-i440fx-2.8”和“pc-q35-2.8”机器类型。 升级时为了允许用户跨几个版本实时迁移虚拟机，QEMU的新版本将支持许多先前版本的机器类型。MA="pc-i440fx-3.1"
	QEMU_SYS=qemu-system-i386 MA="pc-i440fx-3.1" MIGRATION="-global migration.send-configuration=on " CPU_MODEL="n270" VIDEO="-device cirrus-vga" DRIVE="-drive file=${DIRECT}${STORAGE}$hda_name,if=ide,index=0,media=disk" NET="-device e1000,netdev=user0 -netdev user,id=user0" AUDIO="-device AC97" SHARE="-drive file=fat:rw:${DIRECT}/xinhao/share,if=ide,index=3,media=disk" S4="-global PIIX4_PM.disable_s4=1 " S3="-global PIIX4_PM.disable_s3=1 " ;;
#-kvm-asyncpf-int,-kvm-poll-control,-kvm-pv-sched-yield,-rdrand
	2) 	LIST
	HDA_READ
	QEMU_SYS=qemu-system-x86_64 MA=pc VIDEO="-device VGA" CPU_MODEL="max,-hle,-rtm" DRIVE="-drive id=disk,file=${DIRECT}${STORAGE}$hda_name,if=none -device ahci,id=ahci -device ide-hd,drive=disk,bus=ahci.0" NET="-device e1000,netdev=user0 -netdev user,id=user0" AUDIO="-device intel-hda -device hda-duplex" SHARE="-drive if=none,format=raw,id=disk1,file=fat:rw:${DIRECT}/xinhao/share/ -device usb-storage,drive=disk1"
;;
	3) echo -e "${GREEN}此选项参数是hda声卡，virtio网卡，qxl显卡，virtio磁盘接口(注意，模拟系统需已装驱动，否则启动不成功)${RES}"
		sleep 1
		LIST
		HDA_READ
	QEMU_SYS=qemu-system-x86_64 MA=q35 CPU_MODEL="max" VIDEO="-device qxl-vga" DRIVE="-drive file=${DIRECT}${STORAGE}$hda_name,index=0,media=disk,if=virtio" NET="-device virtio-net-pci,netdev=user0 -netdev user,id=user0,smb=${HOME}/share" AUDIO="-device intel-hda -device hda-duplex" SHARE="-drive file=fat:rw:${DIRECT}/xinhao/share,index=3,media=disk,if=virtio,format=raw"
	;;
	4) echo -e "\n${GREEN}此选项参数是小白之家定制${RES}\n"
	sleep 1
	LIST
	HDA_READ ;;
	9) QEMU_SYSTEM ;;
	*) INVALID_INPUT
	QEMU_SYSTEM ;;
	esac
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
	if [ -n "$MA" ]; then
START="$QEMU_SYS -machine $MA,hmat=off,usb=on,vmport=off,dump-guest-core=off,mem-merge=off,kernel-irqchip=off $S3$S4$MIGRATION--accel tcg,thread=multi -m $mem_ -nodefaults -no-user-config -msg timestamp=off -k en-us -cpu $CPU_MODEL -smp 2 $VIDEO $NET -audiodev alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124,out.period-length=1024 $AUDIO,audiodev=alsa1 -rtc base=localtime -boot order=cd,menu=on,strict=off -device usb-tablet $DRIVE $SHARE -display vnc=127.0.0.1:0,lossy=on,non-adaptive=off"
	else
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0"
	printf "%s\n${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}\n"
	qemu-system-x86_64 -name 'Windows 7 x64' -machine pc,vmport='off',kernel-irqchip='off',dump-guest-core='off',mem-merge='off',usb='off' -m '2048M' --accel tcg,thread='multi',tb-size='2048' -boot menu='on',strict='off' -mem-prealloc -k en-us -audiodev alsa,id='alsa1',in.channels='2',in.frequency='44100',out.buffer-length='5124',in.format='s16' -device VGA,id='video0',vgamem_mb='512' -device intel-hda -device hda-duplex,audiodev='alsa1' -uuid '1f8e6f7e-5a70-4780-89c1-464dc0e7f308' -nodefaults -no-user-config -no-hpet -no-fd-bootchk -msg timestamp='off' -cpu Cascadelake-Server-v4,model_id='MediaTek Dimensity 1100 @ 2.60GHz',-mds-no,-fma,-pcid,-x2apic,-tsc-deadline,-avx,-f16c,-avx2,-invpcid,-avx512f,-avx512dq,-avx512cd,-avx512bw,-avx512vl,-rdseed,-avx512vnni,-spec-ctrl,-arch-capabilities,-ssbd,-3dnowprefetch,-xsavec,-rdctl-no,-ibrs-all,-skip-l1dfl-vmentry -smp cpus='8',cores='8' -rtc base=localtime -display vnc='127.0.0.1:0',key-delay-ms='0',connections='15000',to='2',lossy='on',non-adaptive='off' -netdev user,id='n1',ipv4='on',ipv6='off' -device e1000,netdev='n1',mac='52:54:98:76:54:32' -smbios type=3,manufacturer='XBZJ' -smbios type=1,manufacturer='Xiaomi',product=' Note 10 Pro',version='2021.10' -smbios type=4,manufacturer='MediaTek',max-speed='5200',current-speed='3600' -smbios type=0,version='Intel-Xeon' -smbios type=2,manufacturer='Intel',version='2021.7',product='Intel ARM' -drive id=disk,file=${DIRECT}${STORAGE}$hda_name,if=none -device ahci,id=ahci -device ide-hd,drive=disk,bus=ahci.0 >/dev/null 2>>${HOME}/.utqemu_log
#-netdev user,id='n1',ipv4='on',ipv6='off' -device e1000,netdev='n1',mac='52:54:98:76:54:32'
	if [ $? == 1 ]; then
	FAIL
	printf "%s${RED}启动意外中止，请查看日志${YELLOW}d(ŐдŐ๑)${RES}\n"
	fi
	exit 0
	fi
#-display vnc=127.0.0.1:0,key-delay-ms=0,connections=15000"

cat <<-EOF
$START
EOF
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0"
#	echo '如共享目录成功加载，请在地址栏输 \\10.0.2.4'
	printf "%s\n${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}\n"
	$START >/dev/null 2>>${HOME}/.utqemu_log
	if [ $? == 1 ]; then
	FAIL
	printf "%s${RED}启动意外中止，请查看日志${YELLOW}d(ŐдŐ๑)${RES}\n"
	fi
	exit 0 ;;
	esac
			;;
		7) 
		dpkg -l novnc >/dev/null 2>&1
                if [ $? != 0 ]; then
                        echo -e "${YELLOW}检测所需vnc包${RES}"
                        sleep 1
			sudo_
                        $sudo apt install tigervnc-standalone-server novnc --no-install-recommends -y
			sed -i '/Fail/{n;s/^/#/}' /usr/share/novnc/utils/launch.sh
                        fi
			NOVNC=novnc
			display=xvnc ;;
		6) display=wlan_spice
			echo -e "\n${RES}\n因部分机型支持双wifi或wifi热点同开，导致出现两段ip，请确保使用的${RED}局域网ip唯一${RES}\n输出显示的设备spice地址为$IP:0${RES}"
			sleep 1;;
		9) QEMU_SYSTEM ;;
		0) exit 0 ;;
		*) INVALID_INPUT
			QEMU_SYSTEM ;;
	esac
	sleep 1 ;;
	computer)
		echo -e "\n请选择${YELLOW}显示输出方式${RES}"
		read -r -p "1)本地 2)局域网vnc 3)局域网spice 9)返回 0)退出 " input
		case $input in
			1|"")
				display=amd ;;
			2)
				display=wlan_vnc
				echo -e "\n${BLUE}vnc不支持声音输出，输出显示的设备vnc地址为$IP:0${RES}"
				sleep 1 ;;
			3)
				display=wlan_spice
				echo -e "\n${BLUE}输出显示的设备spice地址为$IP:0${RES}"
				sleep 1 ;;
			9) QEMU_SYSTEM ;;
			0) exit 0 ;;
			*) INVALID_INPUT
				QEMU_SYSTEM ;;
		esac
		sleep 1 ;;
	esac
	fi
##################
	echo -e "\n\e[1;33m如果不确定选什么，请直接回车${RES}\n"
	CONFIRM
	echo -e "\n请选择启动哪个${YELLOW}模拟器架构${RES}\n
	1) qemu-system-x86_64 (64位操作系统)
	2) qemu-system-i386   (32位操作系统)\n"
	read -r -p "请选择: " input
	case $input in
		2) QEMU_SYS=qemu-system-i386 ;;
		*) QEMU_SYS=qemu-system-x86_64 ;;
 	esac
###################

	pkill -9 qemu-system-x86 2>/dev/null
	pkill -9 qemu-system-i38 2>/dev/null
	if [ ! -d "${DIRECT}${STORAGE}" ];then
		echo -e "${RED}未获取到镜像目录，请确认已创建镜像目录${RES}\n"
		CONFIRM
		QEMU_SYSTEM
	fi
	LIST
		HDA_READ

#内存
	echo -e -n "请输入模拟的${YELLOW}内存${RES}大小(建议本机的1/4)，以m为单位（1g=1024m，例如输512），自动分配请回车: "
        read mem
	mem=`echo $mem | tr -cd '[0-9]'`
	if [ -n "$mem" ]; then
		if (( "$mem" > "$mem_" )); then
			echo -e "${YELLOW}你设置的内存值大于推荐值，建议使用大页内存(通过创建相应大页文件代替设备ram，响应速度略降低)${RES}"
		read -r -p "1)使用大页 0)使用设备ram " input
		case $input in
			1) HUGEPAGE=true ;;
			*) ;;
		esac
		fi
	fi

#GenuineIntel AuthenticAMD
	echo -e "是否自定义${YELLOW}逻辑cpu${RES}数量"
	read -r -p "1)默认配置 2)自定义 " input
	case $input in
		1|"") _SMP="" ;;
		*) CPU=0
			while [ $CPU -eq 0 ]
	do
	echo -n -e "请输入逻辑cpu参数，分别为核心、线程、插槽个数，输入三位数字(例如2核1线2插槽,不能有0 则输212) "
	read SMP     
	CORES=`echo $SMP | cut -b 1`   
	THREADS=`echo $SMP | cut -b 2`    
	SOCKETS=`echo $SMP | cut -b 3`    
	let CPU=$CORES*$THREADS*$SOCKETS 2>/dev/null
	done
	echo -e "${YELLOW}$CORES核心$THREADS线程$SOCKETS插槽${RES}"
	_SMP="$CPU,cores=$CORES,threads=$THREADS,sockets=$SOCKETS" ;;
	esac
	echo -e "请选择${YELLOW}cpu${RES}"
	case $SYS in
	QEMU_ADV|ANDROID)
		case $ARCH in
		computer)
			read -r -p "1)n270 2)athlon 3)pentium2 4)core2duo 5)Skylake-Server-IBRS 6)Nehalem-IBRS 7)Opteron_G5 8)max(推荐) 9)host(推荐) 0)自己输 " input ;;
		*)
		read -r -p "1)n270 2)athlon 3)pentium2 4)core2duo 5)Skylake-Server-IBRS 6)Nehalem-IBRS 7)Opteron_G5 8)max(推荐) 9)Cascadelake-Server-v4 0)自己输 " input ;;
		esac ;;
		QEMU_PRE) read -r -p "1)n270 2)athlon 3)pentium2 4)core2duo 5)Skylake-Server-IBRS 6)Nehalem-IBRS 7)Opteron_G5 9)max 0)自己输 " input ;;
	esac
#max 对本机cpu的特性加载到虚拟机 host 直接迁移本机cpu到虚拟机(适用于kvm)
:<<\eof
flags各项含义：
所谓指令集，就是CPU中用来计算和控制计算机系统的一套指令的集合，而每一种新型的CPU在设计时就规定了一系列与其他硬件电路相配合的指令系统。而指令集的先进与否，也关系到CPU的性能发挥，它也是CPU性能体现的一个重要标志。
输入用的寄存器为EAX（有时也会用到ECX作为扩展输入），用于指定CPUID的功能。在执行CPUID指令前需要往EAX寄存器写入相应的值。
输出用的寄存器为EAX、EBX、ECX、EDX共四个。在CPUID指令执行后可以从这四个寄存器中获取到所需要的信息。
abm: 高级bit操作
acc: Automatic Clock Control 自动时钟控制
3dnowext: AMD 3DNow扩展
acpi:  ACPI via MSR 高级配置和电源管理接口(ProcessorDutyCycleControl)
aes: AES指令集是一种针对加密计算的CPU测试。AES加密技术被广泛应用于WIFI加密，光盘加密等领域。
apic: Onboard Advanced Programmable Interrupt Controller 板载APIC
avx: 高级 矢量扩展
bts: 分支跟踪存储
fpu:  Onboard (x87) Floating Point Unit 板载FPU浮点运算(常用于kernel)
cid: 上下文ID
cr8legacy: 扩展的APIC空间，cr8legacy – 32位模式下的CR8
cx8:  CMPXCHG8 instruction CMPXCHG8指令 CPU内部，多个核心之间有一条环形总线，当有某一个核心需要锁住cache的时候，这个总线会通知所有的核心，所以只要有某个核心使用了cmpxchg，那么其它的核肯定都会停下来，不会出现并发的情况。
cx16: CMPXCHG16B
vme:  Virtual Mode Extension 虚拟模式扩展
de: Debugging Extensions 调试扩展
dca: 直接缓存访问
ds_cpl: CPL Qual.调试存储
dts: 调试存储
dtes: Debug Trace Store
dtes64: 64 位调试存储，监控器–监控/等待支持
smap: (Supervisor Mode Access Prevention，管理模式访问保护)
smep: (Supervisor Mode Execution Prevention，管理模式执行保护)
syscall: 用户态发起syscall请求，调用陷阱指令（i386为int指令）陷入 内核态执行syscall，CPU特权级别变更，每个系统调用函数都有一个唯一的ID，内核态通过这个ID区别不通的系统调用请求。linux提供了大 约300个系统调用
rdrand: 使用 CPU 内部的热噪声生成随机数。随机数发生器的主要应用是加密技术，它们用于生成随机加密密钥以安全地传输数据。它们广泛用于Internet加密协议，如Secure Sockets Layer（SSL）
est: “Enhanced SpeedStep” 增强的SpeedStep
extapic: 扩展的APIC空
bmi1: 新cpu模拟旧cpu向下兼容特性，不兼容旧特性会出现 #UD (UnDefined)
bmi2: 新cpu模拟旧cpu向下兼容特性，新cpu模拟旧cpu向下兼容特性，不兼容旧特性会出现 #UD (UnDefined)
xptr: 发送任务优先级消息
xsave: XSAVE / XRSTOR / XSETBV / XGETBV 常用于kernel
xsaveopt: 是xsave的优化版,常用于kernel在进程切换的时候保存进程使用fpu寄存器现场
xgetbv: 程序通过访问寄存器XCR0（eXterned Control Register）可以得到操作系统对SIMD扩展的支持信息。该寄存器通过XSETBV进行设置，通过XGETBV进行读取。
wdt: 看门狗定时器，硬件锁定清除功能
功
hle: 硬件锁定清除功能，HLE与RTM为TSX指令集，决定服务器cpu多线程或单线程处理数据。
rtm: 受限事务存储功能，HLE与RTM为TSX指令集，决定服务器cpu多线程或单线程处理数据。
pku: Protection Keys for User-Mode Pages
pse: Page Size Extensions 页面大小扩展
k8: 皓龙，速龙64
k7: 速龙
pebs: 基于 精确事件的采样
tsc: Time Stamp Counter: support for RDTSC and WRTSC instructions 时间戳计数器，操作系统通常可以得到更为精准的时间度量
msr: Model-Specific Registers 特定于模型的寄存器
nonstop_tsc: TSC不会在C状 态下停止
pae: Physical Address Extensions: ability to access 64GB of memory; only 4GB can be accessed at a time though 物理地址扩展
ibs: 基于Sampling的采样
sep: Sysenter/Sysexit Instructions; SYSENTER is used for jumps to kernel memory during system calls, and SYSEXIT is used for jumps： back to the user code
smx: 更安全的模式
mtrr: Memory Type Range Registers 存储器类型范围寄存器
pge: Page Global Enable
mca: Machine Check Architecture 架构检查
mce: Machine Check Exception 当处理器探测mca到机器内部错误或者总线错误的时候，就会发送该中断。
npt: 模拟器二级地址翻译
cmov: CMOV instruction instructions（附加FCMOVcc，带有FPU的FCOMI）
pat: Page Attribute Table 页面属性表
pclmulqdq: PCLMULQDQ指令，无进位乘法指令 与aes可将虚拟电缆调制解调器终端系统 (vCMTS) 数据平面的性能提高
pse36: 36-bit Page Size Extensions: allows to map 4 MB pages into the first 64GB RAM, used with PSE.
ssse3: 补充SSE-3
pn: Processor Serial-Number; only available on Pentium 3
pcommit: 把所有落在持久化内存区域的store持久化(已被intel弃用)
clflush: CLFLUSH instruction clflush CLFLUSH指令，（Cache Line Flush，缓存行刷回）能够把指定缓存行（Cache Line）从所有级缓存中淘汰，若该缓存行中的数据被修改过，则将该数据写入主存；支持现状：目前主流处理器均支持该指令。
clflushopt:（Optimized CLFLUSH，优化的缓存行刷回）作用与 CLFLUSH 相似，但其之间的指令级并行度更高，比如在访问不同 CacheLine 时，CLFLUHOPT 可以乱序执行。支持现状
clwb: （Cache Line Write Back，缓存行写回）作用与 CLFLUSHOPT 相似，但在将缓存行中的数据写回之后，该缓存行仍将呈现为未被修改过的状态；支持现状
mmx: MultiMedia Extension –多媒体扩展
pbe: 等待中断启用
mmxext: AMD MMX扩展
fxsr: FXSAVE and FXSTOR instructions (Fast floating-point save and restore)
fxsr-opt: FXSAVE / FXRSTOR (Fast floating-point save and restore)优化
sse: Streaming SIMD Extensions. Single instruction multiple data. Lets you do a bunch of the same operation on different pieces of input in a single clock tick.
sse2: Streaming SIMD Extensions-2. More of the same.
selfsnoop： CPU self snoop
ia64: IA-64 processor Itanium. IA-64处理器
ht: HyperThreading. Introduces an imaginary second processor that doesn’t do much but lets you run threads in the same process a bit quicker. 超线程
hypervisor: 在hypervisor上运行
nx: No Execute bit. Prevents arbitrary code running via buffer overflows.
pni: Prescott New Instructions aka. SSE3
vmx: Intel Vanderpool hardware virtualization technology 英特尔虚拟化技术(VT技术)
svm: AMD “Pacifica” hardware virtualization technology AMD的虚拟化技术(AMD-V)
lm: “Long Mode,” which means the chip supports the AMD64 instruction set 长模式（芯片支持x86-64指令）
lahf-lm: 
tm: “Thermal Monitor” Thermal throttling with IDLE instructions. Usually hardware controlled in response to CPU temperature. 自动时钟控制
tm2: “Thermal Monitor 2″ Decrease speed by reducing multipler and vcore. 温度监控器2
ss: CPU自侦听，堆栈段寄存器，是在程序运行时动态分配使用，当一个程序要执行时，就要决定程序代码、数据和堆栈各要用到内存的哪些位置，通过设定段寄存器 CS，DS，SS 来指向这些起始位置。
eof

	case $input in
	1) CPU_MODEL=n270
		unset LOW_CORE
		SMP_="2,cores=1,threads=2,sockets=1" ;;
        2) CPU_MODEL=athlon
		unset LOW_CORE
		SMP_="2,cores=2,threads=1,sockets=1" ;;
        3) CPU_MODEL=pentium2
		unset LOW_CORE
		SMP_="1,cores=1,threads=1,sockets=1" ;;
        4) CPU_MODEL=core2duo
		unset LOW_CORE
		SMP_="2,cores=2,threads=1,sockets=1" ;;
	5) CPU_MODEL=Skylake-Server-IBRS
		SMP_="4,cores=2,threads=1,sockets=2" ;;
	6) CPU_MODEL=Nehalem-IBRS
		SMP_="8,cores=8,threads=1,sockets=1" ;;
	7) CPU_MODEL=Opteron_G5
		SMP_="8,cores=8,threads=1,sockets=1" ;;
	8) CPU_MODEL="max"
		unset _SMP
		SMP_="4,cores=4"
		MAXCPUS="4,cores=4,sockets=2,maxcpus=8" ;;
	9) case $ARCH in
		computer) CPU_MODEL=host
			unset _SMP
			SMP_="4,cores=4"
			MAXCPUS="4,cores=4,sockets=2,maxcpus=8"
			;;
		*)
: <<\eof
hv_spinlocks=0xffff：GuestOS执行spinlock期间，其实是可以转让CPU给其他vCPU调度的。短时间的spinlock可以节省vCPU调度开销，长时间的spinlock会浪费CPU资源。为此，参数用于让guest重试"hv-spinlocks=number"次无果后通告hypervisor，主动转让CPU。
hv-spinlocks=0 表示不尝试(一旦guest调用spinlock，立刻退出到hypervisor转让CPU)
hv-spinlocks=0xFFFFFFFF(x86虚机缺省值)任其guest一直执行spinlock。
配置hv-spinlocks决定#cpuid 0x40000004返回后的整个EBX寄存器值。

hv_relaxed：配置hv-relaxed后，vCPU被长时间抢占不会导致WindowsOS蓝屏，建议所有Windows虚机都打开。

hv_time：配置hv-time决定Guest执行#cpuid 0x40000003返回的寄存器EAX中的bit2，bit2表示运行环境的Hypervisor有提供如上MSRs供Guest用。KVM实现了如下MSR，Synthetic-interrupt-controller（SynIC，是LAPIC的功能扩展）SynIC是一个半虚拟化中断控制器提供向Guest发送中断机制(VMBus Message)，guest通过如下MSR接口控制，VMBus-devices和Hyper-V-synthetic-timers依赖此特性，QEMU目前尚未有VMBus-devices设备。
hv-vapic：是否配置hv-vapic决定Guest执行#cpuid 0x40000003返回的寄存器EAX中的bit4，同时也决定Guest执行#cpuid 0x40000004返回的寄存器EAX的bit3。#cpuid 0x40000003返回值各个位表示Hypervisor具备哪些特性。#cpuid 0x40000004返回值各个位表示Hypervisor推荐Guest使用哪些特性。EAX.bit4(HV_SYNTIMERS_AVAILABLE)为1时指示Guest当前Hypervisor有提供如上三个MSRs。guest可以通过rdmsr/wrmsr指令直接提交中断请求或中断应答，相比全模拟的APCI，HV-VAPIC大量减少vmexit。倘若EAX.bit4(HV_SYNTIMERS_AVAILABLE)为0，Guest执行#wrmsr HV_X64_MSR_EOI将引发异常。KVM提供了如下三个MSR(半虚拟化的APIC)，用于pv-guest一次性提交中断请求或中断应答：

HV_X64_MSR_EOI
HV_X64_MSR_ICR
HV_X64_MSR_TPR
hv_spinlocks=0xFFFFFFFF,hv_relaxed,hv_time,hv_vapic

-smbios BIOS信息(Type 0)、系统信息(Type 1)、系统外围或底架(Type 3)、处理器信息(Type 4)、高速缓存信息(Type 7)、系统插槽(Type 9)、物理存储阵列(Type 16)、存储设备(Type 17)、存储阵列映射地址(Type 19)、系统引导信息(Type 32)
Product Name ： 产品名称，苹果电脑型号。
Family ： 家庭， 苹果电脑所属系列。
Manufacturer ： 制造商名称，Apple Inc 即苹果公司。
Bios Version：主板Bios 版本号。
Bios Release Date ：主板Bios发布日期。
Bios Vendor ：主板Bios提供商 。
Chassis Manufacturer ： 机箱制造商
Location In Chassis：机箱位置。
Chassis Asset Tag：机箱资产标签。
Chassis Type ： 机箱类型。
Board Type ：主板类型。
Board-ID：主板ID。
Board Manufacturer ：主板制造商信息。
Board Version ： 主板版本。
Board Serial Number ： 主板序列号。
Serial Number ：电脑序列号。
Generate New ：随机获取新的序列号。
SmUUID： 格式应为“00000000-0000-1000-8000-xxxxxxxxxx”其中“xxxxxxxxxxxx”为你的网卡MAC值。
Mobile：移动。若为移动平台，请勾选。
Trust ：托管。和内置自定义的smbios table相关，如果你的内存侦测存在问题请取消勾选试试。
Firmware Features：固件特征。
Firmware Features Mask：固件特征掩码。
Platform Features ：平台功能。
Version ：固件版本，
eof
		CPU_MODEL="Cascadelake-Server-v4,model_id='Intel(R) Xeno(TM) E7-8891 v2 @ 3.60GHz',l3-cache=true,vmware-cpuid-freq=false,-mds-no,-fma,-pcid,-x2apic,-tsc-deadline,-avx,-f16c,-avx2,-invpcid,-avx512f,-avx512dq,-avx512cd,-avx512bw,-avx512vl,-rdseed,-avx512vnni,-spec-ctrl,-arch-capabilities,-ssbd,-3dnowprefetch,-xsavec,-rdctl-no,-ibrs-all,-skip-l1dfl-vmentry"
#"core2duo,-lm,-syscall,-hle,-rtm,hv_spinlocks=0xFFFFFFFF,hv_relaxed,hv_time,hv_vapic,hv-frequencies"
		unset _SMP
		SMP_="8,cores=8,threads=1,sockets=1"
		MAXCPUS="8,cores=8,threads=1,sockets=2,maxcpus=16" ;;
		esac ;;
	0) NUM=`qemu-system-i386 --cpu help | awk '{print $2}' | cat -n | grep max | awk '{print $1}'`
		qemu-system-i386 --cpu help | awk '{print $2}' | head -n $NUM | tail -n $(( $NUM - 1 ))
		echo -e "$YELLOW已为你列出支持的cpu类型\n(增加特性请+，去除特性请-，例如core2duo,+3dnowext,-avx)$RES"
		echo -e -n "请输入: "
		read CPU_MODEL
		if echo $CPU_MODEL | grep max; then
		unset _SMP
		SMP_="4,cores=4"
		MAXCPUS="4,cores=4,sockets=2,maxcpus=8"
		else
		SMP_="4,cores=4,threads=1,sockets=1"
		fi ;;
#		set -- "${@}" "-name" "${hda_name%.*}"
	94) CPU_MODEL="IvyBridge-v2,model_id=Intel(R) Xeon(R) CPU E5-2680 v2 @ 2.80GHz,-x2apic,-tsc-deadline,-avx,-f16c,-spec-ctrl,-syscall,-lm,+pdpe1gb"
#+hypervisor,hv_spinlocks=0xFFFFFFFF,hv_relaxed,
		SMP_="cpus=8,cores=8"
		set -- "${@}" "-smbios" "type=0,vendor=Hewlett-Packard,version=J61 v03.69,date=03/25/2014,release=03.2014,uefi=on"
		set -- "${@}" "-smbios" "type=1,manufacturer=Hewlett-Packard,product=HP Z620 Workstation,version=Not Specified,serial=6CR419WFHT,uuid=90065980-D287-11E3-B1B0-A0481CABDFB4,sku=G2F14UC#AB2,family=103C_53335X G=D"
		set -- "${@}" "-smbios" "type=2,manufacturer=Hewlett-Packard,product=158A,version=0.00,serial=6CR419WFHT,asset=6CR419WFHT,location=Not Specified"
                set -- "${@}" "-smbios" "type=3,manufacturer=Hewlett-Packard,version=Not Specified,serial=6CR419WFHT,asset=6CR419WFHT,sku=Not Specified"
		set -- "${@}" "-smbios" "type=4,sock_pfx=CPU0,manufacturer=Intel,version=Intel(R) Xeon(R) CPU E5-2680 v2 @ 2.80GHz,serial=Not Specified,asset=Not Specified,part=Not Specified,max-speed=3800,current-speed=2800" ;;
	97) CPU_MODEL="Cascadelake-Server-v4,model_id=Intel(R) Xeno(TM) Gold 5218 @ 2.30GHz,l3-cache=true,-fma,-pcid,-x2apic,-tsc-deadline,-avx,-f16c,-avx2,-invpcid,-avx512f,-avx512dq,-rdseed,-avx512cd,-avx512bw,-avx512vl,-avx512vnni,-spec-ctrl,-arch-capabilities,-ssbd,-syscall,-lm,-3dnowprefetch,-xsavec,-rdctl-no,-ibrs-all,-skip-l1dfl-vmentry,-mds-no,+pdpe1gb"
		SMP_="8,cores=8,threads=1,sockets=1"
		MAXCPUS="8,cores=8,threads=1,sockets=2,maxcpus=16"	;;
	98) CPU_MODEL="EPYC-IBPB,-fma,-avx,-f16c,-avx2,-rdseed,-sha-ni,-fxsr-opt,-misalignsse,-3dnowprefetch,-osvw,-topoext,-ibpb,-nrip-save,-xsavec,-de,-syscall,-lm,+hypervisor,+pdpe1gb,+3dnow,+3dnowext,+sse4.1,+sse4.2,+ssse3,+pni,+cr8legacy,+fxsr,+xgetbv1,+xsave,+xsaveopt,+npt"
		SMP_="4,cores=4,threads=1,sockets=1"
		MAXCPUS="4,cores=4,threads=1,sockets=2,maxcpus=8" ;;
	99) 
#AMD Phenom(tm) 9550 Quad-Core Processor
#	CPU_MODEL="phenom-v1,-fxsr-opt,-syscall,-de,-lm,-clflush,-clflushopt,-clwb,+pdpe1gb,+sse4.1,+sse4.2,+ssse3,+pni,+cr8legacy,+fxsr,+xgetbv1,+xsave,+xsaveopt,+npt"
	CPU_MODEL="phenom-v1,-fxsr-opt,-syscall,-lm,-de,-rdrand,+aes,+pclmulqdq,+3dnowext,+pdpe1gb,+sse4.1,+sse4.2,+ssse3,+pni,+cr8legacy,+fxsr,+xgetbv1,+xsave,+xsaveopt,+npt"
	SMP_="4,cores=4,threads=1,sockets=1"
	MAXCPUS="4,cores=4,threads=1,sockets=2,maxcpus=8" ;;
	*) case $SYS in
		QEMU_PRE) CPU_MODEL="max,-hle,-rtm"
		SMP_="2,cores=1,threads=2,sockets=1"
		;;
        *)      CPU_MODEL="max,-hle,-rtm"
		unset _SMP
		SMP_="4,cores=4"
		MAXCPUS="4,cores=4,sockets=2,maxcpus=8" ;;
	esac ;;
	esac
#####################
#TERMUX
	uname -a | grep 'Android' -q 
	if [ $? == 0 ]; then
	echo -e "请选择${YELLOW}显卡${RES}"
	read -r -p "1)cirrus 2)vmware 3)vga 4)virtio " input
	case $input in 
		1) VGA_MODEL=cirrus-vga ;;
		2) VGA_MODEL=vmware-svga,vgamem_mb=256 ;;
		4) VGA_MODEL=virtio-vga ;;
		*) VGA_MODEL="VGA,vgamem_mb=256,refresh_rate=60"
#-device 'VGA',id='video0',vgamem_mb='256',global-vmstate='false',qemu-extended-regs='off',rombar='1',xmax='1920',xres='1280',ymax='1080',yres='720'  ;;
        esac
	set -- "${@}" "-device" "${VGA_MODEL}"

	echo -e "请选择${YELLOW}网卡${RES}"
	read -r -p "1)e1000 2)rtl8139 3)virtio 0)不加载 " input
	case $input in
		2) NET_MODEL1="rtl8139,netdev=user0" ;;
		3) NET_MODEL1="virtio-net-pci,netdev=user0" ;;
		0) ;;
		*) NET_MODEL1="e1000,netdev=user0" ;;
	esac
	echo -e "请选择${YELLOW}声卡${RES}(强烈不建议)"
	read -r -p "1)ac97 2)sb16 3)es1370 4)hda 0)不加载 " input
	case $input in
		1) SOUND_MODEL=AC97 ;;
		2) SOUND_MODEL=sb16 ;;
		3) SOUND_MODEL=ES1370 ;;
		4) set -- "${@}" "-device" "intel-hda"
		SOUND_MODEL=hda-duplex ;;
		0) ;;
		esac
		if [ -n "${SOUND_MODEL}" ]; then
		pulseaudio --start &
#		set -- "${@}" "-audiodev" "pa,server=127.0.0.1:4713,id=pa1,in.latency=5300,out.latency=5300,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=10248"
		set -- "${@}" "-audiodev" "pa,server=127.0.0.1:4713,id=pa1,timer-period=30000,in.format=s16,in.channels=2,in.frequency=8000,in.buffer-length=10248"
		set -- "${@}" "-device" "$SOUND_MODEL,audiodev=pa1"
		fi
	fi
##################
#PROOT
#####################
#<5.0
	[[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [5-9] ]] || uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	echo -e "请选择${YELLOW}显卡${RES}"
	read -r -p "1)cirrus 2)vmware 3)std 4)virtio 5)qxl " input
        case $input in
                1) VGA_MODEL=cirrus ;;
                2) VGA_MODEL=vmware ;;
		4) VGA_MODEL=virtio ;;
		5) VGA_MODEL=qxl ;;
		*) VGA_MODEL=std ;;
	esac
	set -- "${@}" "-vga" "${VGA_MODEL}"
#内存锁，默认打开
	if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-3] ]]; then
	set -- "${@}" "-realtime" "mlock=off"
	fi

	echo -e "请选择${YELLOW}网卡${RES}"
	read -r -p "1)e1000 2)rtl8139 3)virtio 0)不加载 " input
	if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = 4 ]]; then
	case $input in
		2) NET_MODEL="rtl8139,netdev=user0" ;;
		3) NET_MODEL="virtio-net-pci,netdev=user0" ;;
		0) ;;
		*) NET_MODEL="e1000,netdev=user0" ;;
	esac
	else
	case $input in
		2) NET_MODEL0="nic,model=rtl8139" ;;
		3) NET_MODEL0="nic,model=virtio" ;;
		0) ;;
		*) NET_MODEL0="nic,model=e1000" ;;
	esac
	fi
	case $display in
		wlan_vnc) ;;
		*)
		echo -e "请选择${YELLOW}声卡${RES}(不加载可提升模拟效率)"
	if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = 4 ]]; then
	case $display in
            wlan_spice|spice|spice_)
		    read -r -p "1)es1370 2)sb16 3)hda 4)ac97 5)usb-audio 0)不加载 " input ;;
		    *)
			read -r -p "1)es1370 2)sb16 3)hda 4)ac97 5)usb-audio 6)ac97(修改参数，优化声音卡顿，不适合spice) 7)hda(修改参数，优化声音卡顿，不适合spice) 0)不加载 " input ;;
		esac
	case $display in
		wlan_spice|spice|spice_)
	case $input in
		1) SOUND_MODEL=ES1370 ;;
		2) SOUND_MODEL=sb16 ;;
		4) SOUND_MODEL=AC97 ;;
		5) SOUND_MODEL=usb-audio ;;
		0) ;;
		*) SOUND_MODEL=hda-duplex
			set -- "${@}" "-device" "intel-hda" ;;
	esac
	if [ -n "${SOUND_MODEL}" ]; then
		set -- "${@}" "-device" "${SOUND_MODEL}"
	fi ;;
	*)
		case $input in
		1) AUDIODEV=PA
			SOUND_MODEL=ES1370
			audiodev=pa1 ;;
		2) AUDIODEV=PA
			SOUND_MODEL=sb16
			audiodev=pa1 ;;
		3) AUDIODEV=PA
			set -- "${@}" "-device" "intel-hda"
			SOUND_MODEL=hda-duplex
			audiodev=pa1 ;;
		0) ;;
		4)
                AUDIODEV=PA
                SOUND_MODEL=AC97
                audiodev=pa1 ;;
                5) AUDIODEV=PA
                        SOUND_MODEL=usb-audio
                        audiodev=pa1 ;;
                6) AUDIODEV=ALSA
                        SOUND_MODEL=AC97
			audiodev=alsa1                 ;;
                *) AUDIODEV=ALSA
                set -- "${@}" "-device" "intel-hda"
		SOUND_MODEL=hda-duplex
		audiodev=alsa1 ;;
                esac
		if [ -n "${SOUND_MODEL}" ]; then
			set -- "${@}" "-device" "${SOUND_MODEL},audiodev=${audiodev}"
			case ${AUDIODEV} in
				PA)
        set -- "${@}" "-audiodev" "pa,server=127.0.0.1:4713,id=pa1" ;;
        *)
        set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124" ;;
        esac
        fi ;;
        esac
	else
	read -r -p "1)ac97 2)sb16 3)es1370 4)hda 0)不加载 " input
	case $input in
		1|"") SOUND_MODEL=ac97 ;;
		2) SOUND_MODEL=sb16 ;;
		0) ;;
		3) SOUND_MODEL=es1370 ;;
		4) SOUND_MODEL=hda ;;
		*) SOUND_MODEL=all ;;
	esac
	if [ -n "${SOUND_MODEL}" ]; then
		set -- "${@}" "-soundhw" "${SOUND_MODEL}"
	fi
	fi
	esac
	fi
####################
#5.0
####################
	[[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-4] ]] || uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	echo -e "请选择${YELLOW}显卡${RES}"
	read -r -p "1)cirrus 2)vmware 3)vga 4)virtio 5)qxl " input
	case $input in
		1) set -- "${@}" "-device" "cirrus-vga" ;;
		2) set -- "${@}" "-device" "vmware-svga,vgamem_mb=256" ;;
		4) echo -e "${YELLOW}virtio显卡带3D功能，但因使用的系统环境原因，目前只能通过电脑启用，如果真想尝试，可在vnc或图形界面打开(需32位色彩，否则出现花屏)\n目前测试需win8以上系统。\n系统已装virtio的gpu驱动，如果没有请去virtio相关下载${RES}"
	read -r -p "1)不设置3D参数 2)设置3D参数 " input
	case $input in
#		set -- "${@}" "-device" "virtio-vga,virgl=on"
		2) echo -e "\n${YELLOW}你选择virtio显卡3D参数，该模式只能在vnc或图形界面(桌面)显示\n${RES}"
	CONFIRM
	case $display in
		xsdl) set -- "${@}" "-device" "virtio-vga" "-display" "sdl,gl=on"
		unset display ;;
		vnc|wlan_vnc|xvnc)
		export PULSE_SERVER=tcp:127.0.0.1:4713
			if [ ! $(command -v tigervncserver) ]; then
			echo -e "${YELLOW}检测所需vnc包${RES}"
			sleep 1
#			$sudo apt install tigervnc-standalone-server tigervnc-viewer --no-install-recommends -y
	sudo_
			$sudo apt install tigervnc-standalone-server --no-install-recommends -y
			fi
			dpkg -l libegl1 >/dev/null 2>&1; if [ $? != 0 ]; then 
			sudo_
			$sudo apt install libegl1 libgdk-pixbuf2.0-0 -y; fi
#		set -- "${@}" "-vga" "qxl" "-display" "gtk,gl=on" "-device" "virtio-gpu-pci,virgl=on"
		set -- "${@}" "-device" "virtio-vga" "-display" "gtk,gl=on" "-device" "virtio-gpu-pci"
		display=xvnc
#		set -- "${@}" "-device" "qxl" "-vga" "virtio" "-display" "gtk,gl=on"
;;
		wlan_spice|spice_|spice|amd|gtk_) set -- "${@}" "-device" "virtio-vga" "-display" "gtk,gl=on"
		unset display	;;
	esac
	case $ARCH in
		computer) ;;
		*) env | grep 'PULSE_SERVER' -q
	if [ $? != 0 ]; then
		export PULSE_SERVER=tcp:127.0.0.1:4713
		fi ;;
	esac ;;
	*) set -- "${@}" "-device" "virtio-vga" ;;
	esac ;;
		5) set -- "${@}" "-device" "qxl-vga,vgamem_mb=256"
#			set -- "${@}" "-device" "virtio-keyboard-pci"
;;
		*) set -- "${@}" "-device" "VGA,vgamem_mb=256,edid=off,x-pcie-lnksta-dllla=off" ;;
#EDID是一种VESA标准数据格式，它包含监视器和自身性能的基本信息。基本信息主要有输出分辨率、最大图像尺寸、颜色特征、出厂事先设置时间、频率范围限制和监视器名称等
#global-vmstate=flase: With this in place you don't get a vmstate section naming conflict any more when adding multiple pci vga devices to your vm.此参数可兼容多个pci显卡
#edid=true Extended Display Identification Data(扩展显示标识数据) 为了能让PC或其他的图像输出设备更好的识别显示器属性
#lnksta为当前实际的传输速率，目前系統所提供的速度 PCI-Express 2.0 ( 5GT/s ) dllla动态库链接分配器
#extcap接口是一个多功能插件接口，它允许外部二进制文件直接作为wireshark中的捕获接口。它用于捕获源不是传统捕获模型的场景(从接口、管道、文件等进行实时捕获)
#multifunction=off device can be plugged into each Port. This results in poor IO space utilization.设备可以插入每个端口， 结果是io利用率低
#qemu-extended-regs qemu扩展寄存器
	esac

	echo -e "请选择${YELLOW}网卡${RES}"
	read -r -p "1)e1000 2)rtl8139 3)virtio 0)不加载 " input
	case $input in
		2) NET_MODEL1="rtl8139,netdev=user0" ;;
		3) NET_MODEL1="virtio-net-pci,netdev=user0" ;;
		0) ;;
		*) NET_MODEL1="e1000,netdev=user0" ;;
	esac
	case $display in
		wlan_vnc) ;;
		*)
	echo -e "请选择${YELLOW}声卡${RES}(不加载可提升模拟效率)"
	case $display in                                                      wlan_spice|spice|spice_)
		read -r -p "1)es1370 2)sb16 3)hda 4)ac97 5)usb-audio 0)不加载 " input ;;
		*)
		read -r -p "1)es1370 2)sb16 3)hda 4)ac97 5)usb-audio 6)ac97(修改参数，优化声音卡顿，不适合spice) 7)hda(修改参数，优化声音卡顿，不适合spice) 0)不加载 " input ;;
	esac
	case $display in
		wlan_spice|spice|spice_)
	case $input in
		1) SOUND_MODEL=ES1370 ;;
		2) SOUND_MODEL=sb16 ;;
		4) SOUND_MODEL=AC97 ;;
		5) SOUND_MODEL=usb-audio ;;
		0) ;;
		*) SOUND_MODEL=hda-duplex
			set -- "${@}" "-device" "intel-hda" ;;
	esac
	if [ -n "${SOUND_MODEL}" ]; then
	set -- "${@}" "-device" "${SOUND_MODEL}"
	fi ;;
	*)
	case $input in
		1) AUDIODEV=PA
			SOUND_MODEL=ES1370
			audiodev=pa1 ;;
		2) AUDIODEV=PA
			SOUND_MODEL=sb16
			audiodev=pa1 ;;
		3) AUDIODEV=PA
			set -- "${@}" "-device" "intel-hda" 
			SOUND_MODEL=hda-duplex
			audiodev=pa1 ;;
                0) ;;
		4)
#adc in dac out				
#alsa参数			       	
#延迟timer-period=10000
#采样率out.frequency=8004
#缓冲长度(理论上应为周期长度的倍数)out.buffer-length=10000
#周期长度out.period-length=1020
#pa参数
		AUDIODEV=PA
		SOUND_MODEL=AC97
		audiodev=pa1 ;;
		5) AUDIODEV=PA
			SOUND_MODEL=usb-audio
			audiodev=pa1 ;;
		6) AUDIODEV=ALSA
			SOUND_MODEL=AC97
			audiodev=alsa1                 ;;
		*) AUDIODEV=ALSA
		set -- "${@}" "-device" "intel-hda"
		SOUND_MODEL=hda-duplex
		audiodev=alsa1 ;;
		esac
	if [ -n "${SOUND_MODEL}" ]; then
	set -- "${@}" "-device" "${SOUND_MODEL},audiodev=${audiodev}"
	case ${AUDIODEV} in
		PA)
	set -- "${@}" "-audiodev" "pa,server=127.0.0.1:4713,id=pa1" ;;
	*)
	set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124" ;;
	esac
	fi

		;;
	esac	;;
	esac
	fi
####################
#进阶选项

	echo -e "\n是否进阶选项，包括${YELLOW}共享文件夹、鼠标、启动顺序、时间${RES}等"
	read -r -p "1)是 2)否 " input
	case $input in
        1)

	case $SYS in
		QEMU_ADV)
	case $QEMU_MODE in
		"")
	echo -e "请选择${YELLOW}分区磁盘${RES}加载模式"
	read -r -p "1)加载分区镜像 2)加载双光盘 不加载请直接回车 " input
        case $input in
		1) LIST1
			echo -n -e "请输入${YELLOW}分区镜像${RES}全名,不加载请直接回车（例如hdb.img）: "
	read hdb_name ;;
		2) LIST1
			echo -n -e "请输入${YELLOW}第一个光盘${RES}全名,不加载请直接回车（例如DVD.iso）: "
	read iso1_name ;;
	*) ;;
	esac ;;
	VIRTIO_MODE) ;;
	esac ;;
	*) LIST1
		echo -n -e "请输入${YELLOW}分区镜像${RES}全名,不加载请直接回车（例如hdb.img）: "
	read hdb_name ;;
	esac
	echo -n -e "请输入${YELLOW}光盘${RES}全名,不加载请直接回车（例如DVD.iso）: "
	read iso_name

	echo -e "是否加载${YELLOW}共享文件夹${RES}"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	1) 
	echo -e "\n1) 传统500m容量，只读"
	echo -e "2) mtp协议，可访问整个设备目录，显示部分文件有bug，只读，部分旧系统或精简系统需驱动"
	echo -e "3) 本地网共享"
	echo -e "9) 不加载"
	read -r -p "请选择: " input
	case $input in
	1) SHARE=true ;;
	2) set -- "${@}" "-device" "ich9-usb-ehci1,id=ehci" "-device" "ich9-usb-uhci1,masterbus=ehci.0,multifunction=on" "-device" "usb-mtp,rootdir=${DIRECT}" ;;
	3) if [ ! $(command -v smbcontrol) ]; then
		echo -e "${YELLOW}检测安装支持包${RES}"
		sleep 2
		uname -a | grep 'Android' -q
		if [ $? == 0 ]; then
		sudo_
		pkg install samba --no-install-recommends -y
	else
		$sudo apt install samba --no-install-recommends -y
		fi
	fi
		SMB=",smb=${HOME}/share"
		;;
	*) ;;
	*) esac
	echo "" ;;
	esac
#开全内存balloon功能，俗称内存气球
	echo -e "是否开${YELLOW}全内存balloon${RES}功能(需安装virtio驱动)"
	read -r -p "1)开启 2)不开启 " input
	case $input in
	1) set -- "${@}" "-device" "virtio-balloon-pci" ;;
	*) ;;
	esac
	case $SYS in
	ANDROID) ;;
	*)
	echo -e "是否使用${YELLOW}控制台${RES}调试(部分功能需root用户)${RES}"
        read -r -p "1)使用 2)不使用 " input
        case $input in
	1) 
	if [ ! $(command -v telnet) ]; then
		echo -e "${YELLOW}检测安装支持包${RES}"
		sleep 2
		sudo_
		$sudo apt install telnet -y
	fi
		rm ${HOME}/hugepages* 2>/dev/null
	echo -e "${RED}注意！使用控制台不会因为qemu退出而自动删除大页文件，请退出后输rm ${HOME}/hugepage*自行删除${RES}"
		set -- "${@}" "-monitor" "telnet:127.0.0.1:4444,server,nowait" "-daemonize"
		echo -e "${YELLOW}调试命令telnet 127.0.0.1 4444${RES}\n${YELLOW}#换光盘${RES}：先info block查看光盘标识，例如ide0-cd1，再用命令change ide0-cd1 /sdcard/xinhao/windows/DGDOS.iso\n${YELLOW}#热插拔内存${RES}：本脚本已对默认内存预留两个内存槽$(( $mem_ / 2 ))m\n输入命令\n(qemu) object_add memory-backend-ram,id=mem1,size=$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm0,memdev=mem1\n(qemu) object_add memory-backend-ram,id=mem2,size=$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm,memdev=mem2\n或者大页内存：\n(qemu) object_add memory-backend-file,id=mem1,size=$(( $mem_ / 2 ))m,mem-path=${HOME}/hugepages-$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm1,memdev=mem1输入后可用info memdev或info memory-devices查看\n${YELLOW}#热插拔cpu${RES}：本脚本仅对默认smp的max预留一个cpu槽\n查可用cpu槽info hotpluggable-cpus(找到没有qom_path一组，记住type信息，CPUInstance Properties信息)\n输入格式(以提示为准)：device_add driver=qemu32-i386-cpu,socket-id=2,core-id=0,thread-id=0,node-id=0\n${YELLOW}#退出qemu${RES}，输quit\n"
:<<\eof	
	if [ -z "$mem" ]; then
	set -- "${@}" "-object" "memory-backend-file,id=mem1,size=$(( $mem_ / 2 ))m,mem-path=/mnt/hugepages-$(( $mem_ / 2 ))m"
	set -- "${@}" "-device" "pc-dimm,id=dimm1,memdev=mem1"
	set -- "${@}" "-object" "memory-backend-file,id=mem2,size=$(( $mem_ / 2 ))m,mem-path=/mnt/hugepages-$(( $mem_ / 2 ))m"
	set -- "${@}" "-device" "pc-dimm,id=dimm2,memdev=mem2"
	fi
eof	
:<<\eof
#热插拔内存
(qemu) object_add memory-backend-ram,id=mem0,size=1024m
(qemu) device_add pc-dimm,id=dimm0,memdev=mem0
(qemu) object_add memory-backend-ram,id=mem,size=1G
(qemu) device_add pc-dimm,id=dimm,memdev=mem
#linux宿主机使用大页热插拔内存
(qemu) object_add memory-backend-file,id=mem1,size=1G,mem-path=/mnt/hugepages-1GB
(qemu) device_add pc-dimm,id=dimm1,memdev=mem1
eof


		;;
	*) ;;
	esac
#-L是DOS
#-bios，启动现系统
#-plash，启动UEFI 的BIOS
	echo -e "是否加载${YELLOW}UEFI${RES}"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	1) echo -n -e "请确认UEFI已放进${STORAGE}文件夹内，输入UEFI全名(例如OVMF_CODE.fd)，使用qemu的默认UEFI请直接回车 "
	read UEFI
	if [ -n "$UEFI" ]; then
		set -- "${@}" "-pflash" "${DIRECT}${STORAGE}$UEFI"
	else
#		set -- "${@}" "-pflash" "/usr/share/OVMF/OVMF_CODE.fd"
#		set -- "${@}" "-pflash" "/usr/share/OVMF/OVMF_VARS.fd"
		set -- "${@}" "-drive" "if=pflash,format=raw,file=/usr/share/OVMF/OVMF_CODE.fd,readonly=on"
#OVMF变量（“ VARS”）文件的脚本，该文件中注册了默认的安全启动密钥。 并验证它是否可以正常工作。
		set -- "${@}" "-drive" "if=pflash,format=raw,file=/usr/share/OVMF/OVMF_VARS.fd,readonly=on"
	fi ;;
	*) ;;
#可信平台模块 (TPM) 命令响应缓冲区 （crb）
	esac
:<<\eof
Bus 001 Device 026: ID 0781:5406 SanDisk Corp. Cruzer Micro U3
需先插入usb，lsusb确认Bus(hostbus) 1 Device(hostport) 26
-device usb-ehci,id=ehci
-device usb-host,bus=ehci.0,hostbus=1,hostport=26
-device usb-host,bus=ehci.0,vendorid=0x0781,productid=0x5406
eof
	echo -e "${YELLOW}usb直连${RES}，虚拟机可读u盘(手机需已root并使用root用户)"
	read -r -p "1)通过端口开启 2)通过物理地址开启 0)不使用 " input
        case $input in
		1) if [ ! $(command -v lsusb) ]; then
			echo -e "${YELLOW}检测支持包${RES}"
			sleep 2
			sudo_
			$sudo apt install usbutils -y
		fi
		echo -e "${YELLOW}请插入usb${RES}"
	CONFIRM
	lsusb 2>/dev/null
	if [ $? == 1 ]; then
		echo -e "${RED}未能获取设备信息，请确认已获取系统权限${RES}"
	fi
	read -r -p "输入usb名称对应的bus序号(如001，请输1或者011，请输11) " HOSTBUS
	lsusb -t
	read -r -p "输入Port序号(如001，请输1或者011，请输11) " HOSTPORT
	if echo ${@} | grep -q ehci; then
		printf ""
	else
        set -- "${@}" "-device" "usb-ehci,id=ehci"
	fi
	set -- "${@}" "-device" "usb-host,bus=ehci.0,hostbus=$HOSTBUS,hostport=$HOSTPORT" ;;
	2) if [ ! $(command -v lsusb) ]; then
		echo -e "${YELLOW}检测安装支持包${RES}"
		sleep 2
		sudo_
		$sudo apt install usbutils -y
	fi
		echo -e "\n${YELLOW}请插入usb${RES}"
	CONFIRM
	if echo ${@} | grep -q ehci; then
		printf ""
	else
	set -- "${@}" "-device" "usb-ehci,id=ehci"
	fi
	lsusb 2>/dev/null
	if [ $? == 1 ]; then
	echo -e "${RED}未能获取设备信息，请确认已获取系统权限${RES}"
	fi
	echo -e "${YELLOW}输入usb的ID前段${RES}\n例如，Bus 003 Device 007: ID 0781:5406 SanDisk Corp. Cruzer Micro U3中的\e[1;33m0781${RES}"
	read -r -p "请输入: " VENDORID
	PRODUCTID=`lsusb | grep "$VENDORID" | cut -d ':' -f 3 | cut -d ' ' -f 1`
	echo -e "${GREEN}usb ID为$VENDORID:$PRODUCTID${RES}\n"
	read -r -p "确认ID信息请直接回车，如果usb后段有误，请输入: " PRODUCTID
	if [ -n "$PRODUCTID" ]; then
	set -- "${@}" "-device" "usb-host,bus=ehci.0,vendorid=0x$VENDORID,productid=0x$PRODUCTID"
	echo -e "${GREEN}usb ID为$VENDORID:$PRODUCTID${RES}\n"
	else
	PRODUCTID=`lsusb | grep "$VENDORID" | cut -d ':' -f 3 | cut -d ' ' -f 1`
	set -- "${@}" "-device" "usb-host,bus=ehci.0,vendorid=0x$VENDORID,productid=0x$PRODUCTID"
	fi ;;
        *) ;;
	esac ;;
	esac
	if [ -z "$HUGEPAGE" ]; then
        echo -e "创建${YELLOW}大页文件${RES}代替设备ram，可降低ram使用率，响应速度略降低)${RES}"
        read -r -p "1)加载 2)不加载 " input
        case $input in
        1) HUGEPAGE=true ;;
        *) ;;
        esac
        fi

##################
#S1 =>Standby. 即指说系统处于低电源供应状态,在 windows or BIOS 中可设定屏幕信号输出关闭、硬盘停止运转进入待机状态、电源灯处于闪烁状态.此时动一动鼠标、按键盘任一键均可叫醒电脑.是最耗电的睡眠模式。
#S2 =>Power Standby.和 S1 几乎是一样的.但是睡眠模式比S1更深 不再给CPU供电
#S3 =>Suspend to RAM.即是把 windows 当前存在内存中的所有资料保存不动,然后进入“假关机”.即待机模式
#S4 =>Suspend to Disk. 即是把 windows 内存中的资料完整的保存在硬盘中,等开机时就直接从保存这些资料的地方直接完整的读到内存中,不需要跑一堆应用程序.使用这种模式,硬盘一定要腾出一个完整的连续空间.WinME/2000/XP 在电源管理中休眠的作用就是这个 .
#S5 =>Shutdown.有些部件仍然带电，可被键盘，时钟叫醒
#0关闭1开
	echo -e "${YELLOW}待机休眠管理${RES}(如系统因启用快速启动等存在文件丢失情况，请尝试使用该功能，注意：部分设备可能降低模拟效率)"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	1)
	case $QEMU_SYS in
	qemu-system-i386)
	set -- "${@}" "-global" "PIIX4_PM.disable_s3=1"
	set -- "${@}" "-global" "PIIX4_PM.disable_s4=1" ;;
	*)
	set -- "${@}" "-global" "ICH9-LPC.disable_s3=1"
	set -- "${@}" "-global" "ICH9-LPC.disable_s4=1" ;;
	esac
	;;
	*) ;;
	esac


	echo -e "是否加载${YELLOW}usb鼠标${RES}(提高光标精准度),少部分系统可能不支持"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	2) ;;
	*) set -- "-device" "usb-tablet" "${@}" ;;
	esac
	
	if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [3-4] ]]; then
	echo -e "是否锁定${YELLOW}缓存${RES}"
	read -r -p "1)是 2)否 " input
	case $input in
	1) echo -e "${RED}注意！设置tcg的缓存可以提高模拟效率，以m为单位，跟手机闪存ram也有关系(调高了会出现后台杀)，请谨慎设置${RES}"
	echo -n -e "请输入拟缓存的数值(以m为单位，例如1800)，回车为默认值，请输入: "
	read TB
	if [ -n "$TB" ]; then
	set -- "${@}" "-tb-size" "$TB"
	else
	set -- "${@}" "-tb-size" "$(( $mem_ / 2 ))"
        fi ;;
	*) ;;
	esac
	fi


#时间设置，RTC时钟，用于提供年、月、日、时、分、秒和星期等的实时时间信息，由后备电池供电，当你晚上关闭系统和早上开启系统时，RTC仍然会保持正确的时间和日期
#driftfix=slew i386存在时间漂移
#       *) set -- "${@}" "-rtc" "base=`date +%Y-%m-%dT%T`" ;;
#"base=$(TZ=UTC-8 date +%Y-%m-%dT%T)"
#strict=on|off 是否受宿主机网络控制
	echo -e "请选择${YELLOW}启动顺序${RES}"
	read -r -p "1)优先硬盘启动 2)优先光盘启动 " input
	case $input in
	1|"") set -- "${@}" "-boot" "order=cd,menu=on,strict=off" ;;
	*) set -- "${@}" "-boot" "order=dc,menu=on,strict=on" ;;
	esac
	set -- "${@}" "-rtc" "base=localtime" ;;
	*)
        set -- "${@}" "-rtc" "base=localtime"
        set -- "${@}" "-boot" "order=cd,menu=on,strict=off"
        set -- "-device" "usb-tablet" "${@}"
        ;;
	esac

################
	set -- "${@}" "-cpu" "${CPU_MODEL}"
		if [ -n "$_SMP" ]; then
			set -- "${@}" "-smp" "${_SMP}"
		elif [ -n "$CPU" ]; then
			set -- "${@}" "-smp" "${CPU}"
		else
			if echo "${@}" | grep -q daemonize; then
				set -- "${@}" "-smp" "${MAXCPUS}"
			else
			if [ -n "$LOW_CORE" ]; then
				set -- "${@}" "-smp" "$LOW_CORE"
			else
				set -- "${@}" "-smp" "${SMP_}"
			fi
		fi
	fi
#单内存槽
:<<\eof
	if [ -n "$mem" ]; then
	set -- "${@}" "-m" "$mem"
	else
	if echo ${@} | grep -q daemonize; then
	set -- "${@}" "-m" "$mem_,slots=2,maxmem=$(( $mem_ * 2 ))m"
	else
	set -- "${@}" "-m" "$mem_"
	fi
	fi
eof
#双内存槽
	if [ -n "$mem" ]; then
	set -- "${@}" "-m" "$mem"
	else
	if [[ $(qemu-system-i386 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-4] ]]; then
	set -- "${@}" "-m" "$mem_"
	else
	if echo "${@}" | grep -q daemonize; then
	set -- "${@}" "-m" "$mem_,slots=2,maxmem=$(( $mem_ * 2 ))m"
	else
	set -- "${@}" "-m" "$mem_"
	fi
	fi
	fi
	case $HUGEPAGE in
		true)
######
#单内存分配
	if [ -n "$mem" ]; then
	set -- "${@}" "-object" "memory-backend-file,id=pc.ram,size=${mem}m,mem-path=${HOME}/hugepage,prealloc=on,share=on"
	else
	set -- "${@}" "-object" "memory-backend-file,id=pc.ram,size=${mem_}m,mem-path=${HOME}/hugepage,prealloc=on,share=on"
	fi
		set -- "${@}" "-numa" "node,memdev=pc.ram"
		HMAT=",hmat=on"
	;;
#		*) set -- "${@}" "-mem-prealloc" ;;
	esac
#################
	if [ -n "${NET_MODEL}" ]; then
	set -- "${@}" "-device" "${NET_MODEL}"
	set -- "${@}" "-netdev" "user,id=user0,ipv6=off"
	elif [ -n "$NET_MODEL0" ]; then
	set -- "${@}" "-net" "${NET_MODEL0}"
	set -- "${@}" "-net" "user$SMB"
	elif [ -n "$NET_MODEL1" ]; then
	set -- "${@}" "-device" "${NET_MODEL1}"
	set -- "${@}" "-netdev" "user,ipv6=off,id=user0$SMB"
	else
	set -- "${@}" "-net" "none"
	fi
#################

#不加载默认的配置文件。默认会加载/use/local/share/qemu下的文件，通常模拟器默认加载串口，并口，软盘，光驱等。
	set -- "${@}" "-nodefaults"
#不加载用户自定义的配置文件。
	set -- "${@}" "-no-user-config"
#       set -- "${@}" "-k" "en-us"
#OHCI1.1 UHCI1.1 EHCI2.0 XHCI3.0
#USB2.0
#-device ich9-usb-ehci1,id=usb
#-device ich9-usb-uhci1,masterbus=usb.0,firstport=0,multifunction=on
#USB3.0
#-device nec-usb-xhci,id=usb -device usb-storage,bus=usb.0,drive=disk1 -drive if=none,format=raw,id=disk1,file=/sdcard/xinhao/windows/4g.qcow2
#-device nec-usb-xhci
#       set -- "${@}" "-usbdevice" "keyboard"
	case $ARCH in
		tablet)
#重定向虚拟串口到主机设备
#       set -- "${@}" "-serial" "none"
#重定向虚拟并口到主机设备
#       set -- "${@}" "-parallel" "none"
#控制台，一种类似于shell的交互方式
#       set -- "${@}" "-monitor" "none"
;;
		*) ;;
	esac
#qemu monitor protocol协议，对qemu虚拟机进行交互
#       set -- "${@}" "-qmp" "tcp:127.0.0.1:4444,server,nowait" "-monitor" "none"
#使用bios配置
#       set -- "${@}" "-L" "${DIRECT}${STORAGE}"
#使用bzImage内核镜像
#       set -- "${@}" "-kernel" "bzImage"
#使用cmdline作为内核命令行
#       set -- "${@}" "-append" "cmdline"
	case $QEMU_SYS in
		qemu-system-i386)
#取消高精度定时器
	set -- "${@}" "-no-hpet"
#取消软盘启动检测
	set -- "${@}" "-no-fd-bootchk"
#取消高级配置与电源管理
#        set -- "${@}" "-no-acpi"
	;;
		*) ;;
	esac
#更改消息的格式，时间戳
	set -- "${@}" "-msg" "timestamp=off"

#################
	if echo "${@}" | grep -q usb 2>/dev/null; then
		USB=on
	else
		USB=off
	fi
	if [[ $(qemu-system-i386 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-4] ]]; then
	unset HMAT
	fi
	echo -e "请选择${YELLOW}计算机类型${RES}，默认pc，因系统原因，q35可能导致启动不成功"
	MA="vmport=off,dump-guest-core=off,mem-merge=off,kernel-irqchip=off,usb=$USB"
#enforce-config-section=on
	TCG="tcg,thread=multi"
	case $SYS in
		QEMU_PRE)
	read -r -p "1)pc 2)q35 " input
	;;
	*)
	read -r -p "1)pc 2)q35 " input
	;;
	esac
	case $input in
	2) PC=q35 ;;
	3) case $SYS in
		QEMU_PRE) PC=pc ;;
	*) echo -e "${GREEN}本选项只针对性做了提高开机速度的${YELLOW}概率${RES}，效率并不比qemu5.0以下版本的高(磁盘接口建议选ide)${RES}\n"
                PC=pc-i440fx-3.1
	sleep 1
	;;
	esac ;;
	*) PC=pc ;;
	esac
	case $(dpkg --print-architecture) in
		arm*|aarch64)
	case $SYS in
		QEMU_PRE) set -- "-machine" "$PC,usb=$USB" "--accel" "$TCG" "${@}" ;;
		*)
	if [ $PC == pc-i440fx-3.1 ]; then
		set -- "-machine" "$PC,$MA$HMAT" "-global" "PIIX4_PM.disable_s3=1" "-global" "PIIX4_PM.disable_s4=1" "-global" "migration.send-configuration=on" "--accel" "$TCG" "${@}"
	else
		echo -e "\n请选择${YELLOW}加速${RES}方式(理论上差不多，但貌似指定tcg更流畅点，请自行体验)"
	read -r -p "1)tcg 2)自动检测 3)锁定tcg缓存 " input
	case $input in
		1)
		set -- "-machine" "$PC,$MA$HMAT" "--accel" "$TCG" "${@}" ;;
		3) if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [4-9] ]]; then
		echo -e "${RED}注意！设置tcg的缓存可以提高模拟效率，以m为单位，跟手机闪存ram也有关系(调高了会出现后台杀)，请谨慎设置${RES}"
		echo -n -e "请输入拟缓存的数值(以m为单位，例如1800)，回车为默认值，请输入: "
        read TB
        if [ -n "$TB" ]; then
		set -- "-machine" "$PC,$MA$HMAT" "--accel" "$TCG,tb-size=$TB" "${@}"
        else
		set -- "-machine" "$PC,$MA$HMAT" "--accel" "$TCG,tb-size=$(( $mem_ / 2 ))" "${@}"
	fi
	else
		set -- "-machine" "$PC,$MA$HMAT" "--accel" "$TCG" "${@}"
	fi ;;
	*) set -- "-machine" "$PC,accel=kvm:xen:hax:tcg,$MA$HMAT" "${@}" ;;
	esac
	fi ;;
	esac ;;
	*)
		set -- "-machine" "$PC,accel=kvm:xen:hax:tcg,usb=$USB,dump-guest-core=on$HMAT" "${@}" ;;
	esac
#################
cat >/dev/null<<EOF
if=INTERFACE：指定驱动器接口类型，可用的有：ide，scsi，sd，mtd，floopy，pflash，virtio等
bus=BUS NUM，unit=UNIT NUM：设置驱动器在客户机中的总线编号和单元编号
index=INDEX NUM：设置在同一种接口的驱动器中的索引编号
media=disk|cdrom：设置驱动器中媒介的类型，其值为“disk”或“cdrom”
snapshot=on|off：是否启用镜像功能，启用时，qemu不会将磁盘数据的更改写回镜像文件中，而是写到临时文件，也可以在qemu         monitor中使用commit命令强制将磁盘数据的更改保存回镜像文件中
cache=writethrough|writeback|（none|off）
writethrough：默认值，即直写模式，它是在调用write写入数据的同时将数据写入磁盘缓存和后端块设备中，优点：操作简单，缺点：写入数据速度较慢
writeback：回写模式，将数据写入到磁盘缓存中即返回，只有数据被换出缓存的时候才写入到后端块设备中，优点：写入速度快，缺点：可能造成数据丢失
（none|off）：设置none或者off表示不写入缓存，直接写入到块设备中，优点是数据安全，缺点:速度太慢
aio=threads|native：默认threads，即让一个线程池去处理异步io；而native只适用于cache=none的情况，就是使用linux原生的aio
format=FORMAT：使用的磁盘格式，默认qemu是自动检测磁盘格式的
serial=SERIAL NUM：分配给设备的序列号
add=ADDR:分配给驱动器控制器的pci地址，该选项只有在使用virtio接口才适用
id=NAME：设置该驱动器的id，这个id可以在qemu monitor中用info block命令查看
EOF
	case $QEMU_MODE in
		VIRTIO_MODE)
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,if=ide"
		set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,if=virtio,format=raw"
		set -- "${@}" "-cdrom" "${DIRECT}${STORAGE}$iso_name" ;;
		*)
		echo -e "请选择${YELLOW}磁盘接口${RES},因系统原因,sata可能导致启动不成功,virtio需系统已装驱动,回车为兼容方式"
	read -r -p "1)ide 2)sata 3)virtio " input
	case $input in
##################
#IDE			
		1)
	if [ $PC != pc-i440fx-3.1 ]; then
	AIO=",aio=threads,cache=writeback"
	fi
#		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,if=ide,index=0,media=disk,aio=threads,cache=writeback"
set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,if=ide,index=0,media=disk$AIO"
	if [ -n "$hdb_name" ]; then
set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hdb_name,if=ide,index=1,media=disk$AIO"
	fi
	if [ -n "$iso1_name" ]; then
	set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso1_name,if=ide,media=cdrom,index=1$AIO"
	fi
	if [ -n "$iso_name" ]; then 
	       set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,if=ide,media=cdrom,index=2$AIO"
	fi
	case $SHARE in
		true) set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,if=ide,index=3,media=disk,format=raw" ;;
		*) ;;
	esac ;;
	2)

##################
#SATA        
	set -- "${@}" "-drive" "id=disk,file=${DIRECT}${STORAGE}$hda_name,if=none"
	set -- "${@}" "-device" "ahci,id=ahci"
	set -- "${@}" "-device" "ide-hd,drive=disk,bus=ahci.0"
	set -- "${@}" "-global" "ide-hd.physical_block_size=1024"
	if [ -n "$hdb_name" ]; then
	set -- "${@}" "-drive" "id=installmedia,file=${DIRECT}${STORAGE}$hdb_name,if=none"
	set -- "${@}" "-device" "ide-hd,drive=installmedia,bus=ahci.1"
	fi
	if [ -n "$iso1_name" ]; then
#               set -- "${@}" "-cdrom" "${DIRECT}${STORAGE}$iso1_name"
	set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso1_name,if=ide,media=cdrom,index=2"
	fi
	if [ -n "$iso_name" ]; then
	set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,if=ide,media=cdrom,index=1"
	fi
	case $SHARE in
		true)
		set -- "${@}" "-drive" "if=none,format=raw,id=disk1,file=fat:rw:${DIRECT}/xinhao/share/"
		set -- "${@}" "-device" "usb-storage,drive=disk1"
		;;
	*) ;;
	esac ;;
##################
#VIRTIO

	3) set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,index=0,media=disk,if=virtio"
	if [ -n "$hdb_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hdb_name,index=1,media=disk,if=virtio"
	fi
	if [ -n "$iso1_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso1_name,if=ide,media=cdrom,index=1"
	fi
		if [ -n "$iso_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,if=ide,media=cdrom,index=2"
		fi
	case $SHARE in
		true)
		set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,index=3,media=disk,if=virtio,format=raw"
;;
		*) ;;
	esac ;;
##################
##################
#hda
		*) set -- "${@}" "-hda" "${DIRECT}${STORAGE}$hda_name" 
	if [ -n "$hdb_name" ]; then
		set -- "${@}" "-hdb" "${DIRECT}${STORAGE}$hdb_name"
	fi
	if [ -n "$iso_name" ]; then
		set -- "${@}" "-cdrom" "${DIRECT}${STORAGE}$iso_name"
		fi
	case $SHARE in
		true)
		set -- "${@}" "-hdd" "fat:rw:${DIRECT}/xinhao/share/" ;;
		*) ;;
	esac ;;
	esac ;;
	esac

####################
#test
: <<\eof
BIOS Information BIOS信息(Type 0)
System Information 系统信息(Type 1)
Baseboard (or Module) Information 、基板（或模块）信息(Type 2)
System Enclosure or Chassis 系统外围或底架 (Type 3)
Processor Information 处理器信息(Type 4)
Memory Controller Information 存储控制器信息(已废弃)(Type 5, Obsolete)
Memory Module Information 、存储模块信息(已废弃)(Type 6, Obsolete)
调整缓存信息(Type7)
端口连接器信息(Type8)
系统插槽(Type9)
OEM Strings (Type 11)
从SMBIOS 2.3版本开始，兼容SMBIOS的实现必须包含以下10个数据表结构：BIOS信息(Type 0)、系统信息(Type 1)、系统外围或底架(Type 3)、处理器信息(Type 4)、高速缓存信息(Type 7)、系统插槽(Type 9)、物理存储阵列(Type 16)、存储设备(Type 17)、存储阵列映射地址(Type 19)、系统引导信息(Type 32)。
eof
####################
#cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq
#kernel-irqchip=on|off|split中断控制器，如果可用，控制内核对irqchip的支持。仅kvm
#vmport=on|off|auto为vmmouse等 启用VMWare IO端口的仿真，默认开
#dump-guest-core=on|off将客户机内存包括在核心转储中，类似于dump日志。默认为开。
#tb-size=n (TCG translation block cache size)，Controls the size (in MiB) of the TCG translation block cache.Host instruction codes are stored in code_gen_buffer[]. The default buffer size is32MB.(Ram_size/4, while ram_size default value is 128MB).
#mem-merge=on|off启用或禁用内存合并支持。主机支持时，此功能可在VM实例之间重复删除相同的内存页面（默认情况下启用）。
#aes-key-wrap=on|off在s390-ccw主机上 启用或禁用AES密钥包装支持。此功能控制是否将创建AES包装密钥以允许执行AES加密功能。默认为开。
#dea-key-wrap=on|off在s390-ccw主机上 启用或禁用DEA密钥包装支持。此功能是否DEA控制，默认开
#NUMA（Non Uniform Memory Access Architecture）技术可以使众多服务器像单一系统那样运转，同时保留小系统便于编程和管理的优点。
########################
	if [ -n "$display" ]; then
	case $display in
		wlan_vnc) set -- "${@}" "-display" "vnc=$IP:0" ;;
		vnc) 
		set -- "${@}" "-display" "vnc=127.0.0.1:0,lossy=on,non-adaptive=on"
		export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		xvnc)
		export PULSE_SERVER=tcp:127.0.0.1:4713
		if echo "${@}" | grep ! 'gl=on'; then
		set -- "${@}" "-display" "gtk,gl=off"
		fi
		;;
		xsdl) 
			export DISPLAY=127.0.0.1:0
			export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		wlan_spice) set -- "${@}" "-spice" "port=5900,addr=$IP,disable-ticketing=on"
			export PULSE_SERVER=tcp:$IP:4713 ;;
		spice) set -- "${@}" "-spice" "port=5900,addr=127.0.0.1,disable-ticketing=on,seamless-migration=off"
			export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		spice_) set -- "${@}" "-spice" "port=5900,addr=127.0.0.1,disable-ticketing=on,seamless-migration=off"
			set -- "${@}" "-device" "virtio-serial-pci" "-device" "virtserialport,chardev=spicechannel0,name=com.redhat.spice.0" "-chardev" "spicevmc,id=spicechannel0,name=vdagent"
			export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		amd) set -- "${@}" "-display" "gtk,gl=off" ;;
		gtk_) set -- "${@}" "-display" "gtk,gl=off"
	env | grep 'PULSE_SERVER' -q
	if [ $? != 0 ]; then
	export PULSE_SERVER=tcp:127.0.0.1:4713
	fi ;;
	esac
	fi

        set -- "$QEMU_SYS" "${@}"
#	uname -a | grep 'Android' -q 
#	if [ $? != 0 ]; then
		case $display in
		wlan_vnc|wlan_spice) ;;
		xvnc)
#	trap "pkill Xtightvnc; pkill Xtigervnc; pkill Xvnc; pkill websockify 2>/dev/null;exit" SIGINT EXIT
	printf "%s\n"
cat <<-EOF
${@}
EOF
	echo -e "${RES}"
	if echo "${@}" | grep -q daemonize; then
		echo -e "\n${YELLOW}调试命令：telnet 127.0.0.1 4444${RES}"
	elif echo "${@}" | grep -q hugepage 2>/dev/null; then
	echo -e "${GREEN}注意！你使用了大页内存，脚本会qemu退出而自动删除大页文件，如未删除请退出后输rm ${HOME}/hugepage*自行删除${RES}${RES}"
	trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	fi
	sleep 2
	if [ "$NOVNC" == novnc ]; then
echo -e "本地vncviewer地址 \e[33m127.0.0.1:0\e[0m"
echo -e "局域网vncviewer地址 \e[33m$IP:0\e[0m"
echo -e "浏览器输 \e[33mhttp://localhost:6080/vnc.html\e[0m\n(浏览器适合没vnc，大平板，点击触屏)"
echo -e  "${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}"
	sleep 1
	XVNC
	"${@}" &
	bash /usr/share/novnc/utils/launch.sh --vnc localhost:5900 --listen 6080 2>/dev/null
	else
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0\n本次操作将退出脚本，请留意shell提示\n显卡驱动装上后在开机过程中shell会有${YELLOW}gl_version 45 - core profile enabled${GREEN}提示，请及时在显示界面左上角窗口(View)切换为virtio-gpu-pci，否则启动失败${RES}"
	XVNC
	"${@}" &
	fi
	exit 0 ;;
		*)
	echo -e "创建本次参数的${YELLOW}快捷脚本${RES}"
	read -r -p "1)是 2)否 " input
	case $input in
		1) echo -n "请给脚本起个名字: "
	read script_name

uname -a | grep 'Android' -q
if [ $? == 0 ]; then
	if [ ! -d ${HOME}/short_qemu ]; then
	mkdir ${HOME}/short_qemu
	fi
	cat >${HOME}/short_qemu/$script_name <<-EOF
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
${@}
EOF
else
	case $display in
		xsdl)
cat >/usr/local/bin/$script_name <<-EOF
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
export DISPLAY=127.0.0.1:0
${@}
EOF
;;
		vnc|spice|spice_) 
cat >/usr/local/bin/$script_name <<-EOF
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
${@}
EOF
;;
		gtk_)
cat >/usr/local/bin/$script_name <<-EOF
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
${@}
EOF
;;
		amd)
cat >${HOME}/xinhao/$script_name <<-EOF
pkill -9 qemu-system-x86 2>/dev/null
pkill -9 qemu-system-i38 2>/dev/null
${@}
EOF
	chmod +x ${HOME}/xinhao/$script_name
	echo -e "已保存本次参数的脚本，下次可直接输${GREEN}$script_name${RES}启动qemu"
	sleep 2
        printf "%s\n"
cat <<-EOF
${@}
EOF
	echo -e "${GREEN}启动模拟器\n"
	if echo "${@}" | grep -q "smb="; then
	echo '如共享目录成功加载，请在地址栏输 \\10.0.2.4'
	fi
	echo -e "${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}"
	if echo "${@}" | grep -q daemonize; then
	echo -e "\n${YELLOW}调试命令：telnet 127.0.0.1 4444${RES}"
	else
	trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	fi
	sleep 1
	"${@}" >/dev/null 2>>${HOME}/.utqemu_log
	if [ $? == 1 ]; then
	FAIL
	echo -e "${RED}启动意外中止，请查看日志d(ŐдŐ๑)${RES}\n"
	fi
	exit 0 ;;


	esac
fi
	chmod +x /usr/local/bin/$script_name 2>/dev/null || chmod +x ${HOME}/short_qemu/$script_name 2>/dev/null
	echo -e "已保存本次参数的脚本，下次可直接输${GREEN}$script_name${RES}启动qemu"
	sleep 2 ;;
		*)
	;;
	esac ;;
	esac
#	fi
	printf "%s\n"
	cat <<-EOF
	${@}
	EOF
	case $display in
		vnc) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0\n" 
am start -n com.realvnc.viewer.android/com.realvnc.viewer.android.app.ConnectionChooserActivity >/dev/null 2>&1 ;;
		wlan_vnc) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer $IP:0\n" ;;
		xsdl) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开xsdl\n" ;;
		wlan_spice) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开aspice $IP 端口 5900(部分aspice app可能默认未勾选声音，如无声音请检查打开)\n" ;;
		spice|spice_) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开aspice 127.0.0.1 端口 5900(部分aspice app可能默认未勾选声音，如无声音请检查打开)\n" ;;
		*) printf "%s\n${GREEN}启动模拟器\n" ;;
	esac
	uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
		if echo "${@}" | grep -q "smb="; then
	echo '如共享目录成功加载，请在地址栏输 \\10.0.2.4'
		fi
	else
	am start -n com.realvnc.viewer.android/com.realvnc.viewer.android.app.ConnectionChooserActivity >/dev/null 2>&1
	fi
	if echo "${@}" | grep -q daemonize; then
	echo -e "\n${YELLOW}调试命令：telnet 127.0.0.1 4444${RES}"
	else
#		trap "pkill Xtightvnc; pkill Xtigervnc; pkill Xvnc; pkill websockify 2>/dev/null; exit" SIGINT EXIT
	trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	if echo "${@}" | grep -q hugepage 2>/dev/null; then
	echo -e "${GREEN}你使用了大页内存，开始模拟器前需要时间创建同内存大小文件，文件会在qemu退出后自动删除${RES}"
	fi
	fi
	echo -e  "${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}"
	sleep 1
	if [ "$display" == xvnc ]; then
		XVNC
	fi
	"${@}" >/dev/null 2>>${HOME}/.utqemu_log
	if [ $? == 1 ]; then
		FAIL
	printf "%s${RED}启动意外中止，请查看日志d(ŐдŐ๑)${RES}\n"
	fi
	exit 0
}


#############################
VIRTIO() {

	echo -e "
1) 下载virtio驱动光盘"
	case $SYS in
		ANDROID) ;;
		*)
echo -e "2) 为磁盘接口添加virtio驱动（维基指导模式，需另外下载virtio驱动光盘）
3) 为磁盘接口添加virtio驱动（自定义模式，加载virtio驱动光盘)" ;;
	esac
	echo -e "8) 关于virtio
9) 返回主目录
0) 退出\n"

	read -r -p "请选择: " input
	case $input in
		1) read -r -p "1)下载virtio驱动 2)下载virtio显卡驱动 9)返回 " input
			case $input in
		1) echo -e "${YELLOW}即将下载，下载速度可能比较慢，你也可以复制下载链接通过其他方式下载${RES}\n\n正在检测下载地址..."
	DATE=`date +"%Y"`
	FED_CURL="https://fedorapeople.org/groups/virt/virtio-win/direct-downloads/archive-virtio/"
	VERSION=`curl -s ${FED_CURL} | grep virtio-win | grep $DATE |tail -n 1 | cut -d ">" -f 3 | cut -d "<" -f 1`
	if [ ! -n "$VERSION" ]; then
		unset DATE
		DATE=`date -d "-1 year" +%Y`
		VERSION=`curl -s ${FED_CURL} | grep virtio-win | grep $DATE |tail -n 1 | cut -d ">" -f 3 | cut -d "<" -f 1`
	fi
	VERSION_=`curl ${FED_CURL}$VERSION | grep iso | cut -d ">" -f 3 | cut -d "<" -f 1 | head -n 1`
	echo "$VERSION_" | grep iso -q
	if [ $? -ne 0 ]; then
	echo -e "${RED}无法连接地址${RES}"
	sleep 2
                QEMU_SYSTEM
        else
        echo -e "${YELLOW}下载地址链接为\n\n${GREEN}${FED_CURL}$VERSION$VERSION_${RES}\n"
	read -r -p "1)下载 9)返回 " input
	case $input in
		1)
		curl -O ${FED_CURL}$VERSION$VERSION_
	if [ -f $VERSION_ ]; then
	echo -e "移到${DIRECT}${STORAGE}目录中..."
	mv -v $VERSION_ ${DIRECT}${STORAGE}
	if [ -f ${DIRECT}${STORAGE}$VERSION_ ]; then
		echo -e "\n已下载至${DIRECT}${STORAGE}目录"
		sleep 2
	fi
	else
	echo -e "\n${RED}错误，请重试${RES}"
	sleep 2 
	fi ;;
	*) ;;
	esac
	unset VERSION
	QEMU_SYSTEM
	fi
	;;
2)
	if [ ! -f ${DIRECT}${STORAGE}virtio-gpu-wddm-dod.iso ]; then
	echo -e "\n${GREEN}正在下载virtio显卡驱动盘${RES}"
	curl -O https://shell.xb6868.com/ut/gpu.tar.gz
        tar zxvf gpu.tar.gz
	mv virtio-gpu-wddm-dod.iso ${DIRECT}${STORAGE}
	rm gpu.tar.gz
	echo -e "\n已下载virtio显卡至${DIRECT}${STORAGE}目录，名为virtio-gpu-wddm-dod.iso"
	fi
	sleep 2 ;;
	*) ;;
	esac
	QEMU_SYSTEM
                ;;

	2) case $SYS in
		ANDROID) INVALID_INPUT && VIRTIO ;;
		*)
		echo -e "\n${GREEN}本次操作默认vnc输出，地址127.0.0.1:0\n请确认系统镜像与virtio驱动盘已放入手机目录${STORAGE}里${RES}"
	CONFIRM
	if [ ! -e "${DIRECT}${STORAGE}" ]; then
		echo -e "\n${RED}请选创建windows镜像目录及共享目录，并把系统镜像与驱动盘放入该目录${RES}\n"
		sleep 2
		QEMU_SYSTEM
	fi
	pkill -9 qemu-system-x86 2>/dev/null
	pkill -9 qemu-system-i38 2>/dev/null
	if [ ! -e "${DIRECT}${STORAGE}fake.qcow2" ]; then
	echo -e "\n将为你创建一个新的磁盘镜像，用于搜索virtio驱动\n"
	sleep 2
	qemu-img create -f qcow2 ${DIRECT}${STORAGE}fake.qcow2 1G 2>/dev/null
	if [ -e "${DIRECT}${STORAGE}fake.qcow2" ]; then
	echo -e "\n${GREEN}已创建fake.qcow2磁盘镜像${RES}"
	else
	echo -e "创建失败，请重试"
	sleep 2
	QEMU_SYSTEM
	fi
	fi
	LIST
	HDA_READ
	echo -n -e "请输入${YELLOW}virtio驱动盘${RES}全名,（例如virtio-win-0.1.185.iso）: "
        read iso_name
	echo -e "\e[33m即将开机，参数是默认的，开机过程会比较慢，Windows会自动检测fake磁盘，并搜索适配的驱动。如果失败了，前往Device Manager，找到SCSI驱动器（带有感叹号图标，应处于打开状态），点击Update driver并选择虚拟的CD-ROM。不要定位到CD-ROM内的文件夹了，只选择CD-ROM设备就行，Windows会自动找到合适的驱动的。完成后请关机，然后正常启动qemu-system-x86_64(qemu-system-i386)方式并选择磁盘接口virtio。${RES}"
	CONFIRM
	qemu-system-x86_64 -m 1g -drive file=${DIRECT}${STORAGE}$hda_name,if=ide -drive file=${DIRECT}${STORAGE}fake.qcow2,if=virtio -cdrom ${DIRECT}${STORAGE}$iso_name -vnc :0 2>>${HOME}/.utqemu_log
	exit 0 ;;
	esac ;;
	3) case $SYS in
		ANDROID) INVALID_INPUT && VIRTIO ;;
		*) echo -e "\n${GREEN}你选择了磁盘接口virtio驱动安装模式，此模式下的系统磁盘接口为ide，共享文件接口为virtio，请务必准备好virtio驱动光盘\n如启动安装失败，也请在(VIRTIO驱动相关)选项中进行兼容启动安装${RES}"
	CONFIRM
	QEMU_MODE=VIRTIO_MODE
	START_QEMU ;;
	esac ;;
	8) ABOUT_VIRTIO ;;
	9) QEMU_SYSTEM ;;
	0) exit 0 ;;
	*) INVALID_INPUT && VIRTIO ;;
	esac
}
###################
SOURCE() {
echo -e "1) 换源
9) 返回"
	read -r -p "请选择: " input
	case $input in
		1) read -r -p "1)北外源 2)清华源 9)返回 " input
	case $input in
	1) if [ -d /data/data/com.termux/files/usr/etc/termux/mirrors/china ]; then
		rm -rf /data/data/com.termux/files/usr/etc/termux/chosen_mirrors
		mkdir /data/data/com.termux/files/usr/etc/termux/chosen_mirrors
		ln -svf /data/data/com.termux/files/usr/etc/termux/mirrors/china/mirrors.bfsu.edu.cn /data/data/com.termux/files//usr/etc/termux/chosen_mirrors
	fi
		sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list && pkg update ;;
	2) if [ -d /data/data/com.termux/files/usr/etc/termux/mirrors/china ]; then
		rm -rf /data/data/com.termux/files/usr/etc/termux/chosen_mirrors
		ln -svf /data/data/com.termux/files/usr/etc/termux/mirrors/china /data/data/com.termux/files/usr/etc/termux/chosen_mirrors
	fi
		sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list && pkg update ;;
	*) MAIN ;;
	esac
	pkg update ;;
	*) ;;
        esac
	MAIN
}
###################
LOGIN_() {
	echo -e "\n\e[33m请选择qemu-system-x86的运行环境\e[0m\n
	1)  直接运行，termux(utermux)目前版本为7.0以上，由于termux源的qemu编译的功能不全，强烈建议在容器上使用qemu，\e[33m其他系统的版本各不一样，一些功能参数可能没被编译进去${RES}
	2)  容器内运行qemu-system-x86
	3)  换源(如果无法安装或登录请尝试此操作)
	4)  安装linux-bullseye(预安装xfce4 vlc chromium)
	5)  安装运行轻量版容器+qemu(qemulite)
	6)  体验box64+box86+wine运行exe(整个容器约2.2g)
	10) 下载新版termux
	0) 退出\n"
	read -r -p "请选择: " input
	case $input in
	1) QEMU_SYSTEM ;;
	2) uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	DEBIAN=sid
	sys_name=sid-qemu
	if [ -d $(pwd)/sid-qemu ]; then
		LOGIN
	else
		SYS_DOWN
		LOGIN
	fi
		fi ;;
	3) SOURCE ;;
	4) bash -c "$(curl https://shell.xb6868.com/ut/bullseye.sh)" ;;
	5) bash -c "$(curl https://shell.xb6868.com/ut/qemulite.sh)" ;;
	6) bash -c "$(curl https://shell.xb6868.com/wine/boxwine.sh)" ;;
	10) echo -e "\n${YELLOW}检测最新版本${RES}"
        VERSION=`curl https://f-droid.org/packages/com.termux/ | grep apk | sed -n 2p | cut -d '_' -f 2 | cut -d '"' -f 1`
        echo -e "\n下载地址\n${GREEN}https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/com.termux_$VERSION${RES}\n"
        read -r -p "1)下载 9)返回 " input
        case $input in
                1) rm termux.apk 2>/dev/null
        curl https://mirrors.tuna.tsinghua.edu.cn/fdroid/repo/com.termux_$VERSION -o termux.apk
        mv -v termux.apk ${DIRECT}${STORAGE}
        echo -e "\n已下载至${DIRECT}${STORAGE}目录"
        sleep 2 ;;
        *) ;;
        esac
        unset VERSION
	clear
	LOGIN_ ;;
	0) trap " rm ${HOME}/hugepage* 2>/dev/null;exit" SIGINT EXIT
	exit 0 ;;
	*) INVALID_INPUT
		MAIN ;;
	esac
}
####################
MAIN(){
ARCH_CHECK
MEM
#QEMU_VERSION
SYSTEM_CHECK
uname -a | grep 'Android' -q
if [ $? == 0 ]; then
INFO
LOGIN_
else
QEMU_SYSTEM
fi
}
####################
MAIN "$@"
