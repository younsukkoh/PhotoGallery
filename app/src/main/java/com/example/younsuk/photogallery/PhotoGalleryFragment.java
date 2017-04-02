package com.example.younsuk.photogallery;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Younsuk on 10/3/2015.
 */
public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    private static final int SCROLL_STATE_IDLE = 0;

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private Drawable loadingScreen;
    private GalleryLruCache mCacheDisk;
    private PhotoAdapter mPhotoAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private boolean loadingScroll = true;
    private int previousItemCount, currentItemCount, totalItemCount;
    public static int sPageNumber = 1;

    private GridLayoutManager mLayoutManager;
    //----------------------------------------------------------------------------------------------
    public static PhotoGalleryFragment newInstance(){ return new PhotoGalleryFragment(); }
    //----------------------------------------------------------------------------------------------
    public static Intent newIntent(Context context){ return new Intent(context, PhotoGalleryActivity.class); }
    //----------------------------------------------------------------------------------------------
    @TargetApi(21)
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems(sPageNumber);

        loadingScreen = getResources().getDrawable(R.drawable.bill_up_close, null);
        mCacheDisk = new GalleryLruCache();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        target.bindDrawable(drawable);
                        target.setProgressBarFalse();
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView)view.findViewById(R.id.fragment_photo_gallery_recycler_view);

        mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mLayoutManager);

        mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.fragment_photo_gallery_swipe_refresh);
        mSwipeRefreshLayout.setEnabled(false);

        setupAdapter();

        return view;
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView)searchItem.getActionView();
        searchItem.setActionView(searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity(), query);
                sPageNumber = 1;
                updateItems(sPageNumber);
                searchItem.collapseActionView();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity()))
            toggleItem.setTitle(R.string.stop_polling);
        else
            toggleItem.setTitle(R.string.start_polling);
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems(sPageNumber);
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //----------------------------------------------------------------------------------------------
    private void updateItems(int pageNumber){
        String query = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemTask(query).execute(pageNumber);
    }
    //----------------------------------------------------------------------------------------------
    private void setupAdapter(){
        if(isAdded()){
            mPhotoAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mPhotoAdapter);
        }
    }
    //----------------------------------------------------------------------------------------------
    private class FetchItemTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params){
            if (mQuery == null)
                return new FlickrFetchr().fetchRecentPhotos(params[0]);
            else
                return new FlickrFetchr().searchPhotos(mQuery, params[0]);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            if (sPageNumber == 1){
                mItems = items;
                setupAdapter();
            }
            else {
                mItems.addAll(items);
                mPhotoAdapter.notifyDataSetChanged(); //Do not setup adapter here, because that will refresh the UI back to page 1.
            }
        }
    }
    //----------------------------------------------------------------------------------------------
    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;
        private ProgressBar mProgressBar_Thumbnail;
        private String mImageUrl;

        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);
            itemView.setOnClickListener(this);
            mProgressBar_Thumbnail = (ProgressBar)itemView.findViewById(R.id.gallery_item_progress_bar);
        }

        public void bindDrawable(Drawable drawable) {
            if (drawable.equals(loadingScreen)) {
                if (mCacheDisk.getGalleryCache(mImageUrl) == null)
                    mItemImageView.setImageDrawable(drawable);
                else
                    mItemImageView.setImageDrawable(mCacheDisk.getGalleryCache(mImageUrl));
            } else {
                mCacheDisk.setGalleryCache(mImageUrl, drawable);
                mItemImageView.setImageDrawable(drawable);
            }
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            mGalleryItem = galleryItem;
            mImageUrl = galleryItem.getUrl();
        }

        @Override
        public void onClick(View v) {
            Intent intent = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(intent);
        }

        public void setProgressBarTrue(){ mProgressBar_Thumbnail.setVisibility(View.VISIBLE);}

        public void setProgressBarFalse(){ mProgressBar_Thumbnail.setVisibility(View.INVISIBLE); }

        public void isLoading(){
            bindDrawable(loadingScreen);

            if (mCacheDisk.getGalleryCache(mImageUrl) == null)
                setProgressBarTrue();
            else
                setProgressBarFalse();
        }
    }
    //----------------------------------------------------------------------------------------------
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;

            mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
                    super.onScrollStateChanged(recyclerView, scrollState);

                    currentItemCount = mLayoutManager.getChildCount(); //Items that are currently on the screen.
                    previousItemCount = mLayoutManager.findFirstVisibleItemPosition(); //Index of first item
                    totalItemCount = mLayoutManager.getItemCount();

                    if (loadingScroll) {
                        if (galleryIsAtTheBottom(scrollState)) {
                            loadingScroll = false;
                            Toast.makeText(getActivity(), "Page " + (sPageNumber + 1) + " loaded.", Toast.LENGTH_SHORT).show();
                            updateItems(++sPageNumber);
                            loadingScroll = true;
                        }
                    }//close if(loadingScroll)

                    if (galleryIsAtTheTop(scrollState))
                        mSwipeRefreshLayout.setEnabled(true);
                    else
                        mSwipeRefreshLayout.setEnabled(false);
                }
            });

            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                if (canRefresh()) {
                    Toast.makeText(getActivity(), "New Photos Loaded", Toast.LENGTH_SHORT).show();
                    updateItems(sPageNumber);
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                else {
                    mSwipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getActivity(), "No New Photos", Toast.LENGTH_SHORT).show();
                }
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType){
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position){
            GalleryItem galleryItem = mGalleryItems.get(position);
            photoHolder.bindGalleryItem(galleryItem);
            photoHolder.isLoading();
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() { return mGalleryItems.size(); }
    }
    //----------------------------------------------------------------------------------------------
    private boolean canRefresh(){
        if (mItems.size() == 0)
            return false;

        String lastResultId = QueryPreferences.getLastResultId(getActivity());
        String resultId = mItems.get(0).getId();

        if (!lastResultId.equals(resultId))
            return true;
        else
            return false;
    }
    //----------------------------------------------------------------------------------------------
    private boolean galleryIsAtTheBottom(int scrollState){ return currentItemCount + previousItemCount >= totalItemCount && scrollState == SCROLL_STATE_IDLE; }
    //----------------------------------------------------------------------------------------------
    private boolean galleryIsAtTheTop(int scrollState){ return previousItemCount == 0 && scrollState == SCROLL_STATE_IDLE; }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }
    //----------------------------------------------------------------------------------------------

}
