package com.droidmage;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import clojure.lang.Symbol;
import clojure.lang.Var;
import clojure.lang.RT;

// import com.j256.ormlite.dao.Dao;
// import com.j256.ormlite.dao.DaoManager;
// import com.j256.ormlite.jdbc.JdbcConnectionSource;
// import com.j256.ormlite.support.ConnectionSource;
// import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import com.droidmage.R;

public class SplashActivity extends Activity {

    private static boolean firstLaunch = true;
    private static String TAG = "Splash";

    // private final String LOG_TAG = getClass().getSimpleName();
	// private ConnectionSource connectionSource;
	// private Dao<SimpleData, Integer> simpleDao;

	// {
	// 	if (connectionSource == null) {
	// 		try {
	// 			connectionSource =
    //                 new JdbcConnectionSource("jdbc:h2:/data/data/com.droidmage.debug/helloAndroidH2");
    //             simpleDao = DaoManager.createDao(connectionSource, SimpleData.class);
    //         } catch (SQLException e) {
    //             throw new RuntimeException("Problems initializing database objects", e);
    //         }
    //         try {
    //             TableUtils.createTable(connectionSource, SimpleData.class);
    //         } catch (SQLException e) {
    //             // ignored
    //         }
    //     }
    // }
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // doSampleDatabaseStuff("onCreate");
        
        if (firstLaunch) {
            firstLaunch = false;
            setupSplash();
            loadClojure();
        } else {
            proceed();
        }
    }

    // /**
    //  * Do our sample database stuff.
    //  */
    // private void doSampleDatabaseStuff(String action) {
    //     try {
	// 		// query for all of the data objects in the database
	// 		List<SimpleData> list = simpleDao.queryForAll();
	// 		// our string builder for building the content-view
	// 		StringBuilder sb = new StringBuilder();
	// 		sb.append("got ").append(list.size()).append(" entries in ").append(action).append('\n');

	// 		// if we already have items in the database
	// 		int simpleC = 0;
	// 		for (SimpleData simple : list) {
	// 			sb.append("------------------------------------------\n");
	// 			sb.append("[" + simpleC + "] = ").append(simple).append('\n');
	// 			simpleC++;
	// 		}
	// 		sb.append("------------------------------------------\n");
	// 		for (SimpleData simple : list) {
	// 			simpleDao.delete(simple);
	// 			sb.append("deleted id ").append(simple.id).append('\n');
	// 			Log.i(LOG_TAG, "deleting simple(" + simple.id + ")");
	// 			simpleC++;
	// 		}

	// 		int createNum;
	// 		do {
	// 			createNum = new Random().nextInt(3) + 1;
	// 		} while (createNum == list.size());
	// 		for (int i = 0; i < createNum; i++) {
	// 			// create a new simple object
	// 			long millis = System.currentTimeMillis();
	// 			SimpleData simple = new SimpleData(millis);
	// 			// store it in the database
	// 			simpleDao.create(simple);
	// 			Log.i(LOG_TAG, "created simple(" + millis + ")");
	// 			// output it
	// 			sb.append("------------------------------------------\n");
	// 			sb.append("created new entry #").append(i + 1).append('\n');
	// 			sb.append(simple).append('\n');
	// 			try {
	// 				Thread.sleep(5);
	// 			} catch (InterruptedException e) {
	// 				// ignore
	// 			}
	// 		}

	// 		Log.d(LOG_TAG, sb.toString());
	// 	} catch (SQLException e) {
	// 		Log.e(LOG_TAG, "Database exception", e);
	// 		return;
	// 	}
	// }
    
    public void setupSplash() {
        setContentView(R.layout.splashscreen);

        TextView appNameView = (TextView)findViewById(R.id.splash_app_name);
        appNameView.setText(R.string.app_name);

        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.splash_rotation);
        ImageView circleView = (ImageView)findViewById(R.id.splash_circles);
        circleView.startAnimation(rotation);
    }

    public void proceed() {
        startActivity(new Intent("com.droidmage.MAIN"));
        finish();
    }

    public void loadClojure() {
        new Thread(new Runnable(){
                @Override
                public void run() {
                    Symbol CLOJURE_MAIN = Symbol.intern("neko.init");
                    Var REQUIRE = RT.var("clojure.core", "require");
                    REQUIRE.invoke(CLOJURE_MAIN);

                    Var INIT = RT.var("neko.init", "init");
                    INIT.invoke(SplashActivity.this.getApplication());

                    try {
                        Class.forName("com.droidmage.MainActivity");
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG, "Failed loading MainActivity", e);
                    }

                    proceed();
                }
            }).start();
    }
}
