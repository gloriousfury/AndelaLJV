package com.alc.ljv.ui.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alc.ljv.R;
import com.alc.ljv.adapter.ProfileAdapter;
import com.alc.ljv.adapter.ProfileAdapterGrid;
import com.alc.ljv.constants.IntentActionConstants;
import com.alc.ljv.constants.ServiceActionConstants;
import com.alc.ljv.model.DataModel;
import com.alc.ljv.model.ProfileModel;
import com.alc.ljv.service.AppMainService;
import com.alc.ljv.service.AppMainServiceEvent;
import com.alc.ljv.utility.ChromeTabs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static android.drm.DrmInfoRequest.ACCOUNT_ID;
import static com.alc.ljv.R.id.coordinatorLayout;
import static com.alc.ljv.R.id.txt_javadevcount;
import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "MainActivity";

    private static final int UI_STATE_ERROR = 0;
    private static final int UI_STATE_SUCCES = 1;
    ImageView rightArrow, leftArrow;
    TextView pageNo, devCount;
    ProgressBar progressBar;
    String PAGE_NO = "page_no";
    String SORT_TYPE = "sort_type";
    int currentPageNo = 1;
    int totalPageNo = 1;
    String sortType = "followers";
    CoordinatorLayout coordinatorLayout;
    RelativeLayout relativeLayout;
    boolean isGridView = false;

    List<ProfileModel> FeedList = new ArrayList<ProfileModel>();
    RecyclerView recyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Andela LJV");
        setSupportActionBar(toolbar);

        //initialization of views
        rightArrow = (ImageView) findViewById(R.id.right_arrow);
        leftArrow = (ImageView) findViewById(R.id.left_arrow);
        pageNo = (TextView) findViewById(R.id.pageno_view);
        devCount = (TextView) findViewById(R.id.txt_javadevcount);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        relativeLayout = (RelativeLayout) findViewById(R.id.content_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        //initialize recycler by feeding adapter empty data
        ProfileAdapter adapter = new ProfileAdapter(this, FeedList);
        recyclerView.setAdapter(adapter);

        recyclerView.setAdapter(adapter);

        rightArrow.setOnClickListener(this);
        leftArrow.setOnClickListener(this);
        leftArrow.setVisibility(View.INVISIBLE);

        //get list of java devs in lagos with the github api
        GetLagosJavaDevs(currentPageNo, sortType);

    }

    private void GetLagosJavaDevs(int CurrentPageNo, String sortType) {


        //start background service for the network thread so as not to carry it out on the UI mainthread
        //ServiceActionConstants are used for EventBus to identify which request it is dealing with
        Intent i = new Intent(this, AppMainService.class);
        i.putExtra(ServiceActionConstants.SERVICE_ACTION, ServiceActionConstants.SERVICE_ACTION_GET_JAVA_DEV);
        i.putExtra(PAGE_NO, CurrentPageNo);
        i.putExtra(SORT_TYPE, sortType);
        startService(i);
        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }


    //eventbus subscriber for this event.
    @Subscribe
    public void onEventMainThread(AppMainServiceEvent event) {
        Log.d(TAG, "onEventMainThread() called with: " + "event = [" + event + "]");
        Intent i = event.getMainIntent();
        String getDevCount;


        if (event.getEventType() == AppMainServiceEvent.GITHUB_USERS_RESPONSE) {
            if (i != null) {
                //Get data
                DataModel response = Parcels.unwrap(i.getParcelableExtra(AppMainServiceEvent.RESPONSE_DATA));
                FeedList = response.getItems();

                if (response.getIncompleteResults().toString().contentEquals("false")) {

                    // Pagination: display 20 users on each page, so totalpageno will be equal to (totalno divided by 20) +1(for
                    // leftovers)
                    totalPageNo = (response.getTotalCount() / 20) + 1;
                    devCount.setText(response.getTotalCount() + "");
                    FeedList = response.getItems();


                    //Update view with retrieved Items

                    ProfileAdapter adapter = new ProfileAdapter(this, FeedList);
                    recyclerView.setAdapter(adapter);
                    updateUi(UI_STATE_SUCCES);


                } else {
                    updateUi(UI_STATE_ERROR);


                }

            } else {
                updateUi(UI_STATE_ERROR);

            }

        }


        if (event.getEventType() == AppMainServiceEvent.GITHUB_SINGLE_USER_RESPONSE) {
            if (i != null) {

                DataModel response = Parcels.unwrap(i.getParcelableExtra(AppMainServiceEvent.RESPONSE_DATA));
                FeedList = response.getItems();


                if (response.getIncompleteResults().toString().contentEquals("false")) {
                    FeedList = response.getItems();

                    ProfileAdapter adapter = new ProfileAdapter(this, FeedList);
                    recyclerView.setAdapter(adapter);
                    updateUi(UI_STATE_SUCCES);

                } else {
                    updateUi(UI_STATE_ERROR);
                }

            } else {
                updateUi(UI_STATE_ERROR);

            }

        }

    }


    private void updateUi(int state) {
        if (state == UI_STATE_SUCCES) {

            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.INVISIBLE);

        } else if (state == UI_STATE_ERROR) {

            progressBar.setVisibility(View.INVISIBLE);

            Snackbar snackbar = Snackbar.make(coordinatorLayout, "No Internet Connection", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("RETRY", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    GetLagosJavaDevs(currentPageNo, sortType);
                }
            });
            snackbar.show();

        }


    }


    @Override
    public void onClick(View view) {

        switch (view.getId()) {

            //
            case R.id.right_arrow:
                //pagination-load next page
                if (currentPageNo < totalPageNo) {

                    currentPageNo++;
                    leftArrow.setVisibility(View.VISIBLE);
                    pageNo.setText("" + currentPageNo);
                    GetLagosJavaDevs(currentPageNo, sortType);
                } else if (currentPageNo == (totalPageNo - 1)) {
                    currentPageNo++;
                    rightArrow.setVisibility(View.INVISIBLE);
                    GetLagosJavaDevs(currentPageNo, sortType);


                }

                break;

            case R.id.left_arrow:
                //pagination-load previous page

                if (currentPageNo > 1) {
                    currentPageNo--;
                    rightArrow.setVisibility(View.VISIBLE);

                    pageNo.setText("" + currentPageNo);
                    GetLagosJavaDevs(currentPageNo, sortType);
                } else if (currentPageNo == 2) {
                    currentPageNo--;
                    pageNo.setText("" + currentPageNo);
                    leftArrow.setVisibility(View.INVISIBLE);
                    GetLagosJavaDevs(currentPageNo, sortType);


                }

                break;


        }

    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            Intent aboutActivity = new Intent(this, AboutActivity.class);
            startActivity(aboutActivity);

        } else if (id == R.id.action_githubAudio) {
            ChromeTabs chromeTabs = new ChromeTabs();
            chromeTabs.gotoGithubProfile(this,"https://github.audio");
        } else if (id == R.id.sortbyFollowers) {
            sortType = "followers";
            GetLagosJavaDevs(currentPageNo, sortType);

        } else if (id == R.id.sortbyRepositories) {

            sortType = "repositories";
            GetLagosJavaDevs(currentPageNo, sortType);

        }


        return super.onOptionsItemSelected(item);
    }


}