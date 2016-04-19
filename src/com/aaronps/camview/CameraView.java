/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aaronps.camview;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
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
    
    public CameraView()
    {
        reset();
    }
    
    public final void reset()
    {
        setImageSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        drawDefaultPattern(mImage);
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
        mImage.getRaster().setDataElements(0, 0, mImage.getWidth(), mImage.getHeight(), data);
        repaint();
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
            final BufferedImage img = mImage;
            if ( img.getWidth() == w && img.getHeight() == h )
            {
                return;
            }
        }
        
        final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        drawDefaultPattern(img);
        mImage = img;
    }
    
    final public void drawDefaultPattern(final BufferedImage img)
    {
        final int w = img.getWidth(), h = img.getHeight();
        final byte[] data = new byte[w*h];
        for ( int n = 0; n < h; n++ )
        {
            Arrays.fill(data, n*w, (n+1)*w, (byte)(n&0xff));
        }
        img.getRaster().setDataElements(0, 0, w, h, data);
    }
    
}
