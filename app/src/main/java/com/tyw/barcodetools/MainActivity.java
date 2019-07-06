package com.tyw.barcodetools;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.functions.Consumer;

import android.Manifest;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.tyw.barcodetools.ui.main.MainFragment;
import com.tyw.barcodetools.utils.DebugLog;

public class MainActivity extends AppCompatActivity {

    MainFragment mFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        init();
        if (savedInstanceState == null) {
            mFragment =  MainFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container,mFragment)
                    .commitNow();
        }

    }

    void init()
    {
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission
                .requestEach(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            // 用户已经同意该权限
                            DebugLog.d(permission.name + " is granted.");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            DebugLog.d(permission.name + " is denied. More info should be provided.");
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            DebugLog.d(permission.name + " is denied.");
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_action,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mFragment.onScan();

                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
