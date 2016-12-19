package com.aaronps.camview;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author krom
 */
public final class FrameRequester implements Runnable {

    /** 
     * @attention CameraThread may be called from different threads, I'm not
     * going to synchronize it here, let's see if problems arise.
     */
    private final CameraThread mCameraThread;
    private final ScheduledExecutorService mExecutor;
    private long mLastTimestamp = 0;
    private boolean mScheduled = false;
    private long mMinDelay = 66;
    
    
    public FrameRequester(final CameraThread cameraThread) {
        mCameraThread = cameraThread;
        mExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void setMinDelay(final long minDelay) {
        mMinDelay = minDelay;
    }
    
    public void shutdown() {
        mExecutor.shutdownNow();
    }
    
    public synchronized void request_pic() {
        if ( mMinDelay == 0 )
        {
            mCameraThread.request_pic();
        }
        else
        {
            final long now = System.currentTimeMillis();
            final long dif = now - mLastTimestamp;
            if ( dif >= mMinDelay )
            {
                if ( ! mScheduled )
                {
                    mCameraThread.request_pic();
                    mLastTimestamp = now;
                }
            }
            else
            {
                if ( ! mScheduled )
                {
                    mScheduled = true;
                    if ( dif > 0 )
                        mExecutor.schedule(this, mMinDelay - dif, TimeUnit.MILLISECONDS);
                    else
                        mExecutor.submit(this);
                }
            }
            
        }
    }
    
    @Override
    public void run() {
        mCameraThread.request_pic();
        
        synchronized(this)
        {
            mScheduled = false;
            mLastTimestamp = System.currentTimeMillis();
        }
    }
    
}
