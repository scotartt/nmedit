/* Copyright (C) 2006 Christian Schneider
 * 
 * This file is part of Nomad.
 * 
 * Nomad is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * Nomad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Nomad; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/*
 * Created on Oct 29, 2006
 */
package net.sf.nmedit.nomad.core.swing.explorer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.plaf.metal.MetalTreeUI;
import javax.swing.tree.TreePath;

import net.sf.nmedit.nomad.core.swing.explorer.helpers.ExplorerCellRenderer;
import net.sf.nmedit.nomad.core.swing.explorer.helpers.TreeDynamicTreeExpansion;

public class ExplorerTreeUI extends MetalTreeUI
{
    private static Icon getIcon(String name)
    {
        URL url = ExplorerTreeUI.class.getClassLoader().getResource( "swing/browser/"+name);
        return url == null ? null : new ImageIcon(url);
    }

    static Icon openIcon = getIcon("node-state-opened.png");
    static Icon closedIcon = getIcon("node-state-closed.png");
    static Icon openIconHov = getIcon("node-state-opened-hovered.png");
    static Icon closedIconHov = getIcon("node-state-closed-hovered.png");

    public static final Icon DefaultFolderIcon = getIcon("fldr_obj.gif");
    public static final Icon DefaultFileIcon = getIcon("file_obj.gif");
    //Icon leafIcon = new ImageIcon("net/sf/nmedit/nomad/cbrowser/images/...");
     
    public final static Color defaultSelectionBackground = Color.decode("#A8A8A8");
    private Color backgroundSelectionColor = null;
    private Color alternatingRowColor = null;

    public void installUI( JComponent c ) 
    {
        JTree tree = (JTree) c;
        tree.putClientProperty("JTree.lineStyle", "None");
        super.installUI( c );
        
        c.setBackground(Color.WHITE);
        alternatingRowColor = new Color(0xF0F0FF);
        
        tree.setRootVisible(false);
        ExplorerCellRenderer tcr = new ExplorerCellRenderer();
        tree.setCellRenderer(tcr);
        tcr.setOpenIcon(DefaultFolderIcon);
        tcr.setClosedIcon(DefaultFolderIcon);
        tcr.setLeafIcon(DefaultFileIcon);
        tree.addTreeExpansionListener(new TreeDynamicTreeExpansion(tree));
        tree.setShowsRootHandles(true);
        tree.setScrollsOnExpand(false);
        
        DND dnd = new DND();
        
        
        dnd.dragSource = new DragSource();
        DragGestureRecognizer dgr = 
            dnd.dragSource.createDefaultDragGestureRecognizer(tree, 
                    DnDConstants.ACTION_COPY_OR_MOVE, dnd);
        
        dnd.dropTarget = new DropTarget(tree, DnDConstants.ACTION_COPY_OR_MOVE
                | DnDConstants.ACTION_LINK,
                dnd);
    }
    protected void installDefaults() 
    {
        //tree.putClientProperty("Tree.selectionBackground", defaultSelectionBackground);
        super.installDefaults();
        backgroundSelectionColor = defaultSelectionBackground;//UIManager.getColor("Tree.selectionBackground");
        setExpandedIcon(openIcon);
        setCollapsedIcon(closedIcon);
        
    }
    
