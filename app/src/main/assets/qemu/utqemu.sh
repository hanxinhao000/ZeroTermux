#!/usr/bin/env bash
cd $(dirname $0)
####################
INFO() {
	clear
	UPDATE="2021/08/20"
	printf "${YELLOW}更新日期$UPDATE 更新内容${RES}
	增加qemu6.0源地址下载，当选择支持qemu5.0以上版本容器安装qemu，会提示是否更新6.0还是继续使用5.2，也可以在维护更新系统安装
	增加大页文件创建，相当于虚拟内存，降低设备ram占用率，触发选项是内存设置高于默认值，或者进入进阶选项
	加入了看到与看不到的选项
	为方便配置-machine(-M)参数，相关选项与accel加速选项移至磁盘接口后面
	修正电脑上创建使用快捷脚本
	增加测试本机cpu支持模拟的cpu特性
	新增本地共享文件夹，主目录下share，由于镜像原因，可能部份镜像不支持
	增加了一些未经完全测试通过的参数配置
	修改了一些细节\n"
}
###################
NOTE() {
	clear
	printf "${YELLOW}注意事项${RES}
	最近新增的内容比较多，如不能正常加载，请选择1重新安装qemu
	大页文件虽然可以分担设备ram，但同时会提高设备cpu负担，且创建大容量文件，请审慎使用
	本脚本是方便大家简易配置，所有参数都是经多次测试通过，可运行大部分系统，由于兼容问题，性能不作保证，专业玩家请自行操作。
	qemu5.0前后版本选项参数区别不大，主要在于新版本比旧版多了些旧版本没有的参数。
	xp玩经典游戏(如星际争霸，帝国时代)需使用cirrus显卡才能运行
	模拟效率，因手机而异，我用的是华为手机，termux(utermux)在后台容易被停或降低效率。通过分屏模拟的效果是aspice>vnc>xsdl，win8听歌流畅。
	q35主板与sata，virtio硬盘接口由于系统原因，可能导致启动不成功。
	声音输出（不支持termux与utermux环境，pc建议ac97，q35建议hda）。
	sdl输出显示，源地址并未编译qemu的sdl，这里只是通过信号输出，需先开启xsdl(不支持termux与utermux环境）。
	qemu5.0以下模拟xp较好，qemu5.0以上对win7以上模拟较好。\n"
	if [ $(command -v qemu-system-x86_64) ]; then
		echo -e "\e[33m检测到你已安装qemu-system-x86，版本是\e[0m"
		echo -e "\e[32mQEMU emulator version $(echo $qemu_ver | awk '{print $4}')\e[0m"
	else
	echo -e "\e[1;31m检测到你未安装qemu-system-x86，请先选择安装\e[0m"
	fi
}
###################
: <<\eof
#curl -o ${PREFIX}/bin/utqemu https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh && chmod +x ${PREFIX}/bin/utqemu && sed -i 's/\$(dirname \$0)//' ${PREFIX}/bin/utqemu
#curl -o /usr/local/bin/utqemu https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh && sed -i "1d;2i\#\!$(command -v bash)" /usr/local/bin/utqemu && chmod +x /usr/local/bin/utqemu && sed -i 's/\$(dirname \$0)//' /usr/local/bin/utqemu
curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh | grep '2021/08/02'
if [ $? == 0 ]; then
echo 1
else
echo 2
fi
eof


ABOUT_UTQEMU(){
	clear
	printf "${YELLOW}关于utqemu脚本${RES}
	最初是为utermux写下的qemu-system-x86脚本，目的是增加utermux可选功能，给使用者提供简易快捷的启动，我是业余爱好者，给使用者提供简易快捷的启动。非专业人士，所以内容比较乱，请勿吐槽。为适配常用镜像格式，脚本的参数选用是比较常用。业余的我，专业的参数配置并不懂，脚本参数都是来自官方网站、百度与群友。qemu5.0以上的版本较旧版本变化比较大，所以5.0后的参数选项比较丰富，欢迎群友体验使用。\n\n"
	case $SYS in
		ANDROID) CONFIRM ;;
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
6) 6*最新版"
	read -r -p "请选择 " input
	case $input in
		1) VERSION=2 ;;
		2) VERSION=3 ;;
		3) VERSION=4 ;;
		4) VERSION=5 ;;
		5) VERSION=6 ;;
		6) VERSION=6 RC=".*" ;;
		*) ABOUT_UTQEMU ;;
	esac
	echo -e "${YELLOW}安装所需依赖包${RES}"
	cd
	sudo_
	if ! grep -q https /etc/apt/sources.list; then
	$sudo apt install apt-transport-https ca-certificates -y && sed -i "s/http/https/g" /etc/apt/sources.list && $sudo apt update
	fi
	$sudo apt install git libglib2.0-dev libfdt-dev libpixman-1-dev zlib1g-dev libsdl1.2-dev libsnappy-dev liblzo2-dev automake gcc python3 python3-setuptools build-essential ninja-build libspice-server-dev libsdl2-dev libspice-protocol-dev meson libgtk-3-dev libaio-dev gettext samba xz-utils pulseaudio python libbluetooth-dev libbrlapi-dev libbz2-dev libcap-dev libcap-ng-dev libcurl4-gnutls-dev libibverbs-dev libncurses5-dev libnuma-dev librbd-dev librdmacm-dev libsasl2-dev libseccomp-dev libusb-dev flex bison git-email libssh2-1-dev libvde-dev libvdeplug-dev libvte-*-dev libxen-dev valgrind xfslibs-dev libnfs-dev libiscsi-dev usbutils telnet wget -y
	if [ $? != 0 ]; then
	$sudo apt install -f
	fi
	echo -e "${YELLOW}检测下载${RES}"
	VERSION=$(curl https://download.qemu.org | grep qemu-${VERSION}\..\..$RC\.tar.xz\" | tail -n 1 | awk -F 'href="' '{print $2}' | awk -F '.tar' '{print $1}')
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
#	sed -i 's/^\(spice.*"\)$/#\1\nspice="yes"/' configure
#aarch64-softmmu,arm-softmmu,i386-softmmu,x86_64-softmmu,ppc-softmmu,ppc64-softmmu,mips-softmmu,m68k-softmmu
./configure --target-list=i386-softmmu,x86_64-softmmu --enable-spice --enable-gtk --enable-sdl --audio-drv-list=oss,alsa,sdl,pa --python=$(command -v python3)
	if [ $? != 0 ]; then
		echo -e "${RED}编译失败${RES}"
		CONFIRM
		ABOUT_UTQEMU
	fi
	make -j8 && make install
	if [ -e /usr/local/bin/qemu-system-i386 ]; then
		PA
		echo -e "${YELLOW}已安装\n删除源文件...${RES}"
		cd && rm -rf $VERSION
		rm -rf $VERSION.tar.xz
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
	virtio显卡因参数问题，未发挥其特性功能。3D模式需在gtk或sdl下才能开启，sdl模块在系统源默认是未编译。gtk则可在图形界面中启动。经过多次测试，作出的参数配置如下。当你选择virtio显卡中的3D模式时，vnc，sdl，spice输出端口不再有效，但仍会按你的上述选择作出以下配置。sdl将以-display sdl,gl=on输出（因系统的qemu源默认未编译sdl内容，所以选项未得到测试验证）。而spice则以wiki上的标准参数-display gtk,gl=on输出，但virtio显卡并不被识别。vnc除了spice上的参数外，我还加入了-vga qxl来兼容virtio显卡输出（我成功在图形界面中开启gl，但存在bug）。这个3D模式应该是在linux系统下加载，而非windows系统。

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
IP=`ip -4 -br a | awk '{print $3}' | cut -d '/' -f 1 | sed -n 2p`
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
	else
	sudo=""
	fi
####################
BF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/debian/"
BF_URL="deb http://mirrors.bfsu.edu.cn/debian"
US_URL="deb http://mirrors.ustc.edu.cn/debian"
DEB="main contrib non-free"
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
	case $input in
		*) ;; esac
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
	case $(egrep 'tablet|computer' ${HOME}/.utqemu_) in
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
SELECT_EMU_MODE() {
	 echo -e "\n请选择启动哪个${YELLOW}模拟器架构${RES}\n
	 1) qemu-system-x86_64 (64位操作系统)
	 2) qemu-system-i386   (32位操作系统)\n"
	 read -r -p "请选择: " input
	 case $input in
		 1) QEMU_SYS=qemu-system-x86_64 ;;
		 2) QEMU_SYS=qemu-system-i386 ;;
		 *) INVALID_INPUT
			 SELECT_EMU_MODE ;;
	 esac
}
####################
QEMU_VERSION(){
	uname -a | grep 'Android' -q
        if [ $? == 0 ]; then
                SYS=ANDROID
		qemu_ver=$(qemu-system-x86_64 --version) 2>/dev/null
	elif [ ! $(command -v qemu-system-x86_64) ]; then
		echo ""
	elif qemu_ver=$(qemu-system-x86_64 --version)
		[[ $(echo $qemu_ver | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [5-9] ]]; then
#	elif [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [5-9] ]]; then
#        elif [[ $(qemu-system-x86_64 --version) =~ :[5-9] ]] ; then
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
	echo -n -e "${RES}\n请输入${YELLOW}系统镜像${RES}全名（例如andows.img），退出请输${YELLOW}0${RES}，请输入: "
	read  hda_name
	done
	if [ $hda_name == '0' ]; then
		QEMU_SYSTEM
	fi
}
#################
LIST() {
	echo -e "已为你列出镜像文件夹中的常用镜像格式文件（仅供参考）\e[33m"
	ls ${DIRECT}${STORAGE} | egrep "\.blkdebug|\.blkverify|\.bochs|\.cloop|\.cow|\.tftp|\.ftps|\.ftp|\.https|\.http|\.dmg|\.nbd|\.parallels|\.qcow|\.qcow2|\.qed|\.host_cdrom|\.host_floppy|\.host_device|\.file|\.raw|\.sheepdog|\.vdi|\.vmdk|\.vpc|\.vvfat|\.img|\.XBZJ|\.vhd|\.iso|\.fd"
	if [ $? == 1 ]; then
		echo -e "${GREEN}\n貌似没有符合格式的镜像，请以实际文件名为主${RES}"
	fi
}
#################
FAIL() {
FILE="No such file"
SHARE_="516.06"
PORT="Address already"
CPU="CPU model"
GTK="gtk initialization"
echo -e "\n"
LOG=$(cat ${HOME}/.utqemu_log | tail -n 1)
	echo $LOG
	case $LOG in
	*$FILE*) echo -e "${YELLOW}错误：没有匹配的目录或文件名${RES}" ;;
	*$SHARE_*) echo -e "${YELLOW}错误：共享文件超过516.06 MB${RES}" ;;
	*$PORT*) echo -e "${YELLOW}\n错误：视频输出端口占用${RES}" ;;
	*$CPU*) echo -e "${YELLOW}\n错误：cpu名字有误${RES}" ;;
	*$GTK*) echo -e "${YELLOW}\n错误：图形界面错误${RES}" ;;
	*)  ;;
