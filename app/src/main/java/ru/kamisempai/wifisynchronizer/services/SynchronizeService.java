package ru.kamisempai.wifisynchronizer.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.io.FileFilter;

import ru.kamisempai.wifisynchronizer.R;
import ru.kamisempai.wifisynchronizer.tasks.CopyFilesToSharedFolderTask;

/**
 * Created by KamiSempai on 2015-06-25.
 */
public class SynchronizeService extends Service {
    private static final int FOREGROUND_NOTIFY_ID = 1;
    private static final int MESSAGE_NOTIFY_ID = 2;
    private static final String SHARED_FOLDER_URL = "file://192.168.0.5/shared/";

    private static final String USER = null;
    private static final String PASSWORD = null;

    private NotificationManager mNotifyManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        final File folderToCopy = getFolderToCopy();
        if (folderToCopy == null) {
            showNotification(getString(R.string.synchronize_service_err_no_sd_card), Color.RED);
        }
        else {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.synchronize_service_message))
                    .setContentIntent(getDummyContentIntent())
                    .setColor(Color.BLUE)
                    .setProgress(1000, 0, true);
            startForeground(FOREGROUND_NOTIFY_ID, builder.build());

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            final WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SynchronizeWakelockTag");
            wakeLock.acquire();

            FileFilter fileFilter = new FileFilter() {
                private final File[] BLACK_LIST = new File[] {
                        new File(folderToCopy, "Android")
                };

                @Override
                public boolean accept(File file) {
                    if (file.getName().charAt(0) == '.')
                        return false;
                    for (File blackFile: BLACK_LIST)
                        if (blackFile.equals(file))
                            return false;
                    return false;
                }
            };

            CopyFilesToSharedFolderTask task = new CopyFilesToSharedFolderTask(folderToCopy, SHARED_FOLDER_URL, USER, PASSWORD, fileFilter) {
                @Override
                protected void onProgressUpdate(Double... values) {
                    builder.setProgress(100, values[0].intValue(), false)
                        .setContentText(String.format("%s %.3f", getString(R.string.synchronize_service_progress), values[0]) + "%");
                    mNotifyManager.notify(FOREGROUND_NOTIFY_ID, builder.build());
                }

                @Override
                protected void onPostExecute(String errorMessage) {
                    stopForeground(true);
                    if (errorMessage == null)
                        showNotification(getString(R.string.synchronize_service_success), Color.GREEN);
                    else
                        showNotification(errorMessage, Color.RED);
                    stopSelf();
                }

                @Override
                protected void onCancelled(String errorMessage) {
                    stopSelf();
                    wakeLock.release();
                }
            };
            task.execute();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showNotification(String message, int color) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setContentIntent(getDummyContentIntent())
                .setSound(Uri.parse(Settings.System.DEFAULT_NOTIFICATION_URI.toString()), AudioManager.STREAM_ALARM)
                .setVibrate(new long[]{100, 500, 100, 1000})
                .setColor(color);

        mNotifyManager.notify(MESSAGE_NOTIFY_ID, builder.build());
    }

    private static File getFolderToCopy() {
        if (sdCardEnabled())
            return Environment.getExternalStorageDirectory();
        return null;
    }

    private static boolean sdCardEnabled() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private PendingIntent getDummyContentIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/KamiSempai/WiFiFolderSynchronizer"));
        return PendingIntent.getActivity(this, 0, intent, 0);
    }
}
