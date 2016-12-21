/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aaronps.camview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author krom
 */
public class CameraThread implements Runnable
{
    public static interface Listener
    {
        void onConnected(final CameraThread ct);
        void onDisconnected(final CameraThread ct);
        void onVideoReady(final CameraThread ct, final CameraInfo info);
        void onSizeListReceived(final CameraThread ct, final String[] sizes);
        void onFrameReceived(final CameraThread ct, final byte[] frame);
    }
    
    private static final Logger logger = Logger.getLogger("CameraThread");
    
    private static final byte[] Request_Pic = "Pic\n".getBytes();
    private static final byte[] Request_SizeList = "SizeList\n".getBytes();
//    private static final byte[] Request_BeginVideo = "Begin\n".getBytes();
    
    private volatile Thread thread = null;
    private final Listener listener;
    private final String host;
    private final int port;
    
    private Socket loopSocket;
    private OutputStream socketOutputStream;
    
    public CameraThread(final Listener listener, final String host, final int port)
    {
        this.listener = listener;
        this.host = host;
        this.port = port;
    }
    
    public synchronized void start()
    {
        if ( thread == null )
        {
            logger.info("Thread start");
            thread = new Thread(this);
            thread.start();
        }
    }
    
    public synchronized void stop() throws InterruptedException
    {
        if ( thread != null )
        {
            logger.info("Thread stop");
            final Thread t = thread;
            thread = null;
            try { loopSocket.close(); } catch ( Exception e) {}
            t.interrupt();
            t.join();
        }
        
    }
    
    final public void request_pic()
    {
        try
        {
            socketOutputStream.write(Request_Pic);
            socketOutputStream.flush();
        }
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    final public void request_sizelist()
    {
        try
        {
            socketOutputStream.write(Request_SizeList);
            socketOutputStream.flush();
        }
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    final public void request_beginvideo(final String size)
    {
        try
        {
            socketOutputStream.write(("BeginVideo " + size + "\n").getBytes());
            socketOutputStream.flush();
        }
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    final public void request_stopvideo()
    {
        try
        {
            socketOutputStream.write(("StopVideo\n").getBytes());
            socketOutputStream.flush();
        }
        catch (IOException ex)
        {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void run()
    {        
        logger.info("run begin");
        while ( thread != null )
        {
            
            logger.info("Loop begin");
            try ( final ByteArrayOutputStream inputLine = new ByteArrayOutputStream(128);
                  final Socket socket = new Socket(host, port);
//                  final BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
                  final InputStream input = socket.getInputStream();
                )
            {
                logger.info("Connected");
                loopSocket = socket;
                socketOutputStream = socket.getOutputStream();
                
                listener.onConnected(this);
                
                readLine(input, inputLine);
                
                String line = inputLine.toString();
                while ( ! line.isEmpty() )
                {
//                    logger.info("Got line: [" + line + "]");
                    final String[] parts = line.split(" ");
                    switch (parts[0])
                    {
                        case "Ready":
                        {
                            final CameraInfo info = new CameraInfo();
                            info.width = Integer.parseInt(parts[1]);
                            info.height = Integer.parseInt(parts[2]);
                            info.type = Integer.parseInt(parts[3]);
                            
                            listener.onVideoReady(this, info);
                            break;
                        }
                        case "Pic":
                        {
                            final int len = Integer.parseInt(parts[1]);
                            if ( len > 0 )
                            {
                                final byte[] frame_buffer = new byte[len];
                                readBuffer(input, frame_buffer, len);
                                listener.onFrameReceived(this, frame_buffer);
                            }
                            break;
                        }
                        case "SizeList":
                        {
                            listener.onSizeListReceived(this, Arrays.copyOfRange(parts, 1, parts.length));
                            break;
                        }
                        default:
                        {
                            logger.info("Unknown message from camera: [" + line + "]" );
                        }
                        
                    }
                    
                    readLine(input, inputLine);
                    line = inputLine.toString();
                }
            }
            catch (IOException ex)
            {
                logger.log(Level.SEVERE, null, ex);
                try { Thread.sleep(5000); } catch (InterruptedException ex1) {}
            }
            finally
            {
                loopSocket = null;
                socketOutputStream = null;
            }
//            catch (InterruptedException ex)
//            {
//                logger.log(Level.INFO, "Interrupted", ex);
//            }
            logger.info("Loop end");
                    
        }
        logger.info("run end");
    }
    
    private static void readLine(final InputStream input, final ByteArrayOutputStream line) throws IOException
    {
        line.reset();
        int c;
        while ( (c = input.read()) != -1 )
        {
            if ( c == '\n' ) break;
            line.write(c);
        }
    }
    
    private static void readBuffer(final InputStream input, final byte[] buffer, final int length) throws IOException
    {
        int pos = 0;
        do
        {
            final int readed = input.read(buffer, pos, length - pos);
            if ( readed == -1 ) break;
            pos += readed;
        } while ( pos < length );
    }
    
}
