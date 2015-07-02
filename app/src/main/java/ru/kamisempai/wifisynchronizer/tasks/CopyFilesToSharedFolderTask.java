package ru.kamisempai.wifisynchronizer.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by KamiSempai on 2015-06-25.
 */
public class CopyFilesToSharedFolderTask extends AsyncTask<Void, Double, String> {
    private static final String LOG_TAG = "WiFiSynchronizer";

    private final String mSharedFolderUrl;
    private final File mFolderToCopy;
    private final NtlmPasswordAuthentication mAuth;

    private final FileFilter mFileFilter;

    private double mMaxProgress;
    private double mProgress;

    public CopyFilesToSharedFolderTask(File folderToCopy, String sharedFolderUrl, String user, String password, FileFilter fileFilter) {
        super();
        mSharedFolderUrl = sharedFolderUrl;
        mFolderToCopy = folderToCopy;
        mAuth = (user != null && password != null)
                ? new NtlmPasswordAuthentication(user + ":" + password)
                : NtlmPasswordAuthentication.ANONYMOUS;
        mFileFilter = fileFilter;
    }

    @Override
    protected String doInBackground(Void... voids) {
        mMaxProgress = getFilesSize(mFolderToCopy);
        mProgress = 0;
        publishProgress(0d);

        try {
            SmbFile sharedFolder = new SmbFile(mSharedFolderUrl, mAuth);

            if (sharedFolder.exists() && sharedFolder.isDirectory()) {
                copyFiles(mFolderToCopy, sharedFolder);
            }
        } catch (MalformedURLException e) {
            return "Invalid URL.";
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return null;
    }

    private void copyFiles(File fileToCopy, SmbFile sharedFolder) throws IOException {
        if (!checkFilter(fileToCopy))
            return;

        if (fileToCopy.exists()) {
            if (fileToCopy.isDirectory()) {
                File[] filesList = fileToCopy.listFiles();
                SmbFile newSharedFolder = new SmbFile(sharedFolder, fileToCopy.getName() + "/");
                if (!newSharedFolder.exists()) {
                    newSharedFolder.mkdir();
                    Log.d(LOG_TAG, "Folder created:" + newSharedFolder.getPath());
                }
                else
                    Log.d(LOG_TAG, "Folder already exist:" + newSharedFolder.getPath());
                for (File file : filesList)
                    copyFiles(file, newSharedFolder);
            } else {
                SmbFile newSharedFile = new SmbFile(sharedFolder, fileToCopy.getName());
                if (!newSharedFile.exists()) {
                    copySingleFile(fileToCopy, newSharedFile);
                    Log.d(LOG_TAG, "File copied:" + newSharedFile.getPath());
                }
                else
                    Log.d(LOG_TAG, "File already exist:" + newSharedFile.getPath());
                mProgress += (double) fileToCopy.length();
                publishProgress(mProgress / mMaxProgress * 100d);
            }
        }
    }

    private void copySingleFile(File file, SmbFile sharedFile) throws IOException {
        IOException exception = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            outputStream = new SmbFileOutputStream(sharedFile);
            inputStream = new FileInputStream(file);

            byte[] bytesBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(bytesBuffer)) > 0) {
                outputStream.write(bytesBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            exception = e;
        } finally {
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (outputStream != null)
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        if (exception != null)
            throw exception;
    }

    private double getFilesSize(File file) {
        if (!checkFilter(file))
            return 0;

        if (file.isDirectory()) {
            int size = 0;
            File[] filesList = file.listFiles();
            for (File innerFile : filesList)
                size += getFilesSize(innerFile);
            return size;
        }

        return (double) file.length();
    }

    private boolean checkFilter(File file) {
        return mFileFilter == null || mFileFilter.accept(file);
    }
}
