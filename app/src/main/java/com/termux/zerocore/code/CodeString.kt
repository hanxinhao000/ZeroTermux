package com.termux.zerocore.code


/**
 *
 * 管理命令类
 *
 */

public object CodeString {


    public val QH:String = "sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list && sed -i 's@^\\(deb.*games stable\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/game-packages-24 games stable@' \$PREFIX/etc/apt/sources.list.d/game.list && sed -i 's@^\\(deb.*science stable\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/science-packages-24 science stable@' \$PREFIX/etc/apt/sources.list.d/science.list && apt update && apt upgrade \n"
    public val BJ:String = "sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.bfsu.edu.cn/termux/termux-packages-24 stable main@' \$PREFIX/etc/apt/sources.list &&" +
        "sed -i 's@^\\(deb.*games stable\\)$@#\\1\\ndeb https://mirrors.bfsu.edu.cn/termux/game-packages-24 games stable@' \$PREFIX/etc/apt/sources.list.d/game.list &&" +
        "sed -i 's@^\\(deb.*science stable\\)$@#\\1\\ndeb https://mirrors.bfsu.edu.cn/termux/science-packages-24 science stable@' \$PREFIX/etc/apt/sources.list.d/science.list &&" +
        "apt update && apt upgrade \n"
    public val UpDate:String = "pkg update -y \n"




}
