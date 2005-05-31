/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.antlr.works.visualization.graphics.panel;

import edu.usfca.xj.appkit.frame.XJFrame;
import edu.usfca.xj.appkit.menu.XJMenu;
import edu.usfca.xj.appkit.menu.XJMenuItem;
import edu.usfca.xj.appkit.menu.XJMenuItemDelegate;
import org.antlr.works.visualization.graphics.GContext;
import org.antlr.works.visualization.graphics.graph.GGraphAbstract;
import org.antlr.works.visualization.graphics.graph.GGraphGroup;
import org.antlr.works.visualization.graphics.path.GPath;
import org.antlr.works.visualization.graphics.path.GPathGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GView extends JPanel implements XJMenuItemDelegate {

    private boolean useCachedImage = true;
    private boolean cachedImageRerender = false;
    private boolean cachedImageResize = false;

    private BufferedImage cachedImage = null;
    private Dimension outOfMemoryDimension = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private List graphs = new ArrayList();
    private int currentGraphIndex = 0;
    private GContext context;

    private Point lastMouse;

    public int offset_x = 10;
    public int offset_y = 10;

    public int prev_offset_x = 0;
    public int prev_offset_y = 0;

    public GView(GContext context) {
        this.context = context;
        this.context.setContainer(this);

        setFocusable(true);

        setBackground(Color.white);
        adjustSize();

        addMouseMotionListener(new DefaultMouseMotionListener());
        addMouseListener(new DefaultMouseListener());
        addKeyListener(new DefaultKeyListener());
    }

    public void setEnable(boolean flag) {
        for (Iterator iterator = graphs.iterator(); iterator.hasNext();) {
            GGraphAbstract graph = (GGraphAbstract) iterator.next();
            graph.setEnable(flag);
        }
    }

    public void setParent(XJFrame parent) {
      //  this.parent = parent;
    }

    public void setGraphs(List graphs) {
        if(graphs == null)
            return;

        this.graphs.clear();
        this.graphs.addAll(graphs);

        if(currentGraphIndex >= graphs.size())
            currentGraphIndex = graphs.size()-1;

        applyContext();
    }

    public List getGraphs() {
        return graphs;
    }

    public void applyContext() {
        for (Iterator iterator = graphs.iterator(); iterator.hasNext();) {
            GGraphAbstract graph = (GGraphAbstract) iterator.next();
            graph.setContext(context);
        }
    }

    public void setCacheEnabled(boolean flag) {
        if(useCachedImage != flag) {
            useCachedImage = flag;
            cacheInvalidate();
        }
    }

    public boolean isCachedEnabled() {
        return useCachedImage;
    }

    public void cacheInvalidate() {
        cachedImage = null;
    }

    public void cacheRerender() {
        cachedImageRerender = true;
    }

    public void setCacheResizeImage(boolean flag) {
        cachedImageResize = flag;
    }

    public BufferedImage getCachedImage() {
        return cachedImage;
    }

    public boolean setNextGraph() {
        currentGraphIndex++;
        if(currentGraphIndex>=graphs.size()) {
            currentGraphIndex = graphs.size()-1;
            return false;
        } else
            return true;
    }

    public boolean setPrevGraph() {
        currentGraphIndex--;
        if(currentGraphIndex<0) {
            currentGraphIndex = 0;
            return false;
        } else
            return true;
    }

    public int getCurrentGraphIndex() {
        return currentGraphIndex;
    }

    public GGraphAbstract getCurrentGraph() {
        if(graphs.size()>0)
            return (GGraphAbstract)graphs.get(currentGraphIndex);
        else
            return null;
    }

    public GGraphGroup getCurrentGraphGroup() {
        return (GGraphGroup)getCurrentGraph();
    }

    public GPathGroup getCurrentPathGroup() {
        return getCurrentGraphGroup().pathGroup;
    }

    public GPath getCurrentPath() {
        return getCurrentPathGroup().currentPath();
    }

    public void refresh() {
        if(getCurrentGraph() != null)
            getCurrentGraph().render(0, 0);

        cacheInvalidate();
        adjustSize();
        repaint();
    }

    public void refreshSizeChanged(boolean useCacheImageResize) {
        if(useCachedImage) {
            setCacheResizeImage(useCacheImageResize);
            if(!useCacheImageResize) {
                getCurrentGraph().render(0, 0);
                cacheInvalidate();
            }
            adjustSize();
            repaint();
        } else {
            refresh();
        }
    }

    public void adjustSize() {
        if(getCurrentGraph() == null || context == null)
            return;

        Dimension dimension = new Dimension(getGraphWidth()+2*offset_x, getGraphHeight()+2*offset_y);
        setSize(dimension);
        setPreferredSize(dimension);
    }

    public int getGraphWidth() {
        if(getCurrentGraph().getDimension() == null)
            return 400;
        else
            return (int)getCurrentGraph().getWidth()+20;
    }

    public int getGraphHeight() {
        if(getCurrentGraph().getDimension() == null)
            return 200;
        else
            return (int)(getCurrentGraph().getDimension().getPixelHeight(context));
    }

    public void addMenuItem(JPopupMenu menu, String title, int tag, Object object) {
        XJMenuItem item = new XJMenuItem();
        item.setTitle(title);
        item.setTag(tag);
        item.setObject(object);
        item.setDelegate(this);

        menu.add(item.getSwingComponent());
    }

    public JPopupMenu getContextualMenu() {
        return null;
    }

    public void handleMenuEvent(XJMenu menu, XJMenuItem item) {
    }

    public void processMouseEvent(MouseEvent e) {
        if(e.isPopupTrigger()) {
            JPopupMenu menu = getContextualMenu();
            if(menu != null)
                menu.show(this, e.getX(), e.getY());
        } else
            super.processMouseEvent(e);
    }

    public boolean canDraw() {
        return getCurrentGraph() != null && getCurrentGraph().getDimension() != null && getCurrentGraph().isRendered();
    }

    public void render(Graphics2D g2d) {
        context.offsetX = offset_x;
        context.offsetY = offset_y;
        context.setGraphics2D(g2d);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        getCurrentGraph().draw();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        //System.out.println("Repaint "+System.currentTimeMillis());
        if(!canDraw())
            return;

        int width = getGraphWidth()+offset_x;
        int height = getGraphHeight()+offset_y+1;

        if(useCachedImage) {
            boolean sizeChanged = cachedImage != null && (cachedImage.getWidth() != width || cachedImage.getHeight() != height);

            if(sizeChanged) {
                // Discard the cache image only if it already exists and if the cachedImageResize flag is false.
                // The cachedImageResize flag indicates, if true, that we should use the cachedImage
                // instead of re-creating a new one (useful for fast live resize).
                if(!cachedImageResize && cachedImage != null) {
                    cachedImage.flush();
                    cachedImage = null;
                    System.gc();
                }
            }

            if(cachedImage == null) {
                // Create a new cache image.
                // @todo See what to do with this memory problem (leak somewhere)
                if(width<outOfMemoryDimension.width && height<outOfMemoryDimension.height) {
                    try {
                        cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                        Graphics2D gCache = (Graphics2D)cachedImage.getGraphics();
                        gCache.setColor(Color.white);
                        gCache.fillRect(0, 0, width, height);
                        render(gCache);
                        gCache.dispose();
                    } catch(OutOfMemoryError e) {
                        outOfMemoryDimension.width = width;
                        outOfMemoryDimension.height = height;
                        cachedImage = null;
                        System.gc();
                        System.err.println("Out of memory, disabling cache ("+(int)(width*height*3.0/(1024*1024))+" Mb)");
                    }
                }
            } else if(cachedImageRerender) {
                // Only render the cachedImage without re-creating it again
                Graphics2D gCache = (Graphics2D)cachedImage.getGraphics();
                gCache.setColor(Color.white);
                gCache.fillRect(0, 0, width, height);
                render(gCache);
                gCache.dispose();
                cachedImageRerender = false;
            }
        }

        if(cachedImage == null)
            render((Graphics2D)g);
        else
            g.drawImage(cachedImage, 0, 0, width, height, null);

        if(!cachedImageResize && getCurrentGraph() instanceof GGraphGroup) {
            // Draw the selected segment of a path (and only if we are not resizing using only the cached image)
            Graphics2D g2d = (Graphics2D)g;
            context.offsetX = offset_x;
            context.offsetY = offset_y;
            context.setGraphics2D(g2d);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            getCurrentPathGroup().drawSelectedElement();
        }
    }

    public class DefaultMouseMotionListener extends MouseMotionAdapter {

        public void mouseDragged(MouseEvent e) {
            if(lastMouse == null)
                return;
            
            Point mouse = e.getPoint();
            int dx = mouse.x - lastMouse.x;
            int dy = mouse.y - lastMouse.y;
            offset_x = prev_offset_x+dx;
            offset_y = prev_offset_y+dy;
            refresh();
        }

        public void mouseMoved(MouseEvent e) {
        }
    }

    public class DefaultMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            if(!isFocusOwner()) {
                requestFocus();
                return;
            }

            setCacheEnabled(false);
            prev_offset_x = offset_x;
            prev_offset_y = offset_y;
            lastMouse = e.getPoint();

            if(getCurrentGraph() instanceof GGraphGroup)
                handleMousePressedInGraphGroup(e);
        }

        public void mouseReleased(MouseEvent e) {
            lastMouse = null;
            setCacheEnabled(true);
        }

        public void handleMousePressedInGraphGroup(MouseEvent e) {
            getCurrentPathGroup().selectPath(e.getPoint());
        }
    }

    public class DefaultKeyListener extends KeyAdapter {

        public void keyPressed(KeyEvent e) {
            if(getCurrentGraph() instanceof GGraphGroup)
                handleKeyPressedInGraphGroup(e);
        }

        public void handleKeyPressedInGraphGroup(KeyEvent e) {
            GPath path = getCurrentPath();
            switch(e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:
                    path.nextElement();
                    e.consume();
                    break;
                case KeyEvent.VK_LEFT:
                    path.previousElement();
                    e.consume();
                    break;
                case KeyEvent.VK_UP:
                    path.lastElement();
                    e.consume();
                    break;
                case KeyEvent.VK_DOWN:
                    path.firstElement();
                    e.consume();
                    break;

                case KeyEvent.VK_A:
                    getCurrentPathGroup().toggleShowRuleLinks();
                    cacheRerender();
                    e.consume();
                    break;
            }

            if(e.isConsumed())
                repaint();
        }
    }
}
