package com.termux.zerocore.code


/**
 *
 * 管理命令类
 *
 */

public object CodeString {


    public val QH:String = "sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list && apt update && apt upgrade \n"
    public val BJ:String = "sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.bfsu.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list &&" +
        "apt update && apt upgrade \n"
    public val NJU:String = "sed -i 's@^\\(deb.*stable main\\)\$@#\\1\\ndeb https://mirror.nju.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list &&" +
        "sed -i 's@^\\(deb.*science stable\\)\$@#\\1\\ndeb https://mirror.nju.edu.cn/termux/science-packages-24 science stable@' \$PREFIX/etc/apt/sources.list.d/science.list &&" +
        "apt update && apt upgrade \n"
    public val USTC:String = "sed -i 's@packages.termux.org@mirrors.ustc.edu.cn/termux@' \$PREFIX/etc/apt/sources.list &&" +
        "pkg up \n"
    public val HEB:String = "sed -i 's@^\\(deb.*stable main\\)\$@#\\1\\ndeb https://mirrors.hit.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list &&" +
        "apt update && apt upgrade \n"
    public val UpDate:String = "pkg update -y \n"
    public val runLinuxSh:String = "cd ~ && cd ~ && chmod 777 linux.sh && ./linux.sh \n"
    public val runQemuSh:String = "cd ~ && cd ~ && chmod 777 utqemu.sh && ./utqemu.sh \n"
    public val runWin7Sh:String = "cd ~ && cd ~ && chmod 777 qemu_win7.sh && ./qemu_win7.sh \n"
    public val runWinXPSh:String = "cd ~ && cd ~ && chmod 777 qemu_winxp.sh && ./qemu_winxp.sh \n"
    public val runMoeSh:String = "pkg install -y curl ; bash -c \"$(curl -L gitee.com/mo2/linux/raw/2/2)\" \n"
    public val runsmsChomdSh:String = "cd .. && cd usr && cd bin && chmod 777 smsread && cd ~ \n"
    public val runPhoneChomdSh:String = "cd .. && cd usr && cd bin && chmod 777 readcontacts && cd ~ \n"
    public val runsmsInstallSh:String = "cd ~ && pkg update -y && pkg install vim -y \n"
    public val runstartSh:String = "cd ~ && cd ~ && cd .xinhao_history && chmod 777 start_command.sh && cd ~ \n"

    public var runLineQemu = "cd ~ && pkg update -y && pkg install wget -y && wget http://od.ixcmstudio.cn/repository/main/qemu_sh/utqemu.sh && chmod 777 utqemu.sh && ./utqemu.sh"



}