    private class DND implements 
        DragGestureListener,
        DragSourceListener,
        DropTargetListener
    {
        
        DragSource dragSource;
        DropTarget dropTarget;

        public void dragGestureRecognized(DragGestureEvent dge)
        {
            Point p = dge.getDragOrigin();
            TreePath path = getClosestPathForLocation(tree, p.x, p.y);
            if (path == null)
                return;
            
            Object node = path.getLastPathComponent();
            if (!(node instanceof Transferable))
                return;
            
            Transferable transferable = (Transferable) node;
            
            dragSource.startDrag(dge, 
                    DragSource.DefaultMoveDrop,
                    transferable,
                    this
                    );
        }

        public void dragDropEnd(DragSourceDropEvent dsde)
        {
            // no op
        }

        public void dragEnter(DragSourceDragEvent dsde)
        {
            // no op
        }

        public void dragExit(DragSourceEvent dse)
        {
            // no op
        }

        public void dragOver(DragSourceDragEvent dsde)
        {
            // no op
        }

        public void dropActionChanged(DragSourceDragEvent dsde)
        {
            // no op
        }

        private boolean testFileFlavor(DataFlavor[] list)
        {
            for (DataFlavor f: list)
                if(f.isMimeTypeEqual("text/uri-list"))
                    return true;
            return false;
        }
        
        public void dragEnter(DropTargetDragEvent dtde)
        {
            if (testFileFlavor(dtde.getCurrentDataFlavors()))
                dtde.acceptDrag(DnDConstants.ACTION_LINK);
            else
                dtde.rejectDrag();
        }

        public void dragExit(DropTargetEvent dte)
        {
            // no op
        }

        public void dragOver(DropTargetDragEvent dtde)
        {
            if (testFileFlavor(dtde.getCurrentDataFlavors()))
                dtde.acceptDrag(DnDConstants.ACTION_LINK);
            else
                dtde.rejectDrag();
        }

        public void drop(DropTargetDropEvent dtde)
        {
            DataFlavor flavor = null;
            for (DataFlavor f: dtde.getCurrentDataFlavors())
                if (f.isMimeTypeEqual("text/uri-list"))
                {
                    flavor = f;
                    break;
                }
            if (flavor == null)
            {
                dtde.rejectDrop();
                return;
            }

            
            dtde.acceptDrop(DnDConstants.ACTION_LINK);
            List<File> files = new ArrayList<File>();

            try
            {
                BufferedReader r = new BufferedReader(flavor.getReaderForText(dtde.getTransferable()));
                String line;
                while ((line=r.readLine())!=null)
                {
                    if (line.startsWith("file:"))
                    {
                        File file = new File(URI.create(line));
                        if (file.exists())
                        {
                            files.add(file);
                        }
                    }
                }
            }
            catch (Throwable e)
            {
                dtde.rejectDrop();
                e.printStackTrace();
                return ;
            }
            
            for (File file: files)
            {
                if (file.isDirectory())
                {
                    FileContext fc = new FileContext((ExplorerTree)tree, file);
                    ((ExplorerTree)tree).getRoot()
                    .add(fc);
                    ((ExplorerTree)tree).fireRootChanged();
                }
            }
        }

        public void dropActionChanged(DropTargetDragEvent dtde)
        {
            // TODO Auto-generated method stub
            
        }
        
    }

