package com.example.younsuk.photogallery;

import android.graphics.drawable.Drawable;
import android.support.v4.util.LruCache;

/**
 * Created by Younsuk on 10/12/2015.
 */
public class GalleryLruCache {

    private LruCache<String, Drawable> mMemoryCache;
    //----------------------------------------------------------------------------------------------
    public GalleryLruCache(){
        final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<>(cacheSize);
    }
    //----------------------------------------------------------------------------------------------
    public void setGalleryCache(String url, Drawable image){
        if (getGalleryCache(url) == null)
            mMemoryCache.put(url, image);
    }
    //----------------------------------------------------------------------------------------------
    public Drawable getGalleryCache(String url){ //returns null if the image is not in the cache
        return mMemoryCache.get(url);
    }
    //----------------------------------------------------------------------------------------------
}
