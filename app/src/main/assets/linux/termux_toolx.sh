#!/usr/bin/env bash

clear

##################
YELLOW="\e[1;33m"
GREEN="\e[1;32m"
RED="\e[1;31m"
BLUE="\e[1;34m"
PINK="\e[0;35m"
RES="\e[0m"
##################
echo -e "\n${GREEN}部分系统没相应的架构包，可能会导致下载不成功d(ŐдŐ๑)                                              
${BLUE}手上资源有限，无法每个架构都通过测试(>﹏<)${RES}\n"

##################
ARCH_(){
	case $(dpkg --print-architecture) in
		arm64|aarch*)
			echo "arm64"
			ARCH=arm64 ;;
		x86_64|amd64) 
			echo "amd64"
			ARCH=amd64 ;;
		i*86|x86)
			echo "i386"
			ARCH=i386 ;;
		armv7*|armv8l)
			echo "armhf"
			ARCH=armhf ;;
		armv6*|armv5*)
			echo "armel"
			ARCH=armel ;;
		ppc*)
			echo "ppc64el"
			ARCH=ppc64el ;;
		s390*)
			echo "s390x"
			ARCH=s390x ;;
		*) echo "不被识别"
		echo -e "\nexit...\n"
		sleep 1
		exit ;;
esac
}
#####################
INVALID_INPUT() {
	echo -e "${RED}输入无效，请重输${RES}" \\n
	sleep 1
}
#####################
CONFIRM() {
	read -r -p "按回车键继续" input
	case $input in
		*) ;; esac
}
#####################
SYS_SELECT() {
	echo -e "\n\e[33m请选择系统\n
	1) debian\n
	2) ubuntu\n
	3) kali\n
	4) centos\n
	5) arch\n
	6) fedora\n
	7) XINHAO_HAN 先占个位\n
	9) 返回主目录\n 
	0) 退出\n${RES}"
read -r -p "请选择:" input
case $input in
	1) echo -e "\n选择debian哪个版本\n
		1) buster\n
		2) bullseye\n
		3) sid\n
		9) 返回主目录\n
		0) 退出\n"
		read -r -p "请选择:" input
		case $input in
			1) echo "即将下载安装debian(buster)"
		sys_name=buster
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/debian/buster/$ARCH/default/" ;;
	2) echo "即将下载安装debian(bullseye)"        
		sys_name=bullseye  
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/debian/bullseye/$ARCH/default/" ;;
	3) echo "即将下载安装debian(sid)"
		sys_name=sid
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/debian/sid/$ARCH/default/" ;;
		9) MAIN ;;
		0) echo -e "\nexit"
			sleep 1                           
			exit 0 ;;                           
	*) INVALID_INPUT                                     
		SYS_SELECT ;;
esac ;;
2) echo -e "\n选择ubuntu哪个版本\n                             
	1) bionic\n
	2) focal\n
	3) groovy\n
	9) 返回主目录\n                                     
	0) 退出\n"                                          
	read -r -p "请选择:" input                          
	case $input in
	1) echo "即将下载安装ubuntu(bionic)"
		sys_name=bionic
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/ubuntu/bionic/$ARCH/default/" ;;
	2) echo "即将下载安装ubuntu(focal)"
		sys_name=focal
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/ubuntu/focal/$ARCH/default/" ;;
	3) echo "即将下载安装ubuntu(groovy)"
		sys_name=groovy
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/ubuntu/groovy/$ARCH/default/" ;;
	9) MAIN ;;                         
		0) echo -e "\nexit"     
			sleep 1                               
			exit 0 ;;                         
		*) INVALID_INPUT                             
			SYS_SELECT ;;
	esac ;;
	3) echo "即将下载安装kali"
                sys_name=kali
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/kali/current/$ARCH/default/" ;;
	4) echo "即将下载安装centos"
                sys_name=centos
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/centos/8/$ARCH/default/" ;;
	5) echo "即将下载安装arch"
                sys_name=arch
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/archlinux/current/$ARCH/default/" ;;
	6) echo "即将下载安装fedora"
		sys_name=fedora       
		DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/fedora/33/$ARCH/default/" ;;
	7) echo "别乱选"
		sleep 1
		SYS_SELECT
                #sys_name=alpine
		#DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/alpine/3.10/arm64/default/"
		;;
	9) MAIN ;;
	0) echo -e "\nexit..."
		sleep 1
		exit 0 ;;

	*) INVALID_INPUT
		SYS_SELECT ;;
esac
}
#####################
SYS_SELECT_() {
        echo -e "\n\e[33m请选择系统\n
        1) debian\n
        2) ubuntu\n
        3) kali\n
        4) centos\n
        5) arch\n
        6) fedora\n
	7) XINHAO_HAN 先占个位\n
        9) 返回主目录\n
	0) 退出\n${RES}"
