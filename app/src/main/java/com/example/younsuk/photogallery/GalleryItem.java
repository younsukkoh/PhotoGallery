package com.example.younsuk.photogallery;

import android.net.Uri;
import android.widget.ImageView;

/**
 * Created by Younsuk on 10/3/2015.
 */
public class GalleryItem {
    private String mCaption;
    private String mId;
    private String mUrl;
    private String mOwner;
    //----------------------------------------------------------------------------------------------
    @Override
    public String toString(){
        return mCaption;
    }

    public String getCaption() { return mCaption; }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getOwner() {return mOwner; }

    public void setOwner(String owner) { mOwner = owner; }

    public Uri getPhotoPageUri(){
        return Uri.parse("http://www.flickr.com/photos/").buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }
}