    protected void paintRow(Graphics g, Rectangle clipBounds,
                Insets insets, Rectangle bounds, TreePath path,
                int row, boolean isExpanded,
                boolean hasBeenExpanded, boolean isLeaf) 
    {

        // Don't paint the renderer if editing this row.
        if(editingComponent != null && editingRow == row)
            return;

        int h = tree.getRowHeight();
        if (tree.isRowSelected(row))
        {
            g.setColor(backgroundSelectionColor);
            g.fillRect(clipBounds.x, h*row, clipBounds.width, h );
            

            /*if(shouldPaintExpandControl(path, row, isExpanded,
                        hasBeenExpanded, isLeaf)) {
                        */
  //          }
        }
        else if (alternatingRowColor != null && row%2==0)
        {
            g.setColor(alternatingRowColor);
            g.fillRect(clipBounds.x, h*row, clipBounds.width, h );
        }
        paintExpandControl(g, bounds, insets, bounds,
                path, row, isExpanded,
                hasBeenExpanded, isLeaf);
        
        super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded,
                hasBeenExpanded, isLeaf);
    }
    
    public void paint(Graphics g, JComponent c)
    {
        super.paint(g, c);

        Insets i = tree.getInsets();
        int rc = getRowCount(tree);
        int rh = getRowHeight();
        
        if (rh < 1)
            return;
        
        if (rc%2==1)
            rc++;
        
        int y = i.top + rc*rh;
        int bottom = tree.getHeight()-i.bottom;

        g.setColor(alternatingRowColor);
        //int r = tree.getWidth()-i.left-i.right;
        while (y<bottom)
        {
            g.fillRect(i.left, y, tree.getWidth(), rh);
            y+=rh*2;
        }
    }

    ExpandControlHoverEffect eche = new ExpandControlHoverEffect();
    protected void installListeners() 
    {
        super.installListeners();
        tree.addMouseListener(eche);
        tree.addMouseMotionListener(eche);
    }

    protected void uninstallListeners() 
    {
        tree.removeMouseMotionListener(eche);
        tree.removeMouseListener(eche);
        super.uninstallListeners();
    }

    protected void paintExpandControl(Graphics g,
                      Rectangle clipBounds, Insets insets,
                      Rectangle bounds, TreePath path,
                      int row, boolean isExpanded,
                      boolean hasBeenExpanded,
                      boolean isLeaf) {
        //Object       value = path.getLastPathComponent();
    
        // Draw icons if not a leaf and either hasn't been loaded,
        // or the model child count is > 0.
        //if (!isLeaf && (treeModel.getChildCount(value) > 0)) 
        {
            if (hoveredRow==row)
            {
                setExpandedIcon(openIconHov);
                setCollapsedIcon(closedIconHov);
            }
            else
            {
                setExpandedIcon(openIcon);
                setCollapsedIcon(closedIcon);
            }
            
            super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded,
                    hasBeenExpanded, isLeaf);
        }/*
        else {
            super.paintExpandControl(g, clipBounds, insets, bounds, path, row, isExpanded,
                    hasBeenExpanded, isLeaf);
        }*/
    }

    
    public Rectangle getPathBounds(JTree tree, TreePath path) {
    if(tree != null && treeState != null) {
        Insets           i = tree.getInsets();
        Rectangle        bounds = treeState.getBounds(path, null);

        if(bounds != null && i != null) {
        bounds.x += i.left;
        bounds.y += i.top;
        // we use the full row width instead of only the label bounds
        bounds.width = tree.getWidth()-i.right-bounds.x;
        }
        return bounds;
    }
    return null;
    }
    
    public TreePath getClosestPathForLocation(JTree tree, int x, int y)
    {   
        return super.getClosestPathForLocation(tree, x, y);
    }
    protected void selectPathForEvent(TreePath path, MouseEvent event) {
    	if (getSelectionModel().isPathSelected(path) && event.isPopupTrigger()) {
    		// we don't want to deselect with right clicking
    		return;
    	}
    	super.selectPathForEvent(path, event);
    }
    
    int hoveredRow = -1;
    int hovx = 0;
    int hovy = 0;
    
    private static class ExpandControlHoverEffect
        implements MouseMotionListener, MouseListener
    {
        public void mousePressed(MouseEvent e)
        {
        	forwardMouseEvent(e);
            
            Component c = e.getComponent();
            if (!(c instanceof JTree)) return;
            JTree tree = (JTree) c;
            TreeUI treeUI = tree.getUI();
            
            // XXX this is not portable to macosx i guess, what's the point of this exactly
            if (SwingUtilities.isRightMouseButton(e) && treeUI != null)
            {
                // selection also by right click 
                TreePath tp = treeUI.getClosestPathForLocation(tree, e.getX(), e.getY());
                if (tp != null) {
                	tree.addSelectionPath(tp);
                }
                if (!tree.hasFocus())
                    tree.requestFocus();
            }
            
            // event redirection
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount()==2)
            {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null)
                {
                    Object tnode = path.getLastPathComponent();
                    if (tnode instanceof ETreeNode)
                    {
                        ETreeNode etn = (ETreeNode) tnode;
                        ContextEvent event = new ContextEvent((ExplorerTree)tree, ExplorerTree.ACTION_OPEN, etn);
                        etn.processEvent(event);
                    }
                }
            }
            
            // selection fix
            if (tree.getPathForLocation(e.getX(), e.getY())==null)
            {
                TreePath tp = tree.getUI().getClosestPathForLocation(tree, e.getX(), e.getY());
                if (tp!=null)
                {
                    Rectangle bounds = tree.getPathBounds(tp);
                    if (e.getY()<bounds.y+bounds.height)
                    {
                    	tree.setSelectionPath(tp);

                        if (e.getClickCount()>=2)
                        {
                            if (tree.isExpanded(tp))
                                tree.collapsePath(tp);
                            else
                                tree.expandPath(tp);
                        }
                    }
                }
            }
            
        }
        
        // ExpandControlHoverEffect
        public void mouseMoved(MouseEvent e)
        {
            Component c = e.getComponent();
            if (!(c instanceof JTree)) return;
            JTree tree = (JTree) c;
            TreeUI treeUI = tree.getUI();
            if (!(treeUI instanceof ExplorerTreeUI)) return;
            ExplorerTreeUI etUI = (ExplorerTreeUI) treeUI;
            
            int lastRow = etUI.hoveredRow;
            TreePath tp = 
                etUI.getClosestPathForLocation(tree, e.getX(), e.getY());
            if (tp == null)
                return ;
            
            int row = tree.getUI().getRowForPath(tree,tp);
            if (etUI.isLocationInExpandControl(
                    row, tp.getPathCount()-1, e.getX(), e.getY()
            ))
            {
                etUI.hoveredRow = row;
            }
            else
            {
                etUI.hoveredRow=-1;
            }

            if (etUI.hoveredRow!=lastRow)
            {
                tree.repaint(etUI.hovx-15, etUI.hovy-15,30,30);
                tree.repaint(e.getX()-15, e.getY()-15,30,30);

                etUI.hovx = e.getX();
                etUI.hovy = e.getY();
            }
        }

        public void mouseDragged(MouseEvent e)
        {
            // TODO Auto-generated method stub
            
        }

        public void mouseClicked(MouseEvent e)
        {
            forwardMouseEvent(e);
        }

        public void mouseReleased(MouseEvent e)
        {
            forwardMouseEvent(e);
        }

        public void mouseEntered(MouseEvent e)
        {
            forwardMouseEvent(e);
        }

        public void mouseExited(MouseEvent e)
        {
            forwardMouseEvent(e);
        }

        public void forwardMouseEvent(MouseEvent e)
        {
            Component c = e.getComponent();
            if (!(c instanceof ExplorerTree))
                return;
            
            ExplorerTree tree = (ExplorerTree) c;
            TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
            if (tp == null)
                return;
            Object o = tp.getLastPathComponent();
            
            if (o instanceof ETreeNode)
            {
                 ((ETreeNode)o).processEvent(e);
            }
        }
    }
    

}
