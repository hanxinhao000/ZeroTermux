package com.termux.zerocore.scrcpy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.dialog.LoadingDialog;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class MainActivity extends Activity implements Scrcpy.ServiceCallbacks, SensorEventListener {
    private static final String PREFERENCE_KEY = "default";
    private static final String PREFERENCE_SPINNER_RESOLUTION = "spinner_resolution";
    private static final String PREFERENCE_SPINNER_BITRATE = "spinner_bitrate";
    private static int screenWidth;
    private static int screenHeight;
    private static boolean landscape = false;
    private static boolean first_time = true;
    private static boolean result_of_Rotation = false;
    private static boolean serviceBound = false;
    private static boolean nav = false;
    SensorManager sensorManager;
    private SendCommands sendCommands;
    private int videoBitrate;
    private String local_ip;
    private Context context;
    private String serverAdr = null;
    private SurfaceView surfaceView;
    private Surface surface;
    private Scrcpy scrcpy;
    private long timestamp = 0;
    private byte[] fileBase64;
    private static float remote_device_width;
    private static float remote_device_height;
    private LinearLayout linearLayout;
    private static boolean no_control = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            scrcpy = ((Scrcpy.MyServiceBinder) iBinder).getService();
            scrcpy.setServiceCallbacks(MainActivity.this);
            serviceBound = true;
           if (first_time) {
                scrcpy.start(surface, serverAdr, screenHeight, screenWidth);
               int count = 100;
               while (count!=0 && !scrcpy.check_socket_connection()){
                   count --;
                   try {
                       Thread.sleep(100);
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
               if (count == 0){
                   if (serviceBound) {
                       scrcpy.StopService();
                       unbindService(serviceConnection);
                       serviceBound = false;
                       scrcpy_main();
                   }
                   Toast.makeText(context, "Connection Timed out", Toast.LENGTH_SHORT).show();
               }else{
               int[] rem_res = scrcpy.get_remote_device_resolution();
               remote_device_height = rem_res[1];
               remote_device_width = rem_res[0];
               first_time = false;
               }
            } else {
                scrcpy.setParms(surface, screenWidth, screenHeight);
            }
            set_display_nd_touch();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    public MainActivity() {
    }
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (first_time) {
            scrcpy_main();
        } else {
            this.context = this;
            start_screen_copy_magic();
        }
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        Sensor proximity;
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);

    }


    @SuppressLint("SourceLockedOrientationActivity")
    public void scrcpy_main(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        final TextView startButton = findViewById(R.id.button_start);
        AssetManager assetManager = getAssets();
        try {
            InputStream input_Stream = assetManager.open("scrcpy-server.jar");
            byte[] buffer = new byte[input_Stream.available()];
            input_Stream.read(buffer);
            fileBase64 = Base64.encode(buffer, 2);
        } catch (IOException e) {
            Log.e("Asset Manager", e.getMessage());
        }
        sendCommands = new SendCommands();

        startButton.setOnClickListener(v -> {
           // UUtils.showMsg(UUtils.getString(R.string.adb_title_msg));
            LoadingDialog loadingDialog = new LoadingDialog(this);
            loadingDialog.show();
            loadingDialog.setCancelable(false);
            UUtils.runOnThread(new Runnable() {
                @Override
                public void run() {
                    local_ip = wifiIpAddress();
                    getAttributes();
                    if (!serverAdr.isEmpty()) {
                        if (sendCommands.SendAdbCommands(context, fileBase64, serverAdr, local_ip, videoBitrate, Math.max(screenHeight, screenWidth)) == 0) {
                            UUtils.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    start_screen_copy_magic();
                                    loadingDialog.dismiss();
                                }
                            });
                        } else {
                            UUtils.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(context, "Network OR ADB connection failed", Toast.LENGTH_SHORT).show();
                                    loadingDialog.dismiss();
                                }
                            });

                        }
                    } else {
                      UUtils.runOnUIThread(new Runnable() {
                          @Override
                          public void run() {
                              Toast.makeText(context, "Server Address Empty", Toast.LENGTH_SHORT).show();
                              loadingDialog.dismiss();
                          }
                      });
                    }
                }
            });

        });
        get_saved_preferences();
    }


    public void get_saved_preferences(){
        this.context = this;
        final EditText editTextServerHost = findViewById(R.id.editText_server_host);
        final Switch aSwitch0 = findViewById(R.id.switch0);
        final Switch aSwitch1 = findViewById(R.id.switch1);
        editTextServerHost.setText(context.getSharedPreferences(PREFERENCE_KEY, 0).getString("Server Address", ""));
        aSwitch0.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean("No Control", false));
        aSwitch1.setChecked(context.getSharedPreferences(PREFERENCE_KEY, 0).getBoolean("Nav Switch", false));
        setSpinner(R.array.options_resolution_keys, R.id.spinner_video_resolution, PREFERENCE_SPINNER_RESOLUTION);
        setSpinner(R.array.options_bitrate_keys, R.id.spinner_video_bitrate, PREFERENCE_SPINNER_BITRATE);
        if(aSwitch0.isChecked()){
            aSwitch1.setVisibility(View.GONE);
        }

        aSwitch0.setOnClickListener(v -> {
            if(aSwitch0.isChecked()){
                aSwitch1.setVisibility(View.GONE);
            }else{
                aSwitch1.setVisibility(View.VISIBLE);
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void set_display_nd_touch() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (ViewConfiguration.get(context).hasPermanentMenuKey()) {
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
        } else {
            final Display display = getWindowManager().getDefaultDisplay();
            display.getRealMetrics(metrics);
        }
        float this_dev_height = metrics.heightPixels;
        float this_dev_width = metrics.widthPixels;
        if (nav && !no_control){
            if (landscape){
                this_dev_width = this_dev_width - 96;
            }else {                                                 //100 is the height of nav bar but need multiples of 8.
                this_dev_height = this_dev_height - 96;
            }
        }
        float remote_device_aspect_ratio = remote_device_height/remote_device_width;

        if (!landscape) {                                                            //Portrait
            float this_device_aspect_ratio = this_dev_height/this_dev_width;
            if (remote_device_aspect_ratio > this_device_aspect_ratio) {
                linearLayout.setPadding((int) (((remote_device_aspect_ratio - this_device_aspect_ratio)*this_dev_width)/2),0,(int) (((remote_device_aspect_ratio - this_device_aspect_ratio)*this_dev_width)/2),0);
            } else if (remote_device_aspect_ratio < this_device_aspect_ratio) {
                linearLayout.setPadding(0,(int) (((this_device_aspect_ratio - remote_device_aspect_ratio)*this_dev_width)),0,0);
            }

        }else{                                                                        //Landscape
            float this_device_aspect_ratio = this_dev_width/this_dev_height;
            if (remote_device_aspect_ratio > this_device_aspect_ratio) {
                linearLayout.setPadding(0,(int) (((remote_device_aspect_ratio - this_device_aspect_ratio)*this_dev_height)/2),0,(int) (((remote_device_aspect_ratio - this_device_aspect_ratio)*this_dev_height)/2));
            } else if (remote_device_aspect_ratio < this_device_aspect_ratio) {
                linearLayout.setPadding(((int) (((this_device_aspect_ratio - remote_device_aspect_ratio)*this_dev_height))/2),0,((int) (((this_device_aspect_ratio - remote_device_aspect_ratio)*this_dev_height))/2),0);
            }

        }
        if (!no_control) {
            surfaceView.setOnTouchListener((v, event) -> scrcpy.touchevent(event, surfaceView.getWidth(), surfaceView.getHeight()));
        }

        if (nav && !no_control) {
            final Button backButton = findViewById(R.id.back_button);
            final Button homeButton = findViewById(R.id.home_button);
            final Button appswitchButton = findViewById(R.id.appswitch_button);

            backButton.setOnClickListener(v -> scrcpy.sendKeyevent(4));

            homeButton.setOnClickListener(v -> scrcpy.sendKeyevent(3));

            appswitchButton.setOnClickListener(v -> scrcpy.sendKeyevent(187));
        }
        }

    private void setSpinner(final int textArrayOptionResId, final int textViewResId, final String preferenceId) {

        final Spinner spinner = findViewById(textViewResId);
        ArrayAdapter<CharSequence> arrayAdapter = ArrayAdapter.createFromResource(this, textArrayOptionResId, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putInt(preferenceId, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putInt(preferenceId, 0).apply();
            }
        });
        spinner.setSelection(context.getSharedPreferences(PREFERENCE_KEY, 0).getInt(preferenceId, 0));
    }

    private void getAttributes() {

        final EditText editTextServerHost = findViewById(R.id.editText_server_host);
        serverAdr = editTextServerHost.getText().toString();
        context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putString("Server Address", serverAdr).apply();
        final Spinner videoResolutionSpinner = findViewById(R.id.spinner_video_resolution);
        final Spinner videoBitrateSpinner = findViewById(R.id.spinner_video_bitrate);
        final Switch a_Switch0 = findViewById(R.id.switch0);
        no_control = a_Switch0.isChecked();
        final Switch a_Switch1 = findViewById(R.id.switch1);
        nav = a_Switch1.isChecked();
        context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putBoolean("No Control", no_control).apply();
        context.getSharedPreferences(PREFERENCE_KEY, 0).edit().putBoolean("Nav Switch", nav).apply();

        final String[] videoResolutions = getResources().getStringArray(R.array.options_resolution_values)[videoResolutionSpinner.getSelectedItemPosition()].split(",");
            screenHeight = Integer.parseInt(videoResolutions[0]);
            screenWidth = Integer.parseInt(videoResolutions[1]);
            videoBitrate = getResources().getIntArray(R.array.options_bitrate_values)[videoBitrateSpinner.getSelectedItemPosition()];
    }


    private void swapDimensions() {
        int temp = screenHeight;
        screenHeight = screenWidth;
        screenWidth = temp;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void start_screen_copy_magic() {
//        Log.e("Scrcpy: ","Starting scrcpy service");
            setContentView(R.layout.surface);
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            surfaceView = findViewById(R.id.decoder_surface);
            surface = surfaceView.getHolder().getSurface();
        final LinearLayout nav_bar = findViewById(R.id.nav_button_bar);
        if(nav && !no_control) {
            nav_bar.setVisibility(LinearLayout.VISIBLE);
        }else {
            nav_bar.setVisibility(LinearLayout.GONE);
        }
            linearLayout = findViewById(R.id.container1);
            start_Scrcpy_service();
    }


    protected String wifiIpAddress() {
//https://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device-from-code
        try {
            InetAddress ipv4 = null;
            InetAddress ipv6 = null;
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface int_f = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = int_f
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet6Address) {
                        ipv6 = inetAddress;
                        continue;
                    }
                    if (inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ipv4 = inetAddress;
                        continue;
                    }
                    return inetAddress.getHostAddress();
                }
            }
            if (ipv6 != null) {
                return ipv6.getHostAddress();
            }
            if (ipv4 != null) {
                return ipv4.getHostAddress();
            }
            return null;
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private void start_Scrcpy_service() {
        Intent intent = new Intent(this, Scrcpy.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stop_Scrcpy_service() {
        if (serviceBound) {
            scrcpy.StopService();
            unbindService(serviceConnection);
            serviceBound = false;
            first_time = true;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void loadNewRotation() {
        if (first_time){
            int[] rem_res = scrcpy.get_remote_device_resolution();
            remote_device_height = rem_res[1];
            remote_device_width = rem_res[0];
            first_time = false;
        }
        unbindService(serviceConnection);
        serviceBound = false;
        result_of_Rotation = true;
        landscape = !landscape;
        swapDimensions();
        if (landscape) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceBound) {
            scrcpy.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scrcpy == null) {
            serviceBound = false;
        }
        if (!first_time && !result_of_Rotation) {
            final View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            if (serviceBound) {
                linearLayout = findViewById(R.id.container1);
                scrcpy.resume();
            }
        }
        result_of_Rotation = false;
    }

    @Override
    public void onBackPressed() {
        if (timestamp == 0) {
            timestamp = SystemClock.uptimeMillis();
            Toast.makeText(context, UUtils.getString(R.string.press_again), Toast.LENGTH_SHORT).show();
        } else {
            long now = SystemClock.uptimeMillis();
            if (now < timestamp + 1000) {
                timestamp = 0;
                stop_Scrcpy_service();
                finish();
            }
            timestamp = 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (sensorEvent.values[0] == 0) {
                if (serviceBound) {
                    scrcpy.sendKeyevent(28);
                }
            } else {
                if (serviceBound) {
                    scrcpy.sendKeyevent(29);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
