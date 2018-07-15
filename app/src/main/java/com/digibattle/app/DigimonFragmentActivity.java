package com.digibattle.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.digibattle.app.fragment.Digimon20Original;
import com.digibattle.app.fragment.Digimon20Pendulum;

public class DigimonFragmentActivity extends AppCompatActivity {

    private static final String TAG = "DigimonFragmentActivity";
    private static final int MY_PERMISSIONS_REQUEST_STORAGE = 0;
    private static final int MY_PERMISSIONS_REQUEST_AUDIO = 1;

    private static final String VERSION_20TH_ORIGINAL = "Digimon 20th Original";
    private static final String VERSION_20TH_PENDULUM = "Digimon 20th Pendulum";
    private static final String[] SUPPORTED_VERSIONS =
            new String[]{VERSION_20TH_ORIGINAL, VERSION_20TH_PENDULUM};

    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.digimon_version_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SUPPORTED_VERSIONS);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DigiBattleSharedPrefs.getInstance(
                        DigimonFragmentActivity.this).setLastVersionPosition(position);
                String version = SUPPORTED_VERSIONS[position];
                Fragment fragment;
                switch (version) {
                    case VERSION_20TH_ORIGINAL:
                        fragment = new Digimon20Original();
                        break;
                    case VERSION_20TH_PENDULUM:
                        fragment = new Digimon20Pendulum();
                        break;
                    default:
                        Log.e(TAG, "WTF version");
                        fragment = new Digimon20Original();
                }
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.activity_fragment_layout, fragment);
                transaction.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int position = DigiBattleSharedPrefs.getInstance(
                DigimonFragmentActivity.this).getLastVersionPosition();
        spinner.setSelection(position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        setupSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(this, SettingsScreenActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DigiBattleConfig.update(this);
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Please grant all permissions to proceed",
                        Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_STORAGE);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant all permissions to proceed",
                        Toast.LENGTH_SHORT).show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_STORAGE:
            case MY_PERMISSIONS_REQUEST_AUDIO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Yeah granted " + permissions[0]);
                } else {
                    Log.i(TAG, "Failed to grant " + permissions[0]);
                    finish();
                }
        }
    }
}
