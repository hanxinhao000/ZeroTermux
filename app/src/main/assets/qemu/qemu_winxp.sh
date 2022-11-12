#!/data/data/com.termux/files/usr/bin/sh
echo ""
echo "---------------------------------------------------------"
echo ""
echo ""
echo "欢迎安装 WinXP 学习版本"
echo "Welcome to install WinXP learning version"
echo ""
echo "作者 海"
echo "Author Sea"
echo ""
echo "本包的WIN XP镜像来源于互联网"
echo "The WIN XP mirror of this package comes from the Internet"
echo ""
echo "用途:学习研究"
echo "Purpose: study and research"
echo ""
echo "使用许可:学习研究"
echo "License: study and research"
echo ""
echo "本程序是为了测试qemu的研究与适用性,切勿用作于非法用途"
echo "This program is to test the research and applicability of qemu, and should not be used for illegal purposes"
echo ""
echo "十秒钟后开始安装，请阅读"
echo "---------------------------------------------------------"
sleep 10
echo "[*]开始安装"

cd ~

pkg update -y

pkg install wget proot git -y

wget https://od.ixcmstudio.cn/repository/main/windows/winxp.tar.gz

tar zxvf winxp.tar.gz

rm winxp.tar.gz

chmod 777 start-focal.sh

./start-focal.sh



echo "安装完成！即将启动(start-focal.sh)"
echo "The installation is complete! About to start (start-focal.sh)"
sleep 2

