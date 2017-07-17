package szalai.hu.mymusicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
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
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Handler;


public class MainActivity extends AppCompatActivity {

    private Button start, stop, up, prew, next;
    private ListView listView;
    private String currentPath;
    private boolean bounded;
    private MusicService musicService;
    private SeekBar seekBar;
    private File root;
    private Runnable runnable;


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
        root = Environment.getExternalStorageDirectory();
        Log.i("info", "root " + root.getPath());
        currentPath = root.getPath();
        listView = (ListView) findViewById(R.id.listView);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);
        up = (Button) findViewById(R.id.up);
        prew = (Button) findViewById(R.id.prew);
        next = (Button) findViewById(R.id.next);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        List<String> values = listFiles(root);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String filename = (String) parent.getAdapter().getItem(position);
                filename = currentPath + File.separator + filename;
                int lastSlashIndex = filename.lastIndexOf('/');
                String title = filename.substring(lastSlashIndex + 1);
                Toast.makeText(MainActivity.this, title, Toast.LENGTH_LONG).show();
                File file = new File(filename);
                if (file.isDirectory()) {
                    List<String> values = listFiles(file);
                    currentPath = filename;
                    Log.i("info", currentPath);
                    listView.setAdapter(new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, values));
                }
                if (file.isFile()) {
                    try {
                        ArrayList<String> values = (ArrayList<String>) listFiles(new File(currentPath));
                        for (int i = 0; i < values.size(); i++) {
                            values.set(i, currentPath + File.separator + values.get(i));
                        }
                        musicService.playMusic(new Intent().putExtra("position", position).putExtra("list", values));
                        initSeekBar();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicService.pauseMusic();

            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //unbindService(serviceConnection);
                musicService.stopMusic();
                //onDestroy();
            }
        });
        up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goParentDir();
            }
        });
        prew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.getMediaPlayer() != null)
                    musicService.changeDirection(-1);
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (musicService.getMediaPlayer() != null) {
                    musicService.changeDirection(1);
                }
            }
        });
    }

    private void playCycle() {
        try {
            if (musicService.getMediaPlayer() != null) {
                Handler handler = new Handler();
                seekBar.setProgress(musicService.getMediaPlayer().getCurrentPosition());
                if (musicService.getMediaPlayer().isPlaying()) {
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            playCycle();
                        }
                    };
                    handler.postDelayed(runnable, 100);
                }
            }
        } catch (IllegalStateException exp) {
            exp.printStackTrace();
        }
    }

    private List<String> listFiles(File dir) {
        //TODO select just playable files
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

    private void goParentDir() {
        if (!currentPath.equals(root.getPath())) {
            File file = new File(currentPath);
            File parent = file.getParentFile();
            currentPath = parent.getPath();
            Log.i("info", currentPath);
            List<String> values = listFiles(parent);
            listView.setAdapter(new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, values));
        }
    }

    @Override
    public void onBackPressed() {
        goParentDir();
    }

    private void initSeekBar() {
        musicService.getMediaPlayer().setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                seekBar.setMax(musicService.getMediaPlayer().getDuration());
                playCycle();
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    musicService.getMediaPlayer().seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

}
