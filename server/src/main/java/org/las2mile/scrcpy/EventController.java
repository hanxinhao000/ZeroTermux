package org.las2mile.scrcpy;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.las2mile.scrcpy.wrappers.InputManager;

import java.io.IOException;


public class EventController {

    private final Device device;
    private final DroidConnection connection;
    private final MotionEvent.PointerProperties[] pointerProperties = {new MotionEvent.PointerProperties()};
    private final MotionEvent.PointerCoords[] pointerCoords = {new MotionEvent.PointerCoords()};
    private long lastMouseDown;
    private float then;
    private boolean hit = false;
    private boolean proximity = false;

    public EventController(Device device, DroidConnection connection) {
        this.device = device;
        this.connection = connection;
        initPointer();
    }

    private void initPointer() {
        MotionEvent.PointerProperties props = pointerProperties[0];
        props.id = 0;
        props.toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.orientation = 0;
        coords.pressure = 1;
        coords.size = 1;
    }

    private void setPointerCoords(Point point) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.x = point.x;
        coords.y = point.y;
    }

    private void setScroll(int hScroll, int vScroll) {
        MotionEvent.PointerCoords coords = pointerCoords[0];
        coords.setAxisValue(MotionEvent.AXIS_HSCROLL, hScroll);
        coords.setAxisValue(MotionEvent.AXIS_VSCROLL, vScroll);
    }

    public void control() throws IOException {
        // on start, turn screen on
        turnScreenOn();

        while (true) {
            //           handleEvent();
            int[] buffer = connection.NewreceiveControlEvent();
            if (buffer != null) {
                long now = SystemClock.uptimeMillis();
                if (buffer[2] == 0 && buffer[3] == 0) {
                    if (buffer[0] == 28) {
                        proximity = true;           // Proximity event
                    } else if (buffer[0] == 29) {
                        proximity = false;
                    }else {
                        injectKeycode(buffer[0]);
                    }
                } else {
                    int action = buffer[0];
                    if (action == MotionEvent.ACTION_UP && (!device.isScreenOn() || proximity)) {
                        if (hit) {
                            if (now - then < 250) {
                                then = 0;
                                hit = false;
                                injectKeycode(KeyEvent.KEYCODE_POWER);
                            } else {
                                then = now;
                            }
                        } else {
                            hit = true;
                            then = now;
                        }

                    } else {
                        if (action == MotionEvent.ACTION_DOWN) {
                            lastMouseDown = now;
                        }
                        int button = buffer[1];
                        int X = buffer[2];
                        int Y = buffer[3];
                        Point point = new Point(X, Y);
                        Point newpoint = device.NewgetPhysicalPoint(point);
                        setPointerCoords(newpoint);
                        MotionEvent event = MotionEvent.obtain(lastMouseDown, now, action, 1, pointerProperties, pointerCoords, 0, button, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
                        injectEvent(event);
                    }
                }


            }
        }
    }


    private boolean injectKeyEvent(int action, int keyCode, int repeat, int metaState) {
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        return injectEvent(event);
    }

    private boolean injectKeycode(int keyCode) {
        return injectKeyEvent(KeyEvent.ACTION_DOWN, keyCode, 0, 0)
                && injectKeyEvent(KeyEvent.ACTION_UP, keyCode, 0, 0);
    }

    private boolean injectEvent(InputEvent event) {
        return device.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }

    private boolean turnScreenOn() {
        return device.isScreenOn() || injectKeycode(KeyEvent.KEYCODE_POWER);
    }

}
