package com.example.allergydiary;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.github.mzule.fantasyslide.SideBar;
import com.github.mzule.fantasyslide.SimpleFantasyListener;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawer;

    //TODO Read about navController
    //TODO Add Navigation Bar
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        SideBar leftSideBar = findViewById(R.id.leftSideBar);
        leftSideBar.setFantasyListener(new SimpleFantasyListener() {
            @Override
            public boolean onHover(@Nullable View view, int index) {
                return false;
            }

            @Override
            public boolean onSelect(View view, int index) {
                switch (view.getId()) {
                    case R.id.toDiary:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new DiaryActivity()).commit();
                        break;
                    case R.id.toForecast:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new ForecastFragment()).commit();
                        break;
                    case R.id.toCharts:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new ChartsFragment()).commit();
                        break;
                    case R.id.toSettings:
                        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                                new SettingsFragment()).commit();
                        break;
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }

            @Override
            public void onCancel() {

            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new DiaryActivity()).commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen((GravityCompat.START)))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }
}
