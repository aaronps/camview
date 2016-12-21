/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aaronps.camview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 *
 * @author krom
 */
public class CameraView extends JComponent
{
    static final private int DEFAULT_WIDTH = 320;
    static final private int DEFAULT_HEIGHT = 240;
    
    private BufferedImage mImage;
    private double mRotation = 0;
    
    private int[] mBuffer;
    
    
    public CameraView()
    {
        reset();
    }
    
    public final void reset()
    {
        setImageSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        // keep drawDefaultPattern here, setImageSize will only draw the patter
        // if the size is different, it might be possible the size is the same
        // so it was not drawn.
        drawDefaultPattern();
    }

    @Override
    final public void paint(Graphics g)
    {
        super.paint(g);
        
        final int width = getWidth();
        final int height = getHeight();
        final int img_width = mImage.getWidth();
        final int img_height = mImage.getHeight();
        
        int dx, dy, dw, dh;
        
        final int h_for_vw = width * img_height / img_width;
        if ( h_for_vw <= height )
        {
            dw = width;
            dh = h_for_vw;
            dx = 0;
            
            dy = (height - dh) / 2;
        }
        else
        {
            dw = height * img_width / img_height; 
            dh = height;
            dy = 0;
            
            dx = (width - dw) / 2;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.rotate(mRotation, width/2, height/2);
        
        g2d.drawImage(mImage, dx, dy, dw, dh, null);
    }
    
    final public void updatePic(final byte[] data)
    {
        try
        {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data)); // ignore size for now
            if ( mImage != null ) {
                
            }
            mImage = bi;
            repaint();
        }
        catch (IOException ex)
        {
            Logger.getLogger(CameraView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    final public void setRotation(final int rot)
    {
        switch ( rot )
        {
            case 0: mRotation = 0; break;
            case 1: mRotation = Math.PI/2; break;
            case 2: mRotation = Math.PI; break;
            case 3: mRotation = Math.PI + Math.PI/2; break;
            default: mRotation = 0;
        }
    }
    
    final public void setImageSize(final int w, final int h)
    {
        if ( mImage != null )
        {
            if ( mImage.getWidth() == w && mImage.getHeight() == h )
            {
                return;
            }
        }
        
        mImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        mBuffer = new int[w*h];
        
        drawDefaultPattern();
    }
    
    /**
     * Draws a default pattern on the image.
     * 
     * Also useful to test the image type format, for TYPE_INT_RGB should be
     * blue.
     */
    private void drawDefaultPattern()
    {
        final int w = mImage.getWidth(), h = mImage.getHeight();
        final int[] data = mBuffer;
        for ( int n = 0; n < h; n++ )
        {
            Arrays.fill(data, n*w, (n+1)*w, n);
        }
        mImage.getRaster().setDataElements(0, 0, w, h, data);
    }
    
    /**
     * Converts an nv21 byte array to a int array as expected by
     * BufferedImage.TYPE_INT_RGB.
     * 
     * @param src nv21 format source array
     * @param dst destination array
     * @param width image width
     * @param height image height
     */
    private static void nv21ToRGB(final byte[] src,
                                  final int[] dst,
                                  final int width,
                                  final int height) {
        final int size = width*height;

        for (int yi=0, uvi=size; yi < size; uvi+=2) {
            final int v = (src[uvi  ]&0xff) - 128;
            final int u = (src[uvi+1]&0xff) - 128;
            
            final int rval = (int)(1.402f*v);
            final int gval = (int)(0.344f*u + 0.714f*v);
            final int bval = (int)(1.772f*u);

            dst[yi  ]       = y2rgb(src[yi  ]&0xff, rval, gval, bval);
            dst[yi+1]       = y2rgb(src[yi+1]&0xff, rval, gval, bval);
            dst[width+yi  ] = y2rgb(src[width+yi  ]&0xff, rval, gval, bval);
            dst[width+yi+1] = y2rgb(src[width+yi+1]&0xff, rval, gval, bval);

            yi += 2;
            if ( (yi % width) == 0)
                yi += width;
        }
    }
    
    /**
     * Converts Y value (from yuv) to RGB using precomputed uv values.
     * 
     * @param y Y value from yuv
     * @param rval precomputed value for red
     * @param gval precomputed value for green
     * @param bval precomputed value for blue
     * @return integer value expected by BufferedImage.TYPE_INT_RGB
     */
    private static int y2rgb( final int y,
                              final int rval,
                              final int gval,
                              final int bval ) {
        int r = y + rval;
        int g = y - gval;
        int b = y + bval;
        
        r = r>255 ? 255 : r<0 ? 0 : r;
        g = g>255 ? 255 : g<0 ? 0 : g;
        b = b>255 ? 255 : b<0 ? 0 : b;
        
        return (r<<16) | (g<<8) | b;
    }
    
}
