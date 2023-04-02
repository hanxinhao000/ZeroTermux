package com.termux.zerocore.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.termux.app.TermuxActivity;

public class LocalReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (intent == null ) {
            return;
        }
        String broadcastString = intent.getStringExtra("broadcastString");
        if (broadcastString == null || broadcastString.isEmpty()) {
            return;
        }
        TermuxActivity.mTerminalView.sendTextToTerminal(broadcastString + "\n");
    }
}