esac
}
#################
LOGIN() {
	if [[ ! -e "$DEBIAN-qemu/root/.utqemu_" ]]; then
	echo $UPDATE >>$DEBIAN-qemu/root/.utqemu_
	elif ! grep -q $UPDATE "$DEBIAN-qemu/root/.utqemu_" ; then
	echo -e "\n${GREEN}检测到脚本有更新，更新日期$UPDATE${RES}"
	INFO
	read -r -p "1)更新 0)忽略并不再提示此版本 " input
	case $input in
		1|"") rm $DEBIAN-qemu/root/utqemu.sh 2>/dev/null
			curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh -o $DEBIAN-qemu/root/utqemu.sh ;;
		*) ;;
	esac
	sed -i "/$(date +"%Y")/d" $DEBIAN-qemu/root/.utqemu_ && echo "$UPDATE" >>$DEBIAN-qemu/root/.utqemu_
	fi
pulseaudio --start & 2>/dev/null
echo "" &
unset LD_PRELOAD
command="proot"
command+=" --kill-on-exit"
command+=" --link2symlink"
command+=" -S $DEBIAN-qemu"
command+=" -b /sdcard"
command+=" -b $DEBIAN-qemu/root:/dev/shm"
command+=" -b /sdcard:/root/sdcard"
command+=" -w /root"
command+=" /usr/bin/env -i"
command+=" HOME=/root"
command+=" USER=root"
command+=" PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games"
command+=" TERM=xterm-256color"
command+=" LANG=C.UTF-8"
command+=" /bin/bash --login"
com="$@"
	if [ -z "$1" ];then
		exec $command
	else
		$command -c "$com"
	fi
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
        if [ -e ${BAGNAME} ]; then
                rm -rf ${BAGNAME}
        fi
        curl -o ${BAGNAME} ${DEF_CUR}
                VERSION=`cat ${BAGNAME} | grep href | tail -n 2 | cut -d '"' -f 4 | head -n 1`
                curl -o ${BAGNAME} ${DEF_CUR}${VERSION}${BAGNAME}
        if [ $? -ne 0 ]; then
		echo -e "${RED}下载失败，请重输${RES}\n" && MAIN
        fi
        if [ -e $sys_name ]; then
		rm -rf $sys_name
        fi
                mkdir $sys_name
#tar xvf rootfs.tar.xz -C ${BAGNAME}
	echo -e "${BLUE}正在解压系统包${RES}"
	tar xf ${BAGNAME} --checkpoint=100 --checkpoint-action=dot --totals -C $sys_name 2>/dev/null
        rm ${BAGNAME}
	echo -e "${BLUE}$sys_name系统已下载，文件夹名为$sys_name${RES}"
        echo "127.0.0.1 localhost" > $sys_name/etc/hosts
        rm -rf $sys_name/etc/resolv.conf &&
        echo "nameserver 223.5.5.5
nameserver 223.6.6.6" >$sys_name/etc/resolv.conf
        echo "export  TZ='Asia/Shanghai'" >> $sys_name/root/.bashrc
	case $DEBIAN in
		bullseye)
echo "${US_URL}/ bullseye ${DEB}
${US_URL}/ bullseye-updates ${DEB}
${US_URL}/ bullseye-backports ${DEB}
${US_URL}-security bullseye-security ${DEB}" >$sys_name/etc/apt/sources.list

			;;
		buster) 
echo "$US_URL buster ${DEB}
${US_URL} buster-updates ${DEB}
${US_URL} buster-backports ${DEB}
${US_URL}-security buster/updates ${DEB}" >$sys_name/etc/apt/sources.list
: <<\eof
echo "${US_URL} stable ${DEB}
${US_URL} stable-updates ${DEB}" >$sys_name/etc/apt/sources.list
eof
;;
	esac
cat >/dev/null <<EOF
echo "${US_URL}/ bullseye ${DEB}
${US_URL}/ bullseye-updates ${DEB}
${US_URL}/ bullseye-backports ${DEB}
${US_URL}-security bullseye-security ${DEB}" >$sys_name/etc/apt/sources.list
EOF
	if [ ! -f $(pwd)/utqemu.sh ]; then
	curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh -o $sys_name/root/utqemu.sh 2>/dev/null
	else
		cp utqemu.sh $sys_name/root/
	fi
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
	if grep '^[^#]' ${PREFIX}/etc/apt/sources.list | grep termux.org; then 
		echo -e "${YELLOW}检测到你使用的可能为非国内源，为保证正常使用，建议切换为国内源(0.73版termux勿更换)${RES}\n  
		1) 换国内源    
		2) 不换"   
	read -r -p "是否换国内源: " input   
	case $input in    
		1|"") echo "换国内源" 
	sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list 
	sed -i 's@^\(deb.*games stable\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/game-packages-24 games stable@' $PREFIX/etc/apt/sources.list.d/game.list 
	sed -i 's@^\(deb.*science stable\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/science-packages-24 science stable@' $PREFIX/etc/apt/sources.list.d/science.list && pkg update ;;
		*) echo "#utqemucheck" >>${PREFIX}/etc/apt/sources.list ;;  
	esac                                                    
		fi
	if [ ! $(command -v curl) ]; then
	pkg update && pkg install curl -y
	fi
	dpkg -l | grep pulseaudio -q 2>/dev/null
	if [ $? != 0 ]; then
	echo -e "${YELLOW}检测到你未安装pulseaudio，为保证声音正常输出，将自动安装${RES}"
	sleep 2
	pkg update && pkg install pulseaudio -y
	fi
	if grep -q "anonymous" ${PREFIX}/etc/pulse/default.pa ;
	then
		echo ""
	else
        echo "load-module module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" >> ${PREFIX}/etc/pulse/default.pa
	fi
	if grep -q "exit-idle" ${PREFIX}/etc/pulse/daemon.conf ; then
	sed -i '/exit-idle/d' ${PREFIX}/etc/pulse/daemon.conf
	echo "exit-idle-time = -1" >> ${PREFIX}/etc/pulse/daemon.conf
	fi
	if [ ! $(command -v proot) ]; then
	pkg update && pkg install proot -y
	fi
	fi
}
##################
WEB_SERVER() {
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	if [ ! $(command -v python) ]; then
	echo -e "\n检测到你未安装所需要的包python,将先为你安装上"
	sudo_
	apt install python -y
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

echo -e "\n1)  创建空磁盘(目前支持qcow2,vmdk)
2)  转换镜像磁盘格式(仅支持qcow2,vmdk,其他格式未验证)
3)  修改设备标识(手机、平板、电脑)
4)  修改源(只适用本脚本下载的系统)
5)  安装aqemu(适用于图形界面中操作的qemu皮肤)
6)  获取最新版termux、aspice与xsdl的安卓版下载地址(非永久有效)
7)  模拟系统的时间不准
8)  修改镜像目录
9)  更新为支持qemu6.0容器
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
	6) read -r -p "1)termux 2)aspice 3)xsdl 4)termux-api " input
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
	while ( [ "$SPI_URL" != '0' ] && [[ ! $SPI_URL_ =~ apk ]] )
do
	SPI_URL=`curl --connect-timeout 5 -m 8 -s https://github.com/iiordanov/remote-desktop-clients | grep tag\/ | cut -d '"' -f 4 | cut -d '/' -f 6`
