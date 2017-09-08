package com.cona.traininginstaller;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.BoolRes;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public class MainActivity extends Activity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final String configFile  = "training.txt";
    private static final String transFile = "data_V15TS.db";
    private static final String masterFile = "data_V15MD.db";
    private static final String sourceDir = "Download/training";
    private static final String targetDir = "MovilizerDBImport";
    private static final String dsdDir = "dsd";
    private static final String fsvDir = "fsv";

    private static String appId, apkFile;

    Button installDSDButton, installFSVButton, uninstallButton;
    TextView textView1, textView2, textView3, textView4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        installDSDButton = (Button) findViewById(R.id.button_install_dsd);
        installFSVButton = (Button) findViewById(R.id.button_install_fsv);
        uninstallButton = (Button) findViewById(R.id.button_uninstall);
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);

        installDSDButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_config();
                copyFiles(dsdDir);

                File file = new File(apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(intent);

            }
        });

        installFSVButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_config();
                copyFiles(fsvDir);

                File file = new File(apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(intent);

            }
        });

        uninstallButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_config();
                Intent intent = new Intent(Intent.ACTION_DELETE);
                intent.setData(Uri.parse("package:" + appId));
                startActivity(intent);
            }
        });

        if (verifyStoragePermissions(this)) {
            read_config();
            if (appId.length() > 0 && apkFile.length() > 0) {
                installDSDButton.setEnabled(true);
                installFSVButton.setEnabled(true);
                uninstallButton.setEnabled(true);
            }
        } else {
            textView4.setText("please grant requested permissions and restart app");
        }
    }

    private void read_config() {
        String filepath = Environment.getExternalStorageDirectory().getPath() + "/" + sourceDir + "/" + configFile;
        File file = new File(filepath);
        if (file.isFile()) {
            appId = "";
            String line;
            try {
                InputStream fis = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
                BufferedReader br = new BufferedReader(isr);
                while ((line = br.readLine()) != null) {
                    appId = appId + line;
                }
            } catch (Exception e) {
                Log.e("READ_CNF", e.getMessage());
            }
            textView2.setText(appId);
        }
        String basepath = Environment.getExternalStorageDirectory().getPath() + "/" + sourceDir;
        textView1.setText(basepath);
        File dir = new File(basepath);
        if (dir != null) {
            File[] files = dir.listFiles();
            for (int i=0; i<files.length; i++) {
                String filename = files[i].getName().toLowerCase();
                if (filename.endsWith(".apk")) {
                    apkFile = Environment.getExternalStorageDirectory().getPath() + "/" + sourceDir + "/" + filename;
                    break;
                }
            }
        }
        textView3.setText(apkFile);
    }

    private void copyFiles(String sourcePathExt) {
        String sourceFilePath, targetFilePath;
        File targetDirF = new File(Environment.getExternalStorageDirectory().getPath() + "/" + targetDir);
        if (!targetDirF.exists()) {
            try{
                targetDirF.mkdir();
            }
            catch(SecurityException e){
                Log.e("COPY_FILES", e.getMessage());
                return;
            }
        }
        sourceFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + sourceDir + "/" + sourcePathExt + "/" + transFile;
        targetFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + targetDir + "/" + transFile;
        File sourceFile1 = new File(sourceFilePath);
        File targetFile1 = new File(targetFilePath);
        sourceFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + sourceDir + "/" + sourcePathExt + "/" + masterFile;
        targetFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + targetDir + "/" + masterFile;
        File sourceFile2 = new File(sourceFilePath);
        File targetFile2 = new File(targetFilePath);
        try {
            copyFile(sourceFile1, targetFile1);
            copyFile(sourceFile2, targetFile2);
        } catch (Exception e) {
            Log.e("COPY_FILES", e.getMessage());
        }
    }

    public static boolean verifyStoragePermissions(Activity activity) {
        boolean ret = true;
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ret = false;
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        return ret;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

}
