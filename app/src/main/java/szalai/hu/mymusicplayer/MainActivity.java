package szalai.hu.mymusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button start, stop;
    private ListView listView;
    private String currentPath;
    private boolean bounded;
    private MusicService musicService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            init();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            }
        }
    }

    private void init() {

        Intent intent = new Intent(MainActivity.this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(new Intent(this, MusicService.class));
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(musicService.isPlaying()) {
                    musicService.stopMusic();
                }
            }
        });
        File root = Environment.getExternalStorageDirectory();
        currentPath = root.getPath();
        listView = (ListView) findViewById(R.id.listView);
        List values = listFiles(root);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filename = (String) parent.getAdapter().getItem(position);
                filename = currentPath + File.separator + filename;
                Toast.makeText(MainActivity.this, filename, Toast.LENGTH_LONG).show();
                File file = new File(filename);
                if (file.isDirectory()) {
                    List<String> values = listFiles(file);
                    currentPath = filename;
                    listView.setAdapter(new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, values));
                }
                if (file.isFile()) {
                    try {
                        ArrayList<String> values = (ArrayList<String>) listFiles(new File(currentPath));
                        for (int i = 0; i < values.size(); i++) {
                            values.set(i, currentPath + File.separator + values.get(i));
                        }
                        Intent intent = new Intent();
                        intent.putExtra("position", position);
                        Log.i("info", values.size() + " ");
                        intent.putExtra("list", values);
                        if(!musicService.isPlaying()) {
                            musicService.playMusic(intent);
                        }
                    } catch (Exception e) {

                    }
                }

            }
        });
    }

    private List<String> listFiles(File dir) {
        List values = new ArrayList<String>();
        if (dir.canRead()) {
            String[] list = dir.list();
            if (list != null) {
                for (String file : list) {
                    if (!file.startsWith(".")) {
                        values.add(file);
                    }
                }
            }
        }
        Collections.sort(values);
        return values;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder localBinder = (MusicService.LocalBinder) service;
            musicService = localBinder.getLocalService();
            bounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bounded = false;
        }
    };

}