SPI_URL_=`curl --connect-timeout 5 -m 8 https://github.com/iiordanov/remote-desktop-clients/releases/tag/$SPI_URL | grep SPICE | grep apk | tail -n 1 | cut -d '>' -f 2 | cut -d '<' -f 1`
	if [[ ! $SPI_URL_ =~ apk ]]; then
	read -r -p "获取失败，重试请回车，退出请输0 " input
	case $input in
	0) QEMU_ETC ;;
	*) ;;
	esac
	fi
	done
	echo -e "\n下载地址\n${GREEN}https://github.com/iiordanov/remote-desktop-clients/releases/download/$SPI_URL/$SPI_URL_${RES}\n"
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
	9) echo "${US_URL} sid ${DEB}" >/etc/apt/sources.list && $sudo apt update
	$sudo apt install qemu-system-x86 -y
	if [[ $(qemu-system-i386 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = 6 ]]; then
	echo -e "更新成功"
	else
	echo -e "更新失败"
	fi
	sleep 2
	QEMU_ETC
	;;
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
	if [ ! $(command -v curl) ]; then
		$sudo apt update
		$sudo apt install curl -y
	fi
	uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	if ! grep -q https /etc/apt/sources.list; then
		$sudo apt install apt-transport-https ca-certificates -y && sed -i "s/http/https/g" /etc/apt/sources.list && $sudo apt update
	fi
	fi
	unset hda_name display hdb_name iso_name iso1_name SOUND_MODEL VGA_MODEL CPU_MODEL NET_MODEL SMP URL script_name QEMU_MODE
	QEMU_VERSION
	NOTE
echo -e "
1)  安装qemu-system-x86_64，并联动更新模拟器所需应用\n\e[33m(由于qemu的依赖问题，安装过程可能会失败，请尝试重新安装)${RES}"
case $ARCH in
	computer) echo -e "2)  创建windows镜像目录" ;;
	*)
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	echo -e "2)  创建windows镜像目录"
else
	echo -e "2)  创建windows镜像目录及本地共享文件目录share拷贝"
	fi ;;
esac
echo -e "3)  启动qemu-system-x86_64模拟器
4)  让termux成为网页服务器
    (使模拟系统可以通过浏览器访问本机内容)
5)  virtio驱动相关"
	case $SYS in
	ANDROID) ;;
	*) echo -e "6)  应用维护" ;;
	esac
echo -e "7)  查看日志
8)  更新内容
9)  关于utqemu
10) 在线termux-toolx脚本体验维护linux系统(debian)
11) 在线测试本机cpu支持模拟的特性
0)  退出\n"
	read -r -p "请选择: " input
	case $input in
	1) echo -e "${YELLOW}安装过程中，如遇到询问选择，请输(y)，安装过程容易出错，请重试安装${RES}"
	sleep 2
	uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	pkg update -y && apt --fix-broken install -y && apt install qemu-system-x86-64-headless qemu-system-i386-headless curl -y
	if [ ! $(command -v qemu-system-x86) ]; then
	echo -e "\n检测安装失败，重新安装\n"
	sleep 1
	apt --fix-broken install -y && apt install qemu-system-x86-64-headless qemu-system-i386-headless curl -y
	fi
	else
	sudo_
	if grep -q 'bullseye' "/etc/os-release"; then
		echo -e "\n${YELLOW}debian-sid源地址已有qemu6.0可供安装，是否更新版本？(非本脚本安装的容器慎选)${RES}"
		read -r -p "1)更新为qemu6.0系统 2)继续使用qemu5.2系统 "
		case $input in
			1) echo 'deb http://mirrors.bfsu.edu.cn/debian/ sid main contrib non-free' >/etc/apt/sources.list && apt update ;;
			*) ;;
		esac
	fi
	if ! grep -q https /etc/apt/sources.list; then
	$sudo apt install apt-transport-https ca-certificates -y && sed -i "s/http/https/g" /etc/apt/sources.list && $sudo apt update
	fi
       	$sudo apt install qemu-system-x86 xserver-xorg x11-utils pulseaudio curl samba usbutils telnet -y
	if [ ! $(command -v qemu-system-x86) ]; then
	echo -e "\n检测安装失败，重新安装\n"
	sleep 1
	$sudo apt install qemu-system-x86 xserver-xorg x11-utils pulseaudio curl samba usbutils telnet -y
	fi
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
	echo -e "\n1) 创建镜像目录
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
	more ${HOME}/.utqemu_log | egrep "qemu-system-x86_64|qemu-system-i386|initialization" | egrep -v "stronger memory|Connection reset by peer|requested feature"
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
	10) bash -c "$(curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/termux-toolx.sh)" ;;
	11) bash -c "$(curl -s https://cdn.jsdelivr.net/gh/chungyuhoi/script/Check_cpuids.sh)"
	CONFIRM
	QEMU_SYSTEM ;;
	0) exit 0 ;;
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
	uname -a | grep 'Android' -q 
	if [ $? == 0 ]; then
		echo -e "\n${YELLOW}vncviewer地址为127.0.0.1:0${RES}"     
		sleep 1
		display=vnc
	else
	case $QEMU_MODE in
		VIRTIO_MODE) ;;
		*) echo -n -e "\n${GREEN}是否已有快捷脚本，如有请输快捷脚本名字，如无请回车:${RES} "
	read script_name
	if [ -n "$script_name" ]; then
	if [ $(command -v $script_name) ] || [ -f "${HOME}/xinhao/$script_name" ]; then
		printf "%s\n"
#cat $(which $script_name)

	if grep '\-cpu' ${HOME}/xinhao/$script_name 2>/dev/null; then
	printf "%s\n${GREEN}启动模拟器\n"
	elif grep 'vnc' /usr/local/bin/$script_name; then
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0\n"
	elif grep -q 'DISPLAY' /usr/local/bin/$script_name; then
	grep '\-cpu' /usr/local/bin/$script_name
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开xsdl\n"
	elif grep 'spice' /usr/local/bin/$script_name; then
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开aspice 127.0.0.1 端口 5900\n"
	else
	grep '\-cpu' /usr/local/bin/$script_name
	printf "%s\n${GREEN}启动模拟器\n"
	fi
	echo '如共享目录成功加载，请在浏览器地址输 \\10.0.2.4'
	if grep -q monitor ${HOME}/xinhao/$script_name 2>/dev/null; then
	echo -e "调试命令：telnet 127.0.0.1 4444${RES}"
	elif grep -q monitor /usr/local/bin/$script_name 2>/dev/null; then
	echo -e "调试命令：telnet 127.0.0.1 4444${RES}"
	else
	trap " rm /tmp/hugepage* /mnt/hugepages* 2>/dev/null;exit" SIGINT EXIT
	fi
	if grep -q hugepage ${HOME}/xinhao/$script_name 2>/dev/null; then
	echo -e "${GREEN}你使用了大页内存，开始模拟器前需要时间创建同内存大小文件，文件会在qemu退出后自动删除${RES}"
	elif grep -q hugepage /usr/local/bin/$script_name 2>/dev/null; then
	echo -e "${GREEN}你使用了大页内存，开始模拟器前需要时间创建同内存大小文件，文件会在qemu退出后自动删除${RES}"
	fi
	echo ""
	printf "%s${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}\n"
	sleep 1
	$script_name >/dev/null 2>>${HOME}/.utqemu_log || bash ${HOME}/xinhao/$script_name >/dev/null 2>>${HOME}/.utqemu_log 
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
	case $ARCH in
	tablet) echo -e "\n请选择${YELLOW}显示输出方式${RES}"
	case $SYS in
	QEMU_PRE) read -r -p "1)vnc 2)sdl 3)spice 4)图形界面下 5)局域网vnc 9)返回 0)退出 " input ;;
	*) read -r -p "1)vnc 2)sdl 3)spice 4)图形界面下 5)局域网vnc 6)快速启动(测试阶段) 9)返回 0)退出 " input
	esac
	case $input in
		1|"") echo -e "\n${BLUE}vnc输出${RES}"
			display=vnc
			;;
		2) echo -e "${BLUE}sdl信号输出，需先打开xsdl再继续此操作${RES}"
			display=xsdl
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
	echo -e "\n${GREEN}为减少效率的影响，暂不支持声音输出${RES}\n因部分机型支持双wifi或wifi热点同开，导致出现两段ip，请确保使用的${RED}ip唯一${RES}\n输出显示的设备vnc地址为$IP:0${RES}"
	sleep 1 ;;
		6) case $SYS in
			QEMU_PRE) INVALID_INPUT
			QEMU_SYSTEM ;; 
		*) printf "\n%b\n" "${GREEN}本选项使用常用配置参数${RES}"
		printf "%-7s %-7s %-7s %s\n" 系统 winxp win7 virtio驱动模式 声卡 ac97 hda hda 显卡 cirrus VGA qxl 网卡 e1000 e1000 virtio 接口 ide sata virtio
		printf "%-7s %s %s\n\n" 视频 vnc 127.0.0.1:0
	mem=$(free -m | awk '{print $2/4}' | sed -n 2p | cut -d '.' -f 1)
	echo -e "${YELLOW}请选择拟模拟的系统${RES}"
	read -r -p "1)winxp 2)win7 3)virtio驱动模式 9)返回 " input
	case $input in
	1) echo -e "\nqemu5.0以上版本模拟winxp开机比较慢\n"
	LIST
	HDA_READ
	if (( $mem >= 512 )); then
	mem_=1024
	else
	mem_=512
	fi
	MA=pc-i440fx-3.1 VIDEO="-device cirrus-vga" DRIVE="-drive file=${DIRECT}${STORAGE}$hda_name,if=ide,index=0,media=disk,aio=threads,cache=writeback" NET="-device e1000,netdev=user0 -netdev user,id=user0,smb=${HOME}/share" AUDIO="-device AC97" SHARE="-drive file=fat:rw:${DIRECT}/xinhao/share,if=ide,index=3,media=disk,aio=threads,cache=writeback";;
	2) 	LIST
	HDA_READ
	MA=pc VIDEO="-device VGA" DRIVE="-drive id=disk,file=${DIRECT}${STORAGE}$hda_name,if=none -device ahci,id=ahci -device ide-hd,drive=disk,bus=ahci.0" NET="-device e1000,netdev=user0 -netdev user,id=user0,smb=${HOME}/share" AUDIO="-device intel-hda -device hda-duplex" SHARE="-usb -drive if=none,format=raw,id=disk1,file=fat:rw:${DIRECT}/xinhao/share/ -device usb-storage,drive=disk1"
