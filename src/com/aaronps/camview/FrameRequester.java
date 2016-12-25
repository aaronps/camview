package com.aaronps.camview;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author krom
 */
public final class FrameRequester implements Runnable {
    
    private final static Logger logger = Logger.getLogger("FrameRequester");

    /**
     * @attention CameraThread may be called from different threads, I'm not
     * going to synchronize it here, let's see if problems arise.
     */
    private final RemoteCamera mRemoteCamera;
    private final ScheduledExecutorService mExecutor;
    private long mLastTimestamp = 0;
    private Future<?> mFuture;
    private long mMinDelay = 66; // 66 = ~15 fps
    private boolean mPaused = false;

    public FrameRequester(final RemoteCamera remoteCamera) {
        mRemoteCamera = remoteCamera;
        mExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void setMinDelay(final long minDelay) {
        mMinDelay = minDelay;
        if ( mFuture != null )
        {
            // There was a pending request, try to cancel it and re-schedule
            if ( mFuture.cancel(false) ) 
            {
                // because it was canceled properly (didn't run) we need to null
                // it before request_pic().
                mFuture = null;
                request_pic();
            }
            // if it was not possible to cancel, it measns the request did start
            // and right now it is waiting for me to finish before entering the
            // "synchronized" section, we are sure of that because "mFuture" was
            // not null.
        }
    }

    public void shutdown() {
        mExecutor.shutdownNow();
    }

    public synchronized void cancel() {
        if (mFuture != null)
        {
            mFuture.cancel(false);
            mFuture = null;
        }
    }

    public void pause() {
        cancel();
        mPaused = true;
    }
    
    public void resume() {
        mPaused = false;
    }

    public synchronized void request_pic() {
        if (mPaused)
        {
            return;
        }
        
        try
        {
            if (mMinDelay == 0)
            {
                mRemoteCamera.request_pic();
            }
            else
            {
                final long now = System.currentTimeMillis();
                final long dif = now - mLastTimestamp;
                if (dif >= mMinDelay)
                {
                    if (mFuture == null)
                    {
                        mRemoteCamera.request_pic();
                        mLastTimestamp = now;
                    }
                }
                else
                {
                    if (mFuture == null)
                    {
                        if (dif > 0)
                        {
                            mFuture = mExecutor.schedule(this, mMinDelay - dif, TimeUnit.MILLISECONDS);
                        }
                        else
                        {
                            mFuture = mExecutor.submit(this);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            logger.log(Level.SEVERE, "Friends, it seems we cannot request pics", e);
        }
    }

    @Override
    public void run() {
        try
        {
            mRemoteCamera.request_pic();
        }
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, "Thread unable to request pic... let's hope someone closes that socket", ex);
        }

        synchronized (this)
        {
            mFuture = null;
            mLastTimestamp = System.currentTimeMillis();
        }
    }

}
