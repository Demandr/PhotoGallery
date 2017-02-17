package com.example.oleksandr.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Oleksandr on 11.02.2017.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();
    private Handler mResponseHandler;
    private ThumbnailDownloaderListener mThumbnailDownloaderListner;

    public interface  ThumbnailDownloaderListener<T>{
        void onThumbnailDownloader(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloaderListner(ThumbnailDownloaderListener<T> listener){
        mThumbnailDownloaderListner = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message message) {
                if (message.what == MESSAGE_DOWNLOAD){
                    T target = (T) message.obj;
                    Log.i(TAG, "Got a request for url: " + mRequestMap.get(target));

                    handleRequest(target);
                }
            }
        };


    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a  URL : " + url);

        if (url == null){
            mRequestMap.remove(target);
        }else{
            mRequestMap.put(target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    private void handleRequest(final T target){
        try{
            final String url = mRequestMap.get(target);
            if (url == null){
                return;
            }
            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloaderListner.onThumbnailDownloader(target, bitmap);
                }
            });
        }catch (IOException ioe){
            Log.e(TAG, "Error download image", ioe);
        }
    }
}
