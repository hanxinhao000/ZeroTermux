package com.termux.zerocore.socket;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.dialog.SwitchDialog;
import com.termux.zerocore.socket.config.HelpConfig;
import com.termux.zerocore.socket.config.KnowConfig;
import com.termux.zerocore.socket.config.ZTKeyConstants;
import com.zp.z_file.util.LogUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ZTSocketService extends Service {
    private static final String TAG = "AndroidSocketService";
    public static final String ZT_COMMAND_ACTIVITY_ACTION = "zt_command_activity";
    public static final String ZT_COMMAND_SERVICES_ACTION = "zt_command_services";
    private static final int JAVA_PORT = 19951;

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private boolean isRunning = true;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d(TAG, "ZT服务创建");
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        startSocketService();
    }

    private void sendMessageToActivity(String message) {
        Intent intent = new Intent(ZT_COMMAND_ACTIVITY_ACTION);
        intent.putExtra("message", message);
        localBroadcastManager.sendBroadcast(intent);
    }


    // 接收来自 Activity 的消息
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            // 处理来自 Activity 的消息
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 注册接收器
        IntentFilter filter = new IntentFilter(ZT_COMMAND_SERVICES_ACTION);
        localBroadcastManager.registerReceiver(messageReceiver, filter);

        return START_STICKY;
    }

    private void startSocketService() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(JAVA_PORT);
                executor = Executors.newFixedThreadPool(5);
                LogUtils.d(TAG, "Java Socket服务已启动，端口: " + JAVA_PORT);

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    executor.execute(new ClientHandler(clientSocket));
                }
            } catch (IOException e) {
                e.printStackTrace();
                LogUtils.e(TAG, "服务启动失败 " + e);
            }
        }).start();
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {
                String command = in.readLine();
                LogUtils.d(TAG, "收到命令: " + command);

                String result = processCommand(command);
                out.println(result);
                LogUtils.d(TAG, "返回结果: " + result);

            } catch (IOException e) {
                e.printStackTrace();
                LogUtils.e(TAG, "处理客户端请求失败" + e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    LogUtils.e(TAG, "关闭客户端连接失败" + e);
                }
            }
        }

        private String processCommand(String command) {
            try {
                LogUtils.i(TAG, "processCommand command: " + command);
                // 如果什么命令都没有则返回帮助
                if (TextUtils.isEmpty(command)) {
                    return new HelpConfig().getCommand(getApplicationContext(), command);
                }
                // 判断是否是 多段命令
                // 示例： xxx yyy 例如: toast hello
                String[] commands = command.trim().split(" ");
                if (commands.length >= 2) {
                    return askConfig(command, commands[0]);
                }
                // 转发到termux页面
                if (isForWard(command, null)) {
                    sendMessageToActivity(command);
                    return getOkJson();
                }
                return askConfig(command, null);
            } catch (Exception e) {
                e.printStackTrace();
                return getJson(1, e.toString(), "");
            }
        }
    }

    /**
     *  请求对应的config ID
     *  注意：需要在 ZTKeyConstants 定义 ID
     *  需要在此处链接你的config: ZTCommandConfigStore
     * @param command 用户直接输入的命令行
     * @param commandID 有些命令行是 xx yy，示例：toast hello 这种格式的，传入的时候只需要传入 xx即可,
     *                  如果是单一的命令，看个人情况是否传递，此参数会传递到
     *                  你的 config -> public String getCommand(Context context, String command)
     * @return 返回自定义CONFIG的执行结果
     */
    private String askConfig(String command, String commandID) {
        return ZTCommandConfigStore.getConfig(TextUtils.isEmpty(commandID) ? command : commandID)
            .getCommand(getApplicationContext(), command);
    }
    // 是否需要转发到 TermuxActivity 页面
    private boolean isForWard(String command, String commandID) {
        return ZTCommandConfigStore.getConfig(TextUtils.isEmpty(commandID) ? command : commandID).isForWard();
    }

    private String getJson(int code, String message, String title) {
        return "{\"message\": \"" + message + "\",\"code\": " + code + ",\"title\": \"" + title + "\"}";
    }

    private String getOkJson() {
        return getJson(0, UUtils.getString(R.string.成功), "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (executor != null) executor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.e(TAG, "关闭服务失败" + e);
        }
        localBroadcastManager.unregisterReceiver(messageReceiver);
        LogUtils.d(TAG, "服务已销毁");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
