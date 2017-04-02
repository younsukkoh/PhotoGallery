package com.example.younsuk.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * Created by Younsuk on 10/9/2015.
 */
public class PhotoPageActivity extends SingleFragmentActivity {

    private WebView mWebView;
    private PhotoPageFragment mPhotoPageFragment;
    //----------------------------------------------------------------------------------------------
    public static Intent newIntent(Context context, Uri photoPageUri){
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(photoPageUri);
        return intent;
    }
    //----------------------------------------------------------------------------------------------
    @Override
    protected Fragment createFragment() {
        PhotoPageFragment photoPageFragment = PhotoPageFragment.newInstance(getIntent().getData());
        mPhotoPageFragment = photoPageFragment;
        return photoPageFragment;
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onBackPressed(){ //***
        mWebView = mPhotoPageFragment.getWebView();
        if (mWebView.canGoBack())
            mWebView.goBack();
        else
            super.onBackPressed();
    }
    //----------------------------------------------------------------------------------------------
}
