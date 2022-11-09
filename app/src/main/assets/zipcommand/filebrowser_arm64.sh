#!/data/data/com.termux/files/usr/bin/sh
echo "欢迎安装filebrowser(arm64)"
echo "作者地址:https://github.com/filebrowser/filebrowser"
echo "安装成功后开机自启"
echo "端口:19951"
echo "十秒钟后开始安装，请阅读"
sleep 10
echo "[*]开始安装"

cd ~

cd .filebrowser

tar zxvf linux-arm64.tar.gz

rm linux-arm64.tar.gz

chmod 777 filebrowser



echo "安装完成！重启后运行"
sleep 2