;;
	3) echo -e "${GREEN}此选项参数是hda声卡，virtio网卡，qxl显卡，virtio磁盘接口(注意，模拟系统需已装驱动，否则启动不成功)${RES}"
		sleep 1
		LIST
		HDA_READ
	MA=q35 VIDEO="-device qxl-vga" DRIVE="-drive file=${DIRECT}${STORAGE}$hda_name,index=0,media=disk,if=virtio,cache=none" NET="-device virtio-net-pci,netdev=user0 -netdev user,id=user0,smb=${HOME}/share" AUDIO="-device intel-hda -device hda-duplex" SHARE="-drive file=fat:rw:${DIRECT}/xinhao/share,index=3,media=disk,if=virtio"
	;;
	9) QEMU_SYSTEM ;;
	*) INVALID_INPUT
	QEMU_SYSTEM ;;
	esac
killall -9 qemu-system-x86 2>/dev/null
killall -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
START="qemu-system-x86_64 -machine $MA,hmat=off,usb=off,vmport=off,dump-guest-core=off,mem-merge=off,kernel-irqchip=off --accel tcg,thread=multi -m $mem_ -nodefaults -no-user-config -msg timestamp=off -k en-us -cpu max,-hle,-rtm -smp 2 $VIDEO $NET -audiodev alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124,out.period-length=1024 $AUDIO,audiodev=alsa1 -rtc base=localtime -boot order=cd,menu=on,strict=off -usb -device usb-tablet $DRIVE $SHARE -display vnc=127.0.0.1:0,lossy=on,non-adaptive=off"
#-display vnc=127.0.0.1:0,key-delay-ms=0,connections=15000"

cat <<-EOF
$START
EOF
	printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0"
	echo ""
	echo '如共享目录成功加载，请在浏览器地址输 \\10.0.2.4'
	printf "%s\n${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}\n"
	$START >/dev/null 2>>${HOME}/.utqemu_log
	if [ $? == 1 ]; then
	FAIL
	printf "%s${RED}启动意外中止，请查看日志${YELLOW}d(ŐдŐ๑)${RES}\n"
	fi
	exit 0 ;;
	esac
			;;
		9) QEMU_SYSTEM ;;
		0) exit 0 ;;
		*) INVALID_INPUT
			QEMU_SYSTEM ;;
	esac
	sleep 1 ;;
	computer)
		echo -e "\n请选择${YELLOW}显示输出方式${RES}"
		read -r -p "1)本地 2)局域网vnc 9)返回 0)退出 " input
		case $input in
			1|"")
				display=amd ;;
			2)
				display=wlan_vnc
				echo -e "\n${BLUE}vnc不支持声音输出，输出显示的设备vnc地址为$IP:0${RES}"
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
###################
	SELECT_EMU_MODE

	case $DIRECT in
		\/sdcard)
	echo -e "\n${GREEN}请确认系统镜像已放入手机目录${STORAGE}里${RES}\n" ;;
		*) echo -e "\n${GREEN}请确认系统镜像已放入目录${STORAGE}里${RES}\n" ;;
	esac
	sleep 1
	killall -9 qemu-system-x86 2>/dev/null
	killall -9 qemu-system-i38 2>/dev/null
	if [ ! -d "${DIRECT}${STORAGE}" ];then
		echo -e "${RED}未获取到镜像目录，请确认已创建镜像目录${RES}\n"
		CONFIRM
		QEMU_SYSTEM
	fi
	LIST
		HDA_READ
	case $SYS in
		QEMU_ADV) 
	case $QEMU_MODE in
		"")
	echo -e "请选择${YELLOW}分区磁盘${RES}加载模式"
	read -r -p "1)加载分区镜像 2)加载双光盘 不加载请直接回车 " input
	case $input in
		1) echo -n -e "请输入${YELLOW}分区镜像${RES}全名,不加载请直接回车（例如hdb.img）: "
	read hdb_name ;;
		2) echo -n -e "请输入${YELLOW}第一个光盘${RES}全名,不加载请直接回车（例如DVD.iso）: "
	read iso1_name ;;
		*) ;;
	esac ;;
	VIRTIO_MODE) ;;
	esac ;;
	*) echo -n -e "请输入${YELLOW}分区镜像${RES}全名,不加载请直接回车（例如hdb.img）: "
	read hdb_name ;;
	esac
	echo -n -e "请输入${YELLOW}光盘${RES}全名,不加载请直接回车（例如DVD.iso）: "
	read iso_name
#内存
	echo -e -n "请输入模拟的${YELLOW}内存${RES}大小(建议本机的1/4)，以m为单位（1g=1024m，例如输512），自动分配请回车: "
        read mem
	mem=`echo $mem | tr -cd '[0-9]'`
	if [ -n "$mem" ]; then
		uname -a | grep 'Android' -q
		if [ $? != 0 ]; then
		if (( "$mem" > "$mem_" )); then
			echo -e "${YELLOW}你设置的内存值大于推荐值，建议使用大页内存(通过创建相应大页文件代替设备ram，响应速度略降低)${RES}"
		read -r -p "1)使用大页 0)使用设备ram " input
		case $input in
			1) HUGEPAGE=true ;;
			*) ;;
		esac
		fi
		fi
		set -- "${@}" "-m" "$mem"
	else
#		set -- "${@}" "-m" "$mem_"
		set -- "${@}" "-m" "$mem_,slots=2,maxmem=$(( $mem_ * 2 ))m"
	fi


#	set -- "${@}" "-full-screen"
#不加载默认的配置文件。默认会加载/use/local/share/qemu下的文件，通常模拟器默认加载串口，并口，软盘，光驱等。
	set -- "${@}" "-nodefaults"
#不加载用户自定义的配置文件。
	set -- "${@}" "-no-user-config"
#	set -- "${@}" "-k" "en-us"
#	set -- "${@}" "-usbdevice" "keyboard"
	case $ARCH in
		tablet)
#重定向虚拟串口到主机设备
#	set -- "${@}" "-serial" "none"
#重定向虚拟并口到主机设备
#	set -- "${@}" "-parallel" "none"
#控制台，一种类似于shell的交互方式
#	set -- "${@}" "-monitor" "none"
;;
		*) ;;
	esac
#qemu monitor protocol协议，对qemu虚拟机进行交互
#	set -- "${@}" "-qmp" "tcp:127.0.0.1:4444,server,nowait" "-monitor" "none"
#使用bios配置
#	set -- "${@}" "-L" "${DIRECT}${STORAGE}"
#使用bzImage内核镜像
#	set -- "${@}" "-kernel" "bzImage"
#使用cmdline作为内核命令行
#	set -- "${@}" "-append" "cmdline"
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
		read -r -p "1)n270 2)athlon 3)pentium2 4)core2duo 5)Skylake-Server-IBRS 6)Nehalem-IBRS 7)Opteron_G5 8)max(推荐) 9)(慎选) 0)自己输 " input ;;
QEMU_PRE) read -r -p "1)n270 2)athlon 3)pentium2 4)core2duo 5)Skylake-Server-IBRS 6)Nehalem-IBRS 7)Opteron_G5 9)max 0)自己输 " input ;;
	esac
#max 对本机cpu的特性加载到虚拟机 host 直接迁移本机cpu到虚拟机(适用于kvm)
#部分cpu id flags：fpu –板载FPU，vme –虚拟模式扩展，de –调试扩展，pse –页面大小扩展，tsc –时间戳计数器，操作系统通常可以得到更为精准的时间度量，msr –特定于模型的寄存器，pae –物理地址扩展，cx8 – CMPXCHG8指令，apic–板载APIC，sep– SYSENTER/SYSEXIT，mtrr –存储器类型范围寄存器，pge – Page Global Enable，mca –Machine Check Architecture，cmov – CMOV instructions（附加FCMOVcc，带有FPU的FCOMI），pat –页面属性表，pse36 – 36位PSE，clflush – CLFLUSH指令，dts –调试存储，acpi –ACPI via MSR，mmx –多媒体扩展，fxsr – FXSAVE/FXRSTOR, CR4.OSFXSR，sse – SSE，sse2 – SSE2，ss – CPU自侦听，ht –超线程，tm –自动时钟控制，ia64 – IA-64处理器，pbe –等待中断启用，mmxext – AMD MMX扩展，fxsr_opt – FXSAVE / FXRSTOR优化，rdtscp – RDTSCP，lm –长模式（x86-64），3dnowext – AMD 3DNow扩展，k8 –皓龙，速龙64，k7 –速龙，pebs –基于精确事件的采样，bts –分支跟踪存储，nonstop_tsc – TSC不会在C状态下停止，PNI – SSE-3，pclmulqdq – PCLMULQDQ指令，dtes64 – 64位调试存储，监控器–监控/等待支持，ds_cpl – CPL Qual.调试存储，vmx –英特尔虚拟化技术(VT技术)，smx –更安全的模式，est –增强的SpeedStep，tm2 –温度监控器2，ssse3 –补充SSE-3，cid –上下文ID，cx16 – CMPXCHG16B，xptr –发送任务优先级消息，dca –直接缓存访问，sse4_1 – SSE-4.1，sse4_2 – SSE-4.2，x2apic – x2APIC，aes – AES指令集，xsave – XSAVE / XRSTOR / XSETBV / XGETBV，avx –高级矢量扩展，hypervisor–在hypervisor上运行，svm –AMD的虚拟化技术(AMD-V)，extapic –扩展的APIC空间，cr8legacy – 32位模式下的CR8，abm –高级bit操作，ibs –基于Sampling的采样，sse5 – SSE-5，wdt –看门狗定时器，硬件锁定清除功能（HLE），受限事务存储（RTM）功能，HLE与RTM为TSX指令集，决定服务器cpu多线程或单线程处理数据。
        case $input in
        1) CPU_MODEL=n270
		SMP_="2,cores=1,threads=2,sockets=1" ;;
        2) CPU_MODEL=athlon
		SMP_="2,cores=2,threads=1,sockets=1" ;;
        3) CPU_MODEL=pentium2
		SMP_="1,cores=1,threads=1,sockets=1" ;;
        4) CPU_MODEL=core2duo
		SMP_="2,cores=2,threads=1,sockets=1" ;;
	5) CPU_MODEL=Skylake-Server-IBRS
		SMP_="4,cores=2,threads=1,sockets=2" ;;
	6) CPU_MODEL=Nehalem-IBRS
		SMP_="8,cores=8,threads=1,sockets=1" ;;
	7) CPU_MODEL=Opteron_G5
		SMP_="8,cores=8,threads=1,sockets=1" ;;
	8) case $SYS in
		QEMU_ADV|ANDROID) CPU_MODEL="max,-hle,-rtm"
		SMP_="8,cores=8,threads=1,sockets=1" ;;
		*) CPU_MODEL=max
			unset _SMP
			SMP_=4,maxcpus=5
			;;
	esac ;;
	9) 
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
eof
		CPU_MODEL="core2duo,-lm,-syscall,-hle,-rtm,hv_spinlocks=0xFFFFFFFF,hv_relaxed,hv_time,hv_vapic,hv-frequencies"
		unset _SMP
		SMP_="2,cores=2,threads=1,sockets=2,maxcpus=4" ;;
	0) echo -e -n "请输入: "
		read CPU_MODEL
		if echo $CPU_MODEL | grep max; then
		unset _SMP
		SMP_="4,maxcpus=5"
		else
		SMP_="4,cores=4,threads=1,sockets=1"
		fi ;;
	95) CPU_MODEL="Opteron_G5,hv_spinlocks=0xffff,hv_relaxed,hv_time,hv_vapic,-fma,-avx,-f16c,-syscall,-lm,-misalignsse,-3dnowprefetch,-xop,-fma4,-tbm,-nrip-save"
		SMP_="2,cores=2,threads=1,sockets=2,maxcpus=4" ;;
	96) CPU_MODEL="Penryn-v1,hv_spinlocks=0xffff,hv_relaxed,hv_time,hv_vapic,-lm,-syscall"
		SMP_="2,cores=2,threads=1,sockets=2,m
