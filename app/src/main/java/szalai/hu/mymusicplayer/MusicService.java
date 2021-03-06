package szalai.hu.mymusicplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mihaly Szalai on 2017. 07. 13..
 */

public class MusicService extends Service {

    private final Binder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private int position;
    private boolean paused = false;
    private ArrayList<String> list;
    //TODO remove mediaplayer getter and setter, and isplaying


    public class LocalBinder extends Binder {
        MusicService getLocalService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("info", "binded");
        return binder;
    }


    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {//activity ondestroyba
        return super.onUnbind(intent);
    }

    public void playMusic(Intent intent) {
        position = intent.getIntExtra("position",0);
        list = (ArrayList<String>) intent.getSerializableExtra("list");
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();

        }
        while (!extensionTest(list.get(position))) {
            position++;
        }
        try {
            mediaPlayer = MediaPlayer.create(this, Uri.parse(list.get(position)));
            mediaPlayer.start();
            notification();
            paused = false;
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    changeDirection(1);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeDirection(int direction) {
        Log.i("info","change direction");
        if (direction == -1) {
            try {
                position--;
                while (!extensionTest(list.get(position))) {
                    position--;
                }
            } catch (IndexOutOfBoundsException e) {
                position = list.size();
            }
            changeTrack();

        }
        if (direction == 1) {
            try {
                position++;
                while (!extensionTest(list.get(position))) {
                    position++;
                }
            } catch (IndexOutOfBoundsException e) {
                position = 0;
            }
            changeTrack();

        }

    }

    private void changeTrack() {
        Log.i("info","change track");
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(this, Uri.parse(list.get(position)));
            mediaPlayer.prepare();
            mediaPlayer.start();
            notification();
            paused = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void pauseMusic() {
        if (mediaPlayer != null) {
            if (!paused) {
                mediaPlayer.pause();
                paused = true;
            } else {
                mediaPlayer.start();
                paused = false;
            }
        }
    }

    public void stopMusic() {
        mediaPlayer.stop();
        mediaPlayer.release();
        stopForeground(true);
    }


    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    private boolean extensionTest(String fileName) {
        if (fileName.endsWith("mp3") || fileName.endsWith("3gp") || fileName.endsWith("wav") || fileName.endsWith("cda") || fileName.endsWith("ogg") || fileName.endsWith("wma")) {
            return true;
        } else {
            return false;
        }
    }

    private void notification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(MusicService.this, 0, new Intent(MusicService.this, MainActivity.class), 0);
        int lastSlashIndex = list.get(position).lastIndexOf('/');
        String title = list.get(position).substring(lastSlashIndex + 1);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
        startForeground(1, notification);
    }
}
