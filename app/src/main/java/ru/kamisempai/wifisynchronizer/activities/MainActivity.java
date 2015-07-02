package ru.kamisempai.wifisynchronizer.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import ru.kamisempai.wifisynchronizer.services.SynchronizeService;

/**
 * Created by Shurygin Denis on 25.06.2015.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startService(new Intent(this, SynchronizeService.class));
    }
}