axcpus=4" ;;
	97) CPU_MODEL="EPYC-IBPB,hv_spinlocks=0xffff,hv_relaxed,hv_time,hv_vapic,-fma,-avx,-f16c,-avx2,-rdseed,-sha-ni,-syscall,-fxsr-opt,-lm,-misalignsse,-3dnowprefetch,-osvw,-topoext,-ibpb,-nrip-save,-xsavec"
		SMP_="2,cores=2,threads=1,sockets=2,maxcpus=4" ;;
	98) CPU_MODEL="Cascadelake-Server-v4,-mds-no,-fma,-pcid,-x2apic,-tsc-deadline,-avx,-f16c,-avx2,-invpcid,-avx512f,-avx512dq,-avx512cd,-avx512bw,-avx512vl,-rdseed,-avx512vnni,-spec-ctrl,-arch-capabilities,-ssbd,-3dnowprefetch,-xsavec,-rdctl-no,-ibrs-all,-skip-l1dfl-vmentry,-syscall,-lm"
		SMP_="8,cores=8,threads=1,sockets=2,maxcpus=16"	;;
	99) CPU_MODEL="phenom-v1,hv_spinlocks=0xffff,hv_relaxed,hv_time,hv_vapic,-fxsr-opt,-syscall,-lm"
		SMP_="4,cores=4,threads=1,sockets=2,maxcpus=8" ;;
        *)      CPU_MODEL=max
		unset _SMP
		SMP_="4,maxcpus=5" ;;
	esac
	set -- "${@}" "-cpu" "${CPU_MODEL}"
	if [ -n "$_SMP" ]; then
		set -- "${@}" "-smp" "${_SMP}"
	elif [ -n "$CPU" ]; then
		set -- "${@}" "-smp" "${CPU}"
	else
		set -- "${@}" "-smp" "${SMP_}"
	fi
#####################
#TERMUX
	uname -a | grep 'Android' -q 
	if [ $? == 0 ]; then
	echo -e "请选择${YELLOW}显卡${RES}"
	read -r -p "1)cirrus 2)vmware 3)vga 4)virtio " input
	case $input in 
		1) VGA_MODEL=cirrus-vga ;;
		2) read -r -p "1)不设置3D参数 2)设置3D参数 " input
		case $input in
		1|"") VGA_MODEL=vmware-svga ;;
		*) VGA_MODEL=vmware-svga,vgamem_mb=256 ;;
	esac ;;
		4) VGA_MODEL=virtio-vga ;;
		*) VGA_MODEL=VGA ;;
        esac
	set -- "${@}" "-device" "${VGA_MODEL}"

	echo -e "请选择${YELLOW}网卡${RES}"
	read -r -p "1)e1000 2)rtl8139 3)virtio 0)不加载 " input
	case $input in
		2) NET_MODEL="rtl8139,netdev=user0" ;;
		3) NET_MODEL="virtio-net-pci,netdev=user0" ;;
		0) ;;
		*) NET_MODEL="e1000,netdev=user0" ;;
	esac
	if [ -n "${NET_MODEL}" ]; then
		set -- "${@}" "-device" "${NET_MODEL}"
		set -- "${@}" "-netdev" "user,id=user0"
	else
		set -- "${@}" "-net" "none"
	fi
	else
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
	case $input in
		2) NET_MODEL="nic,model=rtl8139" ;;
		3) NET_MODEL="nic,model=virtio" ;;
		0) ;;
		*) NET_MODEL="nic,model=e1000" ;;
	esac
	if [ -n "${NET_MODEL}" ]; then
	set -- "${@}" "-net" "${NET_MODEL}"
	set -- "${@}" "-net" "user,smb=${HOME}/share"
	else
	set -- "${@}" "-net" "none"
	fi
	case $display in
		wlan_vnc) ;;
		*)
		echo -e "请选择${YELLOW}声卡${RES}(不加载可提升模拟效率)"
	if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = 4 ]]; then
	read -r -p "1)ac97 2)sb16 3)es1370 4)hda 5)ac97(修改参数，不适合spice) 6)hda(修改参数，不适合spice) 0)不加载 " input
	case $input in
		1|"") SOUND_MODEL=ac97 ;;
		2) SOUND_MODEL=sb16 ;;
		0) ;;
                3) SOUND_MODEL=es1370 ;;
		4) SOUND_MODEL=hda ;;
		5) set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124"
		set -- "${@}" "-device" "AC97,audiodev=alsa1" ;;
		6) set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124"
		set -- "${@}" "-device" "intel-hda" "-device" "hda-duplex,audiodev=alsa1" ;;
		*) SOUND_MODEL=all ;;
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
	fi
	if [ -n "${SOUND_MODEL}" ]; then
	set -- "${@}" "-soundhw" "${SOUND_MODEL}"
	fi ;;
	esac
        else
####################
#5.0
####################
	echo -e "请选择${YELLOW}显卡${RES}"
	read -r -p "1)cirrus 2)vmware 3)vga 4)virtio 5)qxl " input
	case $input in
		1) set -- "${@}" "-device" "cirrus-vga" ;;
		2) read -r -p "1)不设置3D参数 2)设置3D参数 " input
	case $input in
		1|"") set -- "${@}" "-device" "vmware-svga" ;;
		*) set -- "${@}" "-device" "vmware-svga,vgamem_mb=256" ;;
	esac ;;
		4) echo -e "${YELLOW}virtio显卡带3D功能，但因使用的系统环境原因，目前只能通过电脑启用，如果真想尝试，可在图形界面打开(需32位色彩，否则出现花屏)。${RES}"
	read -r -p "1)不设置3D参数 2)设置3D参数 " input
	case $input in
		1|"")
		set -- "${@}" "-device" "virtio-vga"
#		set -- "${@}" "-device" "virtio-vga,virgl=on"
;;
		2) echo -e "\n${YELLOW}你选择virtio显卡3D参数，该模式只能在图形界面(桌面)显示${RES}"
	CONFIRM
	case $display in
		xsdl) set -- "${@}" "-device" "virtio-vga" "-display" "sdl,gl=on" ;;
		vnc|wlan_vnc)
		set -- "${@}" "-vga" "qxl" "-display" "gtk,gl=on" "-device" "virtio-gpu-pci,virgl=on"
#		set -- "${@}" "-device" "qxl" "-vga" "virtio" "-display" "gtk,gl=on"
;;
		spice_|spice|amd|gtk_) set -- "${@}" "-device" "virtio-vga" "-display" "gtk,gl=on" ;;
	esac
	unset display
	case $ARCH in
		computer) ;;
		*) env | grep 'PULSE_SERVER' -q
	if [ $? != 0 ]; then
		export PULSE_SERVER=tcp:127.0.0.1:4713
		fi ;;
	esac ;;
	esac ;;
		5) set -- "${@}" "-device" "qxl-vga"
#			set -- "${@}" "-device" "virtio-keyboard-pci"
: <<\EOF
-device ich9-usb-ehci1,id=usb -device ich9-usb-uhci1,masterbus=usb.0,firstport=0,multifunction=on -chardev spicevmc,id=charredir0,name=usbredir -device usb-redir,chardev=charredir0,id=redir0,bus=usb.0,port=4 -chardev spicevmc,id=charredir1,name=usbredir -device usb-redir,chardev=charredir1,id=redir1,bus=usb.0,port=5