read -r -p "请选择:" input
case $input in
	1) echo -e "\n选择debian哪个版本\n          
		1) buster\n                          
		2) bullseye\n
		3) sid\n
		9) 返回主目录\n                    
		0) 退出\n"                    
		read -r -p "请选择:" input           
		case $input in
        1) echo "即将下载安装debian(buster)"
                sys_name=buster
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/debian/buster/$ARCH/default/" ;;
	2) echo "即将下载安装debian(bullseye)"   
		sys_name=bullseye
		DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/debian/bullseye/$ARCH/default/" ;;
	3) echo "即将下载安装debian(sid)"
		sys_name=sid
		DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/debian/sid/$ARCH/default/" ;;
	9) MAIN ;;                          
	0) echo -e "\nexit..."              
		sleep 1                     
		exit 0 ;;
	*) INVALID_INPUT                        
		SYS_SELECT_ ;;
esac ;;
	2) echo -e "\n选择ubuntu哪个版本\n            
		1) bionic\n                        
		2) focal\n
		3) groovy\n
		9) 返回主目录\n                      
		0) 退出\n"                           
		read -r -p "请选择:" input                
		case $input in
        1) echo "即将下载安装ubuntu(bionic)"
                sys_name=bionic
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/ubuntu/bionic/$ARCH/default/" ;;
        2) echo "即将下载安装ubuntu(focal)"
                sys_name=focal
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/ubuntu/focal/$ARCH/default/" ;;
	3) echo "即将下载安装ubuntu(groovy)"
		sys_name=groovy
		DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/ubuntu/groovy/$ARCH/default/" ;;
	9) MAIN ;;                                       
	0) echo -e "\nexit..."                           
		sleep 1                                 
		exit 0 ;;            
	*) INVALID_INPUT                      
		SYS_SELECT_ ;;
esac ;;
        3) echo "即将下载安装kali"
                sys_name=kali
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/kali/current/$ARCH/default/" ;;
        4) echo "即将下载安装centos"
                sys_name=centos
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/centos/8/$ARCH/default/" ;;
        5) echo "即将下载安装arch"
                sys_name=arch
                DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/archlinux/current/$ARCH/default/" ;;
	6) echo "即将下载安装fedora"
		sys_name=fedora              
		DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/fedora/33/$ARCH/default/" ;;
        7) echo "别乱选"
		sleep 1
                SYS_SELECT_
                #sys_name=alpine
                #DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/alpine/3.10/arm64/default/"
                ;;
        9) MAIN ;;
	0) echo -e "\nexit..."
                sleep 1
                exit 0 ;;

        *) INVALID_INPUT
                SYS_SELECT_ ;;
esac
}

#####################

SYS_DOWN() {
BAGNAME="rootfs.tar.xz"
        if [ -e ${BAGNAME} ]; then
                rm -rf ${BAGNAME}
	fi
	curl -o ${BAGNAME} ${DEF_CUR}
		VERSION=`cat ${BAGNAME} | grep href | tail -n 2 | cut -d '"' -f 4 | head -n 1`
		curl -o ${BAGNAME} ${DEF_CUR}${VERSION}${BAGNAME}
		if [ $? -ne 0 ]; then
			echo -e "${RED}下载失败，请重输${RES}\n"
			MAIN
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
}
####################
SYS_SET() {
	echo "更新DNS"
	sleep 1
	echo "127.0.0.1 localhost" > $sys_name/etc/hosts
	rm -rf $sys_name/etc/resolv.conf &&
	echo "nameserver 223.5.5.5
nameserver 223.6.6.6" >$sys_name/etc/resolv.conf
echo "设置时区"
sleep 1
	echo "export  TZ='Asia/Shanghai'" >> $sys_name/root/.bashrc
	if grep -q 'ubuntu' "$sys_name/etc/os-release" ; then
        touch "$sys_name/root/.hushlogin"
fi
}
####################
FIN(){
echo "写入启动脚本"
sleep 1
cat > $sys_name.sh <<- EOM
#!/bin/bash
cd $(dirname $0)
## unset LD_PRELOAD in case termux-exec is installed
pulseaudio --start &
echo "" &
echo "欢迎来到$sys_name系统" &
unset LD_PRELOAD
command="proot"
command+=" --kill-on-exit"
command+=" --link2symlink"
command+=" -S $sys_name"
command+=""
command+=" -b $sys_name/root:/dev/shm"
## uncomment the following line to have access to the home directory of termux
#command+=" -b /data/data/com.termux/files/home:/root"
## uncomment the following line to mount /sdcard directly to /
command+=" -b /sdcard"
command+=" -w /root"
command+=" /usr/bin/env -i"
command+=" HOME=/root"
command+=" USER=root"
command+=" PATH=/usr/local/sbin:/usr/local/bin:/bin:/usr/bin:/sbin:/usr/sbin:/usr/games:/usr/local/games"
command+=" TERM=$TERM"
command+=" LANG=C.UTF-8"
command+=" /bin/bash --login"
com="\$@"
if [ -z "\$1" ];then
    exec \$command
else
    \$command -c "\$com"
fi
EOM

echo "授予启动脚本执行权限"
sleep 1
chmod +x $sys_name.sh
if [ -e ${PREFIX}/etc/bash.bashrc ]; then
	if ! grep -q 'pulseaudio' ${PREFIX}/etc/bash.bashrc; then
		sed -i "1i\pkill -9 pulseaudio" ${PREFIX}/etc/bash.bashrc
	fi
else
	sed -i "1i\pkill -9 pulseaudio" $sys_name.sh
fi
echo -e "${YELLOW}现在可以执行 ./$sys_name.sh 运行 $sys_name 了${RES}"
sleep 1
exit 1
}
####################


