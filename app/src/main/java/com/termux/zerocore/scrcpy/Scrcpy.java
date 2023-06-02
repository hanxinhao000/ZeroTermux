package com.termux.zerocore.scrcpy;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.Surface;


import com.termux.zerocore.scrcpy.decoder.VideoDecoder;
import com.termux.zerocore.scrcpy.model.ByteUtils;
import com.termux.zerocore.scrcpy.model.MediaPacket;
import com.termux.zerocore.scrcpy.model.VideoPacket;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;


public class Scrcpy extends Service {

    private String serverAdr;
    private Surface surface;
    private int screenWidth;
    private int screenHeight;
    private byte[] event = null;
    private VideoDecoder videoDecoder;
    private AtomicBoolean updateAvailable = new AtomicBoolean(false);
    private IBinder mBinder = new MyServiceBinder();
    private boolean first_time = true;
    private AtomicBoolean LetServceRunning = new AtomicBoolean(true);
    private ServiceCallbacks serviceCallbacks;
    private int[] remote_dev_resolution = new int[2];
    private boolean socket_status = false;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setServiceCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public void setParms(Surface NewSurface, int NewWidth, int NewHeight) {
        this.screenWidth = NewWidth;
        this.screenHeight = NewHeight;
        this.surface = NewSurface;
        if (videoDecoder == null) {
            this.videoDecoder = new VideoDecoder();
        }
        videoDecoder.start();
        updateAvailable.set(true);

    }

    public void start(Surface surface, String serverAdr, int screenHeight, int screenWidth) {
        if (videoDecoder == null) {
            this.videoDecoder = new VideoDecoder();
        }
        videoDecoder.start();
        this.serverAdr = serverAdr;
        this.screenHeight = screenHeight;
        this.screenWidth = screenWidth;
        this.surface = surface;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startConnection();
            }
        });
        thread.start();
    }

    public void pause() {
        videoDecoder.stop();
    }

    public void resume() {
        videoDecoder.start();
        updateAvailable.set(true);
    }

    public void StopService() {
        LetServceRunning.set(false);
        stopSelf();
    }


    public boolean touchevent(MotionEvent touch_event, int displayW, int displayH) {

        int[] buf = new int[]{touch_event.getAction(), touch_event.getButtonState(), (int) touch_event.getX() * screenWidth / displayW, (int) touch_event.getY() * screenHeight / displayH};
        final byte[] array = new byte[buf.length * 4]; // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        event = array;
        return true;
    }

    public int[] get_remote_device_resolution(){
        return remote_dev_resolution;
    }

    public boolean check_socket_connection(){
        return socket_status;
    }

    public void sendKeyevent(int keycode) {
        int[] buf = new int[]{keycode};

        final byte[] array = new byte[buf.length * 4];   // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (int j = 0; j < buf.length; j++) {
            final int c = buf[j];
            array[j * 4] = (byte) ((c & 0xFF000000) >> 24);
            array[j * 4 + 1] = (byte) ((c & 0xFF0000) >> 16);
            array[j * 4 + 2] = (byte) ((c & 0xFF00) >> 8);
            array[j * 4 + 3] = (byte) (c & 0xFF);
        }
        event = array;
    }

    private void startConnection() {
        videoDecoder = new VideoDecoder();
        videoDecoder.start();
        DataInputStream dataInputStream;
        DataOutputStream dataOutputStream;
        Socket socket = null;
        VideoPacket.StreamSettings streamSettings = null;
        int attempts = 50;
        while (attempts != 0) {
            try {
//                Log.e("Scrcpy","Connecting to " + serverAdr);
                socket = new Socket(serverAdr, 7007);
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
                byte[] packetSize;
                attempts = 0;
                byte[] buf = new byte[16];
                dataInputStream.read(buf, 0,16);
                for(int i =0; i<remote_dev_resolution.length; i++) {
                    remote_dev_resolution[i] = (((int) (buf[i * 4]) << 24) & 0xFF000000) |
                            (((int) (buf[i * 4 + 1]) << 16) & 0xFF0000) |
                            (((int) (buf[i * 4 + 2]) << 8) & 0xFF00) |
                            ((int) (buf[i * 4 + 3]) & 0xFF);
                }
                if (remote_dev_resolution[0] > remote_dev_resolution[1]){first_time = false;
                int i = remote_dev_resolution[0];
                remote_dev_resolution[0] = remote_dev_resolution[1];
                remote_dev_resolution[1] = i;
                }
                socket_status = true;
//                    Log.e("Remote device res", String.valueOf(remote_dev_resolution[0]+" x "+remote_dev_resolution[1]));
                while (LetServceRunning.get()) {
                    try {
                        if (event != null) {
                            dataOutputStream.write(event, 0, event.length);
                            event = null;
                        }

                        if (dataInputStream.available() > 0) {
                            packetSize = new byte[4];
                            dataInputStream.readFully(packetSize, 0, 4);
                            int size = ByteUtils.bytesToInt(packetSize);
                            byte[] packet = new byte[size];
                            dataInputStream.readFully(packet, 0, size);
                            VideoPacket videoPacket = VideoPacket.fromArray(packet);
                            if (videoPacket.type == MediaPacket.Type.VIDEO) {
                                byte[] data = videoPacket.data;
                                if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get()) {
                                    if (!updateAvailable.get()) {
                                        streamSettings = VideoPacket.getStreamSettings(data);
                                        if (!first_time) {
                                            if (serviceCallbacks != null) {
                                                serviceCallbacks.loadNewRotation();
                                            }
                                            while (!updateAvailable.get()) {
                                                // Waiting for new surface
                                                try {
                                                    Thread.sleep(100);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                        }
                                    }
                                    updateAvailable.set(false);
 //                                   first_time = false;
                                    videoDecoder.configure(surface, screenWidth, screenHeight, streamSettings.sps, streamSettings.pps);
                                } else if (videoPacket.flag == VideoPacket.Flag.END) {
                                    // need close stream
                                } else {
                                    videoDecoder.decodeSample(data, 0, data.length, 0, videoPacket.flag.getFlag());
                                }
                                first_time = false;
                            }

                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException e) {
              //  e.printStackTrace();
                attempts = attempts - 1;
                if (attempts == 0){
                    socket_status = false;
                    return;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignore) {
                }
//                 Log.e("Scrcpy", e.getMessage());
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }

    public interface ServiceCallbacks {
        void loadNewRotation();
    }

    public class MyServiceBinder extends Binder {
        public Scrcpy getService() {
            return Scrcpy.this;
        }
    }
    

}