set -- "${@}" "-device" "ich9-usb-ehci1,id=usb"
#set -- "${@}" "-device" "ich9-usb-ehci1,id=usb"
set -- "${@}" "-device" "ich9-usb-uhci1,masterbus=usb.0,firstport=0,multifunction=on"
#set -- "${@}" "-device" "ich9-usb-uhci2,masterbus=usb.0,firstport=2"
#set -- "${@}" "-device" "ich9-usb-uhci3,masterbus=usb.0,firstport=4"
set -- "${@}" "-chardev" "spicevmc,name=usbredir,id=usbredirchardev1" "-device" "usb-redir,chardev=usbredirchardev1,id=usbredirdev1"
#set -- "${@}" "-chardev" "spicevmc,name=usbredir,id=usbredirchardev2" "-device" "usb-redir,chardev=usbredirchardev2,id=usbredirdev2"
#set -- "${@}" "-chardev" "spicevmc,name=usbredir,id=usbredirchardev3" "-device" "usb-redir,chardev=usbredirchardev3,id=usbredirdev3"
EOF
;;
		*) set -- "${@}" "-device" "VGA" ;;
	esac

	echo -e "请选择${YELLOW}网卡${RES}"
	read -r -p "1)e1000 2)rtl8139 3)virtio 0)不加载 " input
	case $input in
		2) NET_MODEL="rtl8139,netdev=user0" ;;
		3) NET_MODEL="virtio-net-pci,netdev=user0" ;;
		0) ;;
		*) NET_MODEL="e1000,netdev=user0" ;;
	esac
	if [ -n "${NET_MODEL}" ]; then
		set -- "${@}" "-device" "${NET_MODEL}"
		set -- "${@}" "-netdev" "user,id=user0,smb=${HOME}/share"
	else
		set -- "${@}" "-net" "none"
	fi
	case $display in
		wlan_vnc) ;;
		*)
	echo -e "请选择${YELLOW}声卡${RES}(不加载可提升模拟效率)"
	read -r -p "1)es1370 2)sb16 3)hda 4)ac97(推荐) 5)ac97(修改参数，不适合spice) 6)hda(修改参数，不适合spice) 7)usb-audio 0)不加载 " input
	case $input in
		1) set -- "${@}" "-device" "ES1370" ;;
		2) set -- "${@}" "-device" "sb16" ;;
		3) set -- "${@}" "-device" "intel-hda" "-device" "hda-duplex" ;;
                0) ;;
		5)
#adc in dac out				
#alsa参数			       	
#延迟timer-period=10000
#采样率out.frequency=8004
#缓冲长度(理论上应为周期长度的倍数)out.buffer-length=10000
#周期长度out.period-length=1020
#pa参数
		set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124"
		set -- "${@}" "-device" "AC97,audiodev=alsa1" ;;
		6) set -- "${@}" "-audiodev" "alsa,id=alsa1,in.format=s16,in.channels=2,in.frequency=44100,out.buffer-length=5124"
		set -- "${@}" "-device" "intel-hda" "-device" "hda-duplex,audiodev=alsa1" ;;
		7) set -- "${@}" "-usb" "-device" "usb-audio" ;;
		*) set -- "${@}" "-device" "AC97" ;;
	esac	;;
esac
	fi
	fi
####################
#进阶选项

	echo -e "\n是否进阶选项，包括${YELLOW}共享文件夹、鼠标、启动顺序、时间${RES}等"
	read -r -p "1)是 2)否 " input
	case $input in
        1)
	echo -e "是否加载${YELLOW}共享文件夹${RES}"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	1|"") SHARE=true ;;
	*) ;;
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
	1) rm /mnt/hugepages* 2>/dev/null
	echo -e "${RED}注意！使用控制台不会因为qemu退出而自动删除大页文件，请退出后输rm /tmp/hugepage* /dev/hugepage*自行删除${RES}"
		set -- "${@}" "-monitor" "telnet:127.0.0.1:4444,server,nowait" "-daemonize"
		echo -e "${YELLOW}调试命令telnet 127.0.0.1 4444${RES}\n${YELLOW}#换光盘${RES}：先info block查看光盘标识，例如ide0-cd1，再用命令change ide0-cd1 /sdcard/xinhao/windows/DGDOS.iso\n${YELLOW}#热插拔内存${RES}：本脚本已对默认内存预留两个内存槽$(( $mem_ / 2 ))m\n输入命令\n(qemu) object_add memory-backend-ram,id=mem1,size=$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm0,memdev=mem1\n(qemu) object_add memory-backend-ram,id=mem2,size=$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm,memdev=mem2\n或者大页内存：\n(qemu) object_add memory-backend-file,id=mem1,size=$(( $mem_ / 2 ))m,mem-path=/mnt/hugepages-$(( $mem_ / 2 ))m\n(qemu) device_add pc-dimm,id=dimm1,memdev=mem1输入后可用info memdev或info memory-devices查看\n${YELLOW}#热插拔cpu${RES}：本脚本仅对默认smp的max预留一个cpu槽\n查可用cpu槽info hotpluggable-cpus(找到没有qom_path一组，记住type信息，CPUInstance Properties信息)\n输入格式(以提示为准)：device_add driver=qemu32-i386-cpu,socket-id=2,core-id=0,thread-id=0,node-id=0\n${YELLOW}#退出qemu${RES}，输quit\n"
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
	1) echo -e "${YELLOW}请插入usb${RES}"
	CONFIRM
	lsusb 2>/dev/null
	if [ $? == 1 ]; then
		echo -e "${RED}未能获取设备信息，请确认已获取系统权限${RES}"
	fi
	read -r -p "输入usb名称对应的bus序号(如001，请输1或者011，请输11) " HOSTBUS
	lsusb -t | grep -w "Bus 0$HOSTBUS" -A 4 2>/dev/null
	read -r -p "输入Port序号(如001，请输1或者011，请输11) " HOSTPORT
        set -- "${@}" "-device" "usb-ehci,id=ehci"
	set -- "${@}" "-device" "usb-host,bus=ehci.0,hostbus=$HOSTBUS,hostport=$HOSTPORT" ;;
	2) echo -e "\n${YELLOW}请插入usb${RES}"
	CONFIRM
	set -- "${@}" "-device" "usb-ehci,id=ehci"
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
#amd
####################
	case $(dpkg --print-architecture) in
	i*86|x86*|amd64)
#在KVM中内存允许过载使用，分配给客户机的内存总数可以大于实际可用的物理内存总数。客户机过载使用内存的上限是：宿主机可用物理内存空间和交换空间的大小之和。超过这个上限会使客户机因内存不足被强制关闭。		
	echo -e "${YELLOW}过量内存使用${RES}(默认关闭)"
	read -r -p "1)开启 2)关闭 " input
	case $input in
		1) set -- "${@}" "-overcommit" "mem-lock=on" ;;
		*) set -- "${@}" "-overcommit" "mem-lock=off" ;;
	esac
#KVM允许客户机过载使用（over-commit）CPU资源，即让一个或多个客户机使用vCPU的总数量超过宿主机实际拥有的物理CPU数量。但不建议单个客户机的CPU数量多于物理宿主机的CPU数量。
	echo -e "${YELLOW}过量cpu控制${RES}(默认关闭)"
	read -r -p "1) 开启 2)关闭 " input
	case $input in
		1) set -- "${@}" "-overcommit" "cpu-pm=on" ;;
		*) set -- "${@}" "-overcommit" "cpu-pm=off" ;;
	esac ;;
	esac
	uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	if [ -z "$HUGEPAGE" ]; then
	echo -e "创建${YELLOW}大页文件${RES}代替设备ram，可降低ram使用率，响应速度略降低)${RES}"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	1) HUGEPAGE=true ;;
	*) ;;
	esac
	fi
	fi
:<<\eof
echo -e "
1) 创建${YELLOW}大页文件${RES}代替设备ram，可降低ram使用率，响应速度略降低)${RES}
2) 加载${YELLOW}mem-prealloc${RES}参数(创建大页文件以指派内存占用，提高响应速度，${RED}测试无效，勿选${RES})
0) 跳过"
	read -r -p "请选择: " input
	case $input in
		1) HUGEPAGE=true ;;
		2)
#让meminfo文件中HugePages_Free数量的减少和分配给客户机的内存保持一致。getconf  PAGESIZE
	rm /tmp/hugepage* 2>/dev/null
		echo -n -e "请输入大页拟使用的占用数值(以m为单位，例如1800)，回车为默认值，请输入: "
        read mem_m
        if [ -n "$mem_m" ]; then
                set -- "${@}" "-mem-path" "/tmp/hugepage,share=on,size=${mem_m}m"
        else
#		set -- "${@}" "-mem-path" "/tmp/hugepage,share=yes,size=$(($mem_ * 1048576))"
		set -- "${@}" "-mem-path" "/tmp/hugepage,share=on,size=${mem_}m"
	fi
	set -- "${@}" "-mem-prealloc" ;;
	*) ;; esac
	fi
	fi
eof
	echo -e "是否加载${YELLOW}usb鼠标${RES}(提高光标精准度),少部分系统可能不支持"
	read -r -p "1)加载 2)不加载 " input
	case $input in
	2) ;;
	*) set -- "${@}" "-usb" "-device" "usb-tablet" ;;
	esac
