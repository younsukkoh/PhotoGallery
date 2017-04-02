package com.example.younsuk.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Younsuk on 10/8/2015.
 */
public class VisibleFragment extends Fragment {
    private static final String TAG = "VisibleFragment";
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
    //----------------------------------------------------------------------------------------------
    @Override
    public void onStart(){
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mBroadcastReceiver, filter, PollService.PERMISSION_PRIVATE, null);
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }
    //----------------------------------------------------------------------------------------------
}