SYS_SELECT__(){

echo -e "\n你选择的是x86架构rootfs,将会通过qemu的模拟方式运行;
目前仍是测试阶段，故下载资源有限\n"
                CONFIRM
        if [ -e rootfs.tar.xz ]; then
        rm -rf rootfs.tar.xz
        fi
        echo -e "
	1) ubuntu(bionic)\n
        2) kali\n
	3) debian(buster)\n
	9) 返回主目录\n
	0) 退出\n"
        read -r -p "请选择:" input
        case $input in
                1) DEF_CUR="https://mirrors.tuna.tsinghua.edu.cn/lxc-images/images/ubuntu/bionic/amd64/default/" 
			sys_name=bionic-x86
			echo "下载x86的Ubuntu(bionic)系统..."   
			sleep 1 ;;
		2) DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/kali/current/amd64/default/"
			sys_name=kali-x86
			echo "下载x86的kali系统..."    
			sleep 1 ;;
		3) DEF_CUR="https://mirrors.bfsu.edu.cn/lxc-images/images/debian/buster/amd64/default/"
			sys_name=buster-x86
			echo "下载x86的debian(buster)系统..."  
			sleep 1 ;;
                0) echo -e "\nexit..."                
			sleep 1
			exit 0 ;;
                9) MAIN ;;
		*) INVALID_INPUT                        
			SYS_SELECT__ ;;
	esac
}
#########################
FIN_(){
echo "配置qemu"
sleep 2
mkdir termux_tmp && cd termux_tmp
CURL_T=`curl https://mirrors.bfsu.edu.cn/debian/pool/main/q/qemu/ | grep '\.deb' | grep 'qemu-user-static' | grep arm64 | tail -n 1 | cut -d '=' -f 3 | cut -d '"' -f 2`
curl -o qemu.deb https://mirrors.bfsu.edu.cn/debian/pool/main/q/qemu/$CURL_T
apt install binutils
ar -vx qemu.deb
tar xvf data.tar.xz
cd && cp termux_tmp/usr/bin/qemu-x86_64-static $sys_name/ && rm -rf termux_tmp
echo "删除临时文件"
sleep 1
echo "创建登录系统脚本"
sleep 1
echo "pkill -9 pulseaudio 2>/dev/null
pulseaudio --start &
unset LD_PRELOAD
proot --kill-on-exit -S $sys_name --link2symlink -b $sys_name/root:/dev/shm -b /sdcard -q $sys_name/qemu-x86_64-static -w /root /usr/bin/env -i HOME=/root PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin TERM=xterm-256color LANG=en_US.UTF-8 LANGUAGE=en_US.UTF-8 /bin/bash" >$sys_name.sh
echo "赋予执行权限"
sleep 1
chmod +x $sys_name.sh
echo -e "${YELLOW}现在可以执行 ./$sys_name.sh 运行 $sys_name 了${RES}"
sleep 2
exit 1
}

####################
MAIN(){
	printf "\n经检测,你的CPU架构对应系统包为"
	ARCH_
	echo -e "\n${YELLOW}请选择系统下载地址(推荐北京外国语大学)\n
	1) 北京外国语大学
	2) 清华大学
	3) arm64模拟x86架构rootfs系统下载 (新增)
	0) 退出${RES}\n"
	read -r -p "请选择:" input
	case $input in
		1) SYS_SELECT ;;
		2) SYS_SELECT_ ;;
		3) SYS_SELECT__
			SYS_DOWN 
			SYS_SET 
			FIN_ ;;
		0) echo -e "\nexit..."
			sleep 1
			exit 1 ;;
		*) echo -e "${RED}无效选择，请重选${RES}"
			sleep 2
			MAIN ;;
	esac
	SYS_DOWN
	SYS_SET
	FIN
}
####################
MAIN "$@"