#时间设置，RTC时钟，用于提供年、月、日、时、分、秒和星期等的实时时间信息，由后备电池供电，当你晚上关闭系统和早上开启系统时，RTC仍然会保持正确的时间和日期
#driftfix=slew i386存在时间漂移
	echo -e "请选择${YELLOW}系统时间${RES}标准"
	read -r -p "1)utc 2)localtime " input
	case $input in
	1) case $QEMU_SYS in
		qemu-system-i386) set -- "${@}" "-rtc" "base=utc,clock=host,driftfix=slew" ;;
		*) set -- "${@}" "-rtc" "base=utc,clock=host" ;;
	esac ;;
	*) set -- "${@}" "-rtc" "base=localtime" ;;
#       *) set -- "${@}" "-rtc" "base=`date +%Y-%m-%dT%T`" ;;
	esac
#strict=on|off 是否受宿主机网络控制
	echo -e "请选择${YELLOW}启动顺序${RES}"
	read -r -p "1)优先硬盘启动 2)优先光盘启动 " input
	case $input in
	1|"") set -- "${@}" "-boot" "order=cd,menu=on,strict=off" ;;
	*) set -- "${@}" "-boot" "order=dc,menu=on,strict=off" ;;
	esac ;;
	*)
        set -- "${@}" "-rtc" "base=localtime"
        set -- "${@}" "-boot" "order=cd,menu=on,strict=off"
        set -- "${@}" "-usb" "-device" "usb-tablet"
        ;;
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
		set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,if=virtio"
		set -- "${@}" "-cdrom" "${DIRECT}${STORAGE}$iso_name" ;;
		*)
		echo -e "\n请选择${YELLOW}磁盘接口${RES},因系统原因,sata可能导致启动不成功,virtio需系统已装驱动,回车为兼容方式"
	read -r -p "1)ide 2)sata 3)virtio 4)测试用(勿选) " input
	case $input in
##################
#IDE			
		1)
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,if=ide,index=0,media=disk,aio=threads,cache=writeback"
	if [ -n "$hdb_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hdb_name,if=ide,index=1,media=disk,aio=threads,cache=writeback"
	fi
	if [ -n "$iso1_name" ]; then
#		set -- "${@}" "-cdrom" "${DIRECT}${STORAGE}$iso1_name"
	set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso1_name,if=ide,media=cdrom,index=1"
	fi
	if [ -n "$iso_name" ]; then 
	       set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,if=ide,media=cdrom,index=2"
	fi
	case $SHARE in
		true) set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,if=ide,index=3,media=disk,aio=threads,cache=writeback" ;;
		*) ;;
	esac ;;
	2)

##################
#SATA        
	set -- "${@}" "-drive" "id=disk,file=${DIRECT}${STORAGE}$hda_name,if=none,cache=none"
	set -- "${@}" "-device" "ahci,id=ahci"
	set -- "${@}" "-device" "ide-hd,drive=disk,bus=ahci.0"

	if [ -n "$hdb_name" ]; then
	set -- "${@}" "-drive" "id=installmedia,file=${DIRECT}${STORAGE}$hdb_name,if=none,cache=none"
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
		set -- "${@}" "-usb" "-drive" "if=none,format=raw,id=disk1,file=fat:rw:${DIRECT}/xinhao/share/"
		set -- "${@}" "-device" "usb-storage,drive=disk1"
		;;
	*) ;;
	esac ;;
##################
#VIRTIO

	3) set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,index=0,media=disk,if=virtio,cache=none"
	if [ -n "$hdb_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hdb_name,index=1,media=disk,if=virtio,cache=none"
	fi
	if [ -n "$iso1_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso1_name,if=ide,media=cdrom,index=1"
	fi
		if [ -n "$iso_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,if=ide,media=cdrom,index=2"
		fi
	case $SHARE in
		true)
		set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,index=3,media=disk,if=virtio"
;;
		*) ;;
	esac ;;
##################
#test
		4) set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hda_name,index=0,media=disk"
	if [ -n "$hdb_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$hdb_name,index=1,media=disk"
	fi
	if [ -n "$iso_name" ]; then
		set -- "${@}" "-drive" "file=${DIRECT}${STORAGE}$iso_name,index=2,media=cdrom"
	fi
	case $SHARE in
		true)
		set -- "${@}" "-drive" "file=fat:rw:${DIRECT}/xinhao/share,index=3,media=disk,format=raw" ;;
		*) ;;
	esac
				;;
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
	case $HUGEPAGE in
		true)
	if echo ${@} | egrep -qw "512|1024|2048"; then
	if echo ${@} | grep -wq "512"; then
	set -- "${@}" "-object" "memory-backend-ram,id=mem,size=512m"
	set -- "${@}" "-numa" "node,memdev=mem"
	HMAT=",hmat=on"
	fi
	if echo ${@} | egrep -wq "1024|2048"; then
	set -- "${@}" "-object" "memory-backend-file,id=mem,size=1024m,mem-path=/tmp/hugepage,prealloc=on,share=on"
	set -- "${@}" "-numa" "node,memdev=mem"
	HMAT=",hmat=on"
	fi
	if echo ${@} | egrep -wq "2048"; then
	set -- "${@}" "-object" "memory-backend-file,id=pc.ram,size=1024m,mem-path=/tmp/hugepage1,prealloc=on,share=on"
	if echo ${@} | egrep -wq "maxcpus"; then
	set -- "${@}" "-numa" "node,memdev=pc.ram"
	else
	set -- "${@}" "-numa" "node,memdev=pc.ram,initiator=0"
	fi
	fi
#       set -- "${@}" "-numa" "node,memdev=mem0,initiator=0"
#       set -- "${@}" "-numa" "cpu,node-id=0,socket-id=0"
#        set -- "${@}" "-numa" "cpu,node-id=0,socket-id=1"
	else
	set -- "${@}" "-object" "memory-backend-file,id=pc.ram,size=${mem}m,mem-path=/tmp/hugepage,prealloc=on,share=on"
        set -- "${@}" "-numa" "node,memdev=pc.ram"
        HMAT=",hmat=on"
        fi
	;;

	*)
	if echo ${@} | grep -q 'mem-prealloc'; then
		echo ""
	else
	if echo ${@} | egrep -wq "512|1024|2048"; then
	case $SYS in
	QEMU_ADV)
	echo -e ""
        read -r -p "请回车 " input
        case $input in
        1)
        if echo ${@} | grep -wq "512"; then
	set -- "${@}" "-object" "memory-backend-ram,id=mem,size=512m"
	set -- "${@}" "-numa" "node,memdev=mem"
	HMAT=",hmat=on"
	fi
	if echo ${@} | egrep -wq "1024|2048"; then
	set -- "${@}" "-object" "memory-backend-ram,id=mem,size=1024m"
	set -- "${@}" "-numa" "node,memdev=mem"
	HMAT=",hmat=on"
	fi
        if echo ${@} | egrep -wq "2048"; then                 
	set -- "${@}" "-object" "memory-backend-ram,id=mem0,size=1024m"
	if echo ${@} | egrep -wq "maxcpus"; then
	set -- "${@}" "-numa" "node,memdev=mem0"
	else
	set -- "${@}" "-numa" "node,memdev=mem0,initiator=0"
	fi
        fi ;;
        *) ;;
        esac ;;

        esac
	fi
	fi ;;
	esac
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
	if [[ $(qemu-system-i386 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [1-4] ]]; then
	unset HMAT
	fi
	echo -e "请选择${YELLOW}计算机类型${RES}，默认pc，因系统原因，q35可能导致启动不成功"
MA="vmport=off,dump-guest-core=off,mem-merge=off,kernel-irqchip=off"
#enforce-config-section=on
TCG="tcg,thread=multi"
	read -r -p "1)pc 2)q35 " input
	case $input in
	1|"")
	case $(dpkg --print-architecture) in
	arm*|aarch64)
	case $SYS in
		QEMU_PRE) set -- "-machine" "pc" "--accel" "$TCG" "${@}" ;;
		*)
        echo -e "\n请选择${YELLOW}加速${RES}方式(理论上差不多，但貌似指定tcg更流畅点，请自行体验)"
	read -r -p "1)tcg 2)自动检测 3)锁定tcg缓存 " input
	case $input in
		1)
	set -- "-machine" "pc,$MA$HMAT,usb=off" "--accel" "$TCG" "${@}" ;;
	3) if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [4-9] ]]; then
	echo -e "${RED}注意！设置tcg的缓存可以提高模拟效率，以m为单位，跟手机闪存ram也有关系(调高了会出现后台杀)，请谨慎设置${RES}"
	echo -n -e "请输入拟缓存的数值(以m为单位，例如1800)，回车为默认值，请输入: "
	read TB
	if [ -n "$TB" ]; then
	set -- "-machine" "pc,$MA$HMAT,usb=off" "--accel" "$TCG,tb-size=$TB" "${@}"
	else
	set -- "-machine" "pc,$MA$HMAT,usb=off" "--accel" "$TCG,tb-size=1024" "${@}"
        fi
        else
	set -- "-machine" "pc,$MA$HMAT,usb=off" "--accel" "$TCG" "${@}"
	fi ;;
        *) set -- "-machine" "pc,accel=kvm:xen:hax:tcg,$MA$HMAT,usb=off" "${@}" ;;
        esac ;;
        esac ;;
	*)
	set -- "-machine" "pc,accel=kvm:xen:hax:tcg,usb=on,dump-guest-core=on$HMAT" "${@}" ;;
	esac ;;
        2) echo -e ${BLUE}"如果无法进入系统，请选择pc${RES}"
        case $(dpkg --print-architecture) in
                arm*|aarch64)
        case $SYS in
                QEMU_PRE) set -- "-machine" "q35" "--accel" "$TCG" "${@}" ;;
		*)
                echo -e "\n请选择${YELLOW}加速${RES}方式(理论上差不多，但貌似指定tcg更流畅点，请自行体验)"
        read -r -p "1)tcg 2)自动检测 3)锁定tcg缓存 " input
        case $input in
                1) set -- "-machine" "q35,$MA$HMAT,usb=off" "--accel" "$TCG" "${@}" ;;
                3) if [[ $(qemu-system-x86_64 --version | grep version | awk -F "." '{print $1}' | awk '{print $4}') = [4-9] ]]; then
        echo -e "${RED}注意！设置tcg的缓存可以提高模拟效率，以m为单位，跟手机闪存ram也有关系(调高了会出现后台杀)，请谨慎设置${RES}"
        echo -n -e "请输入拟缓存的数值(以m为单位，例如1800)，回车为默认值，请输入: "
        read TB
        if [ -n "$TB" ]; then
        set -- "-machine" "q35,$MA$HMAT,usb=off" "--accel" "$TCG,tb-size=$TB" "${@}"
        else
        set -- "-machine" "q35,$MA$HMAT,usb=off" "--accel" "$TCG,tb-size=1024" "${@}"
        fi
	else
	set -- "-machine" "pc,$MA$HMAT,usb=off" "--accel" "$TCG" "${@}"
        fi ;;
	*) set -- "-machine" "q35,accel=kvm:xen:hax:tcg,$MA$HMAT,usb=off" "${@}" ;;
        esac ;;
        esac ;;
	*) set -- "-machine" "q35,accel=kvm:xen:hax:tcg,usb=on,dump-guest-core=on$HMAT" "${@}" ;;
	esac ;;
	esac
