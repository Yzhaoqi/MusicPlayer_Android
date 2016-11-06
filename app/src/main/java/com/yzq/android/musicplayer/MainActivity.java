package com.yzq.android.musicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private Button play, stop, quit;
    private SeekBar seekBar;
    private MusicService musicService;
    private TextView current, duration, filepath, status, load_file;
    private ImageView logo;
    private FrameLayout frame;
    private MusicHandler musicHandler;
    private MusicThread musicThread;
    private String address;
    private Thread thread;
    private float degree = 0;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");

    private ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            musicService = ((MusicService.MyBinder)(iBinder)).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
        }
    };

    class MusicHandler extends Handler {
        MusicHandler(){}
        @Override
        public void handleMessage(Message msg) {
            if (musicService != null && musicService.isValid()) {
                seekBar.setMax(musicService.getDuration());
                seekBar.setProgress(musicService.getCurrentPosition());
                Date date = new Date(musicService.getCurrentPosition());
                current.setText(time.format(date));
                degree = (float) ((degree+0.2)%360);
                frame.setRotation(degree);
            }
        }
    }

    class MusicThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    musicHandler.sendMessage(new Message());
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showPhonePermissions() {
        int permissionsCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionsCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Needed")
                        .setMessage("Rationale")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            }
                        });
                builder.create().show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, sc, BIND_AUTO_CREATE);

        play = (Button)findViewById(R.id.play);
        stop = (Button)findViewById(R.id.stop);
        quit = (Button)findViewById(R.id.quit);
        seekBar = (SeekBar)findViewById(R.id.seek);
        current = (TextView)findViewById(R.id.current);
        duration = (TextView)findViewById(R.id.duration);
        filepath = (TextView)findViewById(R.id.source);
        status = (TextView)findViewById(R.id.status);
        load_file = (TextView)findViewById(R.id.load_file);
        frame = (FrameLayout)findViewById(R.id.frame);
        logo = (ImageView)findViewById(R.id.logo);

        musicHandler = new MusicHandler();
        musicThread = new MusicThread();
        thread = new Thread(musicThread);
        thread.start();
        showPhonePermissions();

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (play.getText().equals("PLAY")) {
                    musicService.play();
                    play.setText("PAUSE");
                    status.setText("PLAYING");
                } else {
                    musicService.pause();
                    play.setText("PLAY");
                    status.setText("PAUSE");
                }
            }
        });

        stop.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicService.stop();
                play.setText("PLAY");
                status.setText("STOP");
            }
        }));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b)
                    musicService.seek(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        quit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                musicHandler.removeCallbacks(musicThread);
                unbindService(sc);
                try {
                    MainActivity.this.finish();
                    System.exit(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicHandler.removeCallbacks(musicThread);
        unbindService(sc);
        try {
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String mineType;
            Uri uri = data.getData();
            address = uri.getPath();
            mineType = URLConnection.guessContentTypeFromName(address);
            filepath.setText(address);
            play.setText("PLAY");
            status.setText("IDLE");
            if (mineType != null && mineType.startsWith("audio")) {
                setCover(uri);
                musicService.load(address);
                Date date = new Date(musicService.getDuration());
                duration.setText(time.format(date));
                load_file.setText("Loaded media file:");
                play.setEnabled(true);
                stop.setEnabled(true);
            } else {
                load_file.setText("Error file open:");
                play.setEnabled(false);
                stop.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_load:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("audio/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCover(Uri uri) {
        MediaMetadataRetriever myRetriever = new MediaMetadataRetriever();
        myRetriever.setDataSource(this, uri);
        byte[] artwork;
        artwork = myRetriever.getEmbeddedPicture();
        if (artwork != null) {
            Bitmap bMap = BitmapFactory.decodeByteArray(artwork, 0, artwork.length);
            logo.setImageBitmap(getRoundedShape(bMap));
        } else {
            logo.setImageResource(R.drawable.cover);
        }
        frame.setRotation(0);
        degree = 0;
    }

    private Bitmap getRoundedShape(Bitmap bMap) {
        int targetWidth = 1000;
        int targetHeight = 1000;
        Bitmap targetBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(targetBitmap);
        Path path = new Path();
        path.addCircle(((float)targetWidth-1)/2, ((float)targetHeight-1)/2, (Math.min(((float)targetWidth), ((float)targetHeight))/2), Path.Direction.CCW);

        canvas.clipPath(path);
        Bitmap sourceBitmap = bMap;
        canvas.drawBitmap(sourceBitmap, new Rect(0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight()), new Rect(0, 0, targetWidth, targetHeight), null);
        return targetBitmap;
    }
}