#!/data/data/com.termux/files/usr/bin/sh
echo ""
echo "---------------------------------------------------------"
echo ""
echo ""
echo "欢迎安装 Win7 学习版本"
echo "Welcome to install Win7 learning version"
echo ""
echo "作者 海"
echo "Author Sea"
echo ""
echo "本包的WIN7镜像来源于互联网"
echo "The WIN7 mirror of this package comes from the Internet"
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

wget http://od.ixcmstudio.cn/repository/main/windows/win7P.tar.gz

tar zxvf win7P.tar.gz

rm win7P.tar.gz

chmod 777 start-win7.sh

./start-win7.sh



echo "安装完成！即将启动(start-win7.sh)"
echo "The installation is complete! About to start (start-win7.sh)"
sleep 2