########################
	if [ -n "$display" ]; then
	case $display in
		wlan_vnc) set -- "${@}" "-display" "vnc=$IP:0" ;;
		vnc) 
		set -- "${@}" "-display" "vnc=127.0.0.1:0,lossy=on,non-adaptive=off"
		export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		xsdl)
			export DISPLAY=127.0.0.1:0
			export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		spice) set -- "${@}" "-spice" "port=5900,addr=127.0.0.1,disable-ticketing,seamless-migration=off"
			export PULSE_SERVER=tcp:127.0.0.1:4713 ;;
		spice_) set -- "${@}" "-spice" "port=5900,addr=127.0.0.1,disable-ticketing,seamless-migration=off"
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
	uname -a | grep 'Android' -q 
	if [ $? != 0 ]; then
		case $display in
		wlan_vnc) ;;
		*)
	echo -e "创建本次参数的${YELLOW}快捷脚本${RES}"
	read -r -p "1)是 2)否 " input
	case $input in
		1) echo -n "请给脚本起个名字: "
	read script_name


	case $display in
		xsdl)
cat >/usr/local/bin/$script_name <<-EOF
killall -9 qemu-system-x86 2>/dev/null
killall -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
export DISPLAY=127.0.0.1:0
${@}
EOF
;;
		vnc|spice|spice_) 
cat >/usr/local/bin/$script_name <<-EOF
killall -9 qemu-system-x86 2>/dev/null
killall -9 qemu-system-i38 2>/dev/null
export PULSE_SERVER=tcp:127.0.0.1:4713
${@}
EOF
;;
		gtk_)
cat >/usr/local/bin/$script_name <<-EOF
killall -9 qemu-system-x86 2>/dev/null
killall -9 qemu-system-i38 2>/dev/null
${@}
EOF
;;
		amd)
cat >${HOME}/xinhao/$script_name <<-EOF
killall -9 qemu-system-x86 2>/dev/null
killall -9 qemu-system-i38 2>/dev/null
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
	echo '如共享目录成功加载，请在浏览器地址输 \\10.0.2.4'
	echo -e "${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}"
	if echo "${@}" | grep -q monitor; then
	echo -e "\n${YELLOW}调试命令：telnet 127.0.0.1 4444${RES}"
	else
	trap " rm /tmp/hugepage* /mnt/hugepages* 2>/dev/null;exit" SIGINT EXIT
	fi
	sleep 1
	"${@}" >/dev/null 2>>${HOME}/.utqemu_log
	if [ $? == 1 ]; then
	FAIL
	echo -e "${RED}启动意外中止，请查看日志d(ŐдŐ๑)${RES}\n"
	fi
	exit 0 ;;


	esac
	chmod +x /usr/local/bin/$script_name 2>/dev/null
	echo -e "已保存本次参数的脚本，下次可直接输${GREEN}$script_name${RES}启动qemu"
	sleep 2 ;;
		*) ;;
	esac ;;
	esac
	fi
	printf "%s\n"
	cat <<-EOF
	${@}
	EOF
	case $display in
		vnc) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer 127.0.0.1:0\n" ;;
		wlan_vnc) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开vncviewer $IP:0\n" ;;
		xsdl) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开xsdl\n" ;;
		spice|spice_) printf "%s\n${BLUE}启动模拟器\n${GREEN}请打开aspice 127.0.0.1 端口 5900\n" ;;
		*) printf "%s\n${GREEN}启动模拟器\n" ;;
	esac
	uname -a | grep 'Android' -q
	if [ $? != 0 ]; then
	echo '如共享目录成功加载，请在浏览器地址输 \\10.0.2.4'
	fi
	if echo "${@}" | grep -q monitor; then
	echo -e "\n${YELLOW}调试命令：telnet 127.0.0.1 4444${RES}"
	else
	trap " rm /tmp/hugepage* /mnt/hugepages* 2>/dev/null;exit" SIGINT EXIT
	if echo "${@}" | grep -q hugepage 2>/dev/null; then
	echo -e "${GREEN}你使用了大页内存，开始模拟器前需要时间创建同内存大小文件，文件会在qemu退出后自动删除${RES}"
	fi
	fi
	echo -e  "${YELLOW}如启动失败请ctrl+c退回shell，并查阅日志${RES}"
	sleep 1
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
3) 为磁盘接口添加virtio驱动（自定义模式，载virtio驱动光盘)" ;;
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
	curl -O https://cdn.jsdelivr.net/gh/chungyuhoi/script/gpu.tar.gz
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
	killall -9 qemu-system-x86 2>/dev/null
	killall -9 qemu-system-i38 2>/dev/null
#	pkill -9 qemu-system-x86
#	pkill -9 qemu-system-i38
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
		1) read -r -p "1)北外源 2)腾讯源 3)清华源 9)返回 " input
	case $input in
	1) sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list
sed -i 's@^\(deb.*games stable\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/game-packages-24 games stable@' $PREFIX/etc/apt/sources.list.d/game.list
sed -i 's@^\(deb.*science stable\)$@#\1\ndeb https://mirrors.bfsu.edu.cn/termux/science-packages-24 science stable@' $PREFIX/etc/apt/sources.list.d/science.list ;;
	2) sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.cloud.tencent.com/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list
	sed -i 's@^\(deb.*games stable\)$@#\1\ndeb https://mirrors.cloud.tencent.com/termux/game-packages-24 games stable@' $PREFIX/etc/apt/sources.list.d/game.list
	sed -i 's@^\(deb.*science stable\)$@#\1\ndeb https://mirrors.cloud.tencent.com/termux/science-packages-24 science stable@' $PREFIX/etc/apt/sources.list.d/science.list ;;
	3) sed -i 's@^\(deb.*stable main\)$@#\1\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable main@' $PREFIX/etc/apt/sources.list
	sed -i 's@^\(deb.*games stable\)$@#\1\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/game-packages-24 games stable@' $PREFIX/etc/apt/sources.list.d/game.list
	sed -i 's@^\(deb.*science stable\)$@#\1\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/science-packages-24 science stable@' $PREFIX/etc/apt/sources.list.d/science.list ;;
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
	1) 直接运行，termux(utermux)目前版本为5.0以上，由于termux源的qemu编译的功能不全，强烈建议在容器上使用qemu，\e[33m其他系统的版本各不一样，一些功能参数可能没被编译进去${RES}
	2) 支持qemu5.0以下版本容器(选项内容比较简单，模拟xp建议此版本)
	3）支持qemu5.0以上版本容器(选项内容丰富)
	4) 换源(如果无法安装或登录请尝试此操作)
	5) 在线脚本安装体验linux系统(debian)
	6) 在线脚本安装体验linux系统(ubuntu)
	7) 下载新版termux

	9) 设置打开termux(utermux)自动启动本脚本
	0) 退出\n"
	read -r -p "请选择: " input
	case $input in
	1) QEMU_SYSTEM ;;
	2) uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	DEBIAN=buster
	sys_name=buster-qemu
	if [ -d $(pwd)/buster-qemu ]; then
		LOGIN
	else
		SYS_DOWN
		fi
		LOGIN
		fi ;;
	3) uname -a | grep 'Android' -q
	if [ $? == 0 ]; then
	DEBIAN=bullseye
	sys_name=bullseye-qemu
	if [ -d $(pwd)/bullseye-qemu ]; then
		LOGIN
	else
		SYS_DOWN
		fi
		LOGIN
		fi ;;
	4) SOURCE ;;
	5) bash -c "$(curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/bullseye.sh)" ;;
	6) bash -c "$(curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/focal.sh)" ;;
	7) echo -e "\n${YELLOW}检测最新版本${RES}"
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
	9) read -r -p "1)开机启动脚本 2)取消开机启动脚本 " input
	case $input in
	1) curl https://cdn.jsdelivr.net/gh/chungyuhoi/script/utqemu.sh -o ${HOME}/utqemu.sh
	echo "bash utqemu.sh" >>${PREFIX}/etc/bash.bashrc ;;
	*) sed -i "/utqemu/d" ${PREFIX}/etc/bash.bashrc ;;
	esac
	MAIN ;;
	0) exit 0 ;;
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
