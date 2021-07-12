package com.termux.zerocore.utermux_windows.qemu.dialog;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.blockchain.ub.util.custom.dialog.BaseDialogCentre;
import com.termux.R;

/**
 * @author ZEL
 * @create By ZEL on 2020/10/20 11:21
 **/
public class SwitchQemuDialog extends BaseDialogCentre {

    private LinearLayout x86_64;
    private LinearLayout i386;
    private LinearLayout mac;
    public SwitchQemuDialog(@NonNull Context context) {
        super(context);
    }

    public SwitchQemuDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
   public void initViewDialog(View mView) {
        x86_64 = mView.findViewById(R.id.x86_64);
        i386 = mView.findViewById(R.id.i386);
        mac = mView.findViewById(R.id.mac);

        x86_64.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSwitchJGListener!= null){

                    mSwitchJGListener.SwitchSystem("amd");
                }
            }
        });
        i386.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitchJGListener.SwitchSystem("i386");
            }
        });
        mac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitchJGListener.SwitchSystem("mac");
            }
        });
    }

    @Override
    public int getContentView() {
        return R.layout.dialog_qemu_switch;
    }

    private SwitchJGListener mSwitchJGListener;
    public void setSwitchJGListener(SwitchJGListener mSwitchJGListener){
        this.mSwitchJGListener = mSwitchJGListener;
    }

    public interface SwitchJGListener{


        void SwitchSystem(String string);

    }
}
