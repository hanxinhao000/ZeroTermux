package com.termux.zerocore.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.zerocore.fragment.BackupFragment;
import com.termux.zerocore.fragment.RestoreFragment;
import com.termux.zerocore.fragment.SettingFragment;


public class BackNewActivity extends AppCompatActivity implements View.OnClickListener {


    private LinearLayout mBackup;
    private LinearLayout mRestore;
    private LinearLayout mSetting;
    private FragmentManager mSupportFragmentManager;
    private FragmentTransaction mFragmentTransaction;
    private BackupFragment mBackupFragment;
    private RestoreFragment mRestoreFragment;
    private SettingFragment mSettingFragment;


    private TextView mBackupText;
    private TextView mRestoreText;
    private TextView mSettingText;

    public int mSwitch = 0;

    public static boolean mIsRun = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_new);

        mBackup = findViewById(R.id.backup);
        mRestore = findViewById(R.id.restore);
        mSettingText = findViewById(R.id.setting_text);

        mBackupText = findViewById(R.id.backup_text);
        mRestoreText = findViewById(R.id.restore_text);
        mSetting = findViewById(R.id.setting);

        mBackup.setOnClickListener(this);
        mRestore.setOnClickListener(this);
        mSetting.setOnClickListener(this);


        mSupportFragmentManager = getSupportFragmentManager();

        mFragmentTransaction = mSupportFragmentManager.beginTransaction();
        mBackupFragment = new BackupFragment();
        mRestoreFragment = new RestoreFragment();
        mSettingFragment = new SettingFragment();
        mFragmentTransaction.add(R.id.fragment, mRestoreFragment);
        mFragmentTransaction.add(R.id.fragment, mSettingFragment);
        mFragmentTransaction.add(R.id.fragment, mBackupFragment);
        mFragmentTransaction.show(mBackupFragment).commit();
        mBackupText.setTextColor(Color.parseColor("#FF6EC7"));



    }



    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return isCosumenBackKey();
        }
        return false;
    }

    private boolean isCosumenBackKey() {
        if (mIsRun) {
            Toast.makeText(this, UUtils.getString(R.string.有任务正在进行), Toast.LENGTH_SHORT).show();
            return true;
        } else {
            // Toast.makeText(this, "返回键被阻拦了，您可以按home键退出再进来", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }


    }

    @Override
    public void onClick(View v) {


        if (mIsRun) {
            Toast.makeText(this, UUtils.getString(R.string.有任务进行中), Toast.LENGTH_SHORT).show();
            return;
        }

        int id = v.getId();

        if (id == R.id.backup) {
            mBackupText.setTextColor(Color.parseColor("#ffffff"));
            mRestoreText.setTextColor(Color.parseColor("#ffffff"));
            mSettingText.setTextColor(Color.parseColor("#ffffff"));

            mBackupText.setTextColor(Color.parseColor("#FF6EC7"));
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            supportFragmentManager.beginTransaction().replace(R.id.fragment, mBackupFragment).commit();

        } else if (id == R.id.restore) {
            mBackupText.setTextColor(Color.parseColor("#ffffff"));
            mRestoreText.setTextColor(Color.parseColor("#ffffff"));
            mSettingText.setTextColor(Color.parseColor("#ffffff"));

            mRestoreText.setTextColor(Color.parseColor("#FF6EC7"));
            FragmentManager supportFragmentManager1 = getSupportFragmentManager();
            supportFragmentManager1.beginTransaction().replace(R.id.fragment, mRestoreFragment).commit();

        } else if (id == R.id.setting) {
            mBackupText.setTextColor(Color.parseColor("#ffffff"));
            mRestoreText.setTextColor(Color.parseColor("#ffffff"));
            mSettingText.setTextColor(Color.parseColor("#ffffff"));

            mSettingText.setTextColor(Color.parseColor("#FF6EC7"));

            FragmentManager supportFragmentManager2 = getSupportFragmentManager();
            supportFragmentManager2.beginTransaction().replace(R.id.fragment, mSettingFragment).commit();
        }
    }

    @Override
    public void finish() {
        super.finish();
        mIsRun = false;
    }



}
