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
 * Created on Jan 21, 2007
 */
package net.sf.nmedit.jtheme.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.border.Border;

import net.sf.nmedit.jpatch.PConnection;
import net.sf.nmedit.jpatch.PConnectionManager;
import net.sf.nmedit.jpatch.PConnector;
import net.sf.nmedit.jpatch.PModule;
import net.sf.nmedit.jpatch.PModuleContainer;
import net.sf.nmedit.jpatch.PModuleMetrics;
import net.sf.nmedit.jpatch.event.PConnectionEvent;
import net.sf.nmedit.jpatch.event.PConnectionListener;
import net.sf.nmedit.jpatch.event.PModuleContainerEvent;
import net.sf.nmedit.jpatch.event.PModuleContainerListener;
import net.sf.nmedit.jtheme.JTContext;
import net.sf.nmedit.jtheme.JTException;
import net.sf.nmedit.jtheme.cable.Cable;
import net.sf.nmedit.jtheme.cable.JTCableManager;
import net.sf.nmedit.jtheme.component.plaf.mcui.JTModuleContainerUI;
import net.sf.nmedit.jtheme.store2.ModuleElement;

public class JTModuleContainer extends JTBaseComponent 
{
    
    /**
     * 
     */
    private static final long serialVersionUID = 4287282305262880549L;

    public static final String uiClassId = "ModuleContainerUI";
    
    private boolean optimizedDrawing;
    private JTPatch patchContainer;
    private PModuleContainer moduleContainer;
    private ContentSynchronisation cs;
    //private CableOverlay overlay;
    
    private JTLayerRoot cableLayer;
    
    public JTModuleContainer(JTContext context, JTCableManager cableManager)
    {
        super(context);
        setOpaque(true);
        setFocusable(true);
        optimizedDrawing = false;// 'true' does not work: !context.hasModuleContainerOverlay();
                                // we have to overwrite boolean isPaintingOrigin() which is package private

        this.cableLayer = new JTLayerRoot(context);
        add(cableLayer);
        
        setCableManager(cableManager);
        cs = createContentSynchronisation();
        if (cs != null)
            cs.install();

        setJTFlag(FLAG_INVALIDATE, true);
        setJTFlag(FLAG_VALIDATE, true);
        setJTFlag(FLAG_REVALIDATE, true);
        setJTFlag(FLAG_VALIDATE_TREE, true);
        
        setLayout(new ModuleContainerLayout());
    }
    
    protected boolean isRepaintOrigin()
    {
        return true;
    }
    
    /**
     * Returns a mutable list of all modules in this container.
     * @return modules in this container
     */
    public List<JTModule> getModules()
    {
        return getComponents(JTModule.class);
    }
    
    
    private Set<JTModule> selectionSet = new HashSet<JTModule>();

    public int getSelectionSize()
    {
    	return selectionSet.size();
    }

    public boolean isOnlyThisSelected(JTModule module)
    {
    	return getSelectionSize() == 1 && isInSelection(module);
    }

    public void selectOnly(JTModule module)
    {
        if (!isOnlyThisSelected(module))
        {
            if (!selectionSet.isEmpty())
            {
                for (JTModule m: selectionSet)
                    m.setSelected(false);
                selectionSet.clear();
            }
            addSelection(module);
        }
    }

    public void addSelection(JTModule module)
    {
    	JTPatch patch = getPatchContainer();
    	for (JTModuleContainer c : patch.getModuleContainers()) {
    		if (c != null && c != this) { 
    			c.clearSelection();
    		}
    	}
    	selectionSet.add(module);
    	module.setSelected(true);
    }

    public void removeSelection(JTModule module)
    {
    	selectionSet.remove(module);
    	module.setSelected(false);
    }

    public boolean isInSelection(JTModule module)
    {
    	return selectionSet.contains(module);
    }

    public boolean isSelectionEmpty()
    {
    	return selectionSet.isEmpty();
    }
    
    public Collection<? extends JTModule> getSelectedModules()
    {
        return new HashSet<JTModule>(selectionSet);
    }

    public Collection<? extends PModule> getSelectedPModules()
    {
    	HashSet<PModule> result = new HashSet<PModule>();
    	for (JTModule m : selectionSet) {
    		result.add(m.getModule());
    		
    	}
    	return result;
    }

    public void clearSelection()
    {
    	if (selectionSet.isEmpty())
    		return;

    	for (JTModule module: selectionSet)
    		module.setSelected(false);

    	selectionSet.clear();
    }

    private JTCableManager cableManager;

/*
    public Component findComponentAt(int x, int y) { 
        if (!(contains(x, y) && isVisible() && isEnabled())) {
            return null;
        }
        synchronized (getTreeLock())
        {
            for (int i=getComponentCount()-1;i>=0;i--)
            {
                Component comp = getComponent(i);
                if (comp != overlay)
                {
                    if (comp instanceof Container)
                    {
                        comp = ((Container)comp).findComponentAt(x-comp.getX(), y-comp.getY());
                    }
                    else
                    {
                        comp = comp.getComponentAt(x, y);
                    }
                    if (comp != null && comp.isVisible() && comp.isEnabled())
                    {
                        return comp;
                    }
                }
            }
        }
        return this;
    }*/
    
    protected ContentSynchronisation createContentSynchronisation()
    {
        return new ContentSynchronisation();
    }
    
    protected class ContentSynchronisation implements PModuleContainerListener, PConnectionListener
    {
        private PModuleContainer mc;
        private PConnectionManager pc;
        //private boolean oneUpdate = false;
        
        public void install()
        {
           // no op
        }

        public void update()
        {
            setInstalledContainer(moduleContainer);
        }

        private void setInstalledContainer(PModuleContainer moduleContainer)
        {
            if (this.mc != null)
                uninstall(this.mc);
            this.mc = moduleContainer;
            if (this.mc != null)
                install(this.mc);
        }

        protected void install(PModuleContainer mc)
        {
            mc.addModuleContainerListener(this);
            pc = mc.getConnectionManager();
            if (pc != null)
                pc.addConnectionListener(this);
        }

        protected void uninstall(PModuleContainer mc)
        {
            mc.removeModuleContainerListener(this);
            if (pc != null)
                pc.removeConnectionListener(this);
            pc = null;
        }

        public void moduleAdded(PModuleContainerEvent e)
        {
            createUIFor(e.getModule());
        }

        public void moduleRemoved(PModuleContainerEvent e)
        {
            removeUI(e.getModule());
        }

        private void createUIFor(PModule module)
        {
            ModuleElement ms = getContext().getStorageContext().getModuleStoreById(module.getComponentId());

            try
            {
                JTModule mui = ms.createModule(getContext(), module);
                mui.setLocation(module.getScreenLocation());    
                add(mui);
                JTModuleContainer.this.revalidate();
                mui.repaint();
            }
            catch (JTException e)
            {
                e.printStackTrace();
            }
        }

        public void removeUI(PModule module)
        {
            for (JTModule mui: JTModuleContainer.this.getModules())
            {
                if (mui.getModule() == module)
                {
                    int x = mui.getX();
                    int y = mui.getY();
                    int w = mui.getWidth();
                    int h = mui.getHeight();
                    
                    remove(mui);
                    JTModuleContainer.this.revalidate();
                    JTModuleContainer.this.repaint(x, y, w, h);
                    break;
                }
            }
        }

        public void connectionAdded(PConnectionEvent e)
        {
            colorCables(e, true);
        }

        public void connectionRemoved(PConnectionEvent e)
        {
            colorCables(e, false);
        }

        public void colorCables(PConnectionEvent e, boolean joined)
        {
            if (joined)
            {
                Color color = null;
                PConnector graph = null;
                if (e.getDestination().getOutputConnector() != null)
                {
                    graph = e.getSource();
                    color = e.getDestination().getOutputConnector().getSignalType().getColor();
                    
                }
                else if (e.getSource().getOutputConnector() != null)
                {
                    graph = e.getDestination();
                    color = e.getSource().getOutputConnector().getSignalType().getColor();
                }
                
                if (graph != null) 
                {
                    
                    Collection<Cable> cables = getCables(graph); 
                    for (Cable cable: cables)
                    {
                        cable.setColor(color);
                    }
                    getCableManager().update(cables);
                }
            }
            else
            {
                PConnector graph = null;
                if (e.getDestination().getOutputConnector() != null)
                {
                    graph = e.getSource();
                }
                else if (e.getSource().getOutputConnector() != null)
                {
                    graph = e.getDestination();
                }
                
                // make grey
                if (graph != null)
                {
                    Collection<Cable> cables = getCables(graph); 
                    for (Cable cable: cables)
                    {
                        // TODO lookup correct color in patch 
                        cable.setColor(Color.WHITE);
                    }
                    getCableManager().update(cables);
                }
            }
        }
        
        private Collection<Cable> getCables(PConnector c)
        {
            Collection<Cable> cables = new ArrayList<Cable>(getCableManager().size());
            Collection<PConnection> connections = c.getGraphConnections();
            
            for (Cable cable: getCableManager())
            {
                for (Iterator<PConnection> iter=connections.iterator();iter.hasNext();)
                {
                    if (isSameConnection(iter.next(), cable))
                        cables.add(cable);
                }
            }
            return cables;
        }

        private boolean isSameConnection(PConnection c, Cable cable)
        {
            return isSameConnection(c.getA(), c.getB(), cable.getSource(), cable.getDestination())
            || isSameConnection(c.getA(), c.getB(), cable.getDestination(), cable.getSource());
        }

        private boolean isSameConnection(PConnector a1, PConnector b1,
                PConnector a2, PConnector b2)
        {
            // TODO Auto-generated method stub
            return a2==a2 && b1==b2;
        }
        
    }
    
    public void setModuleContainer(PModuleContainer mc)
    {
        this.moduleContainer = mc;
        setName(moduleContainer == null ? null : moduleContainer.getName());
        PModuleMetrics metrics = null;
        if (mc != null) metrics = mc.getModuleMetrics();
        if (metrics == null) setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        else setMaximumSize(new Dimension(metrics.getMaxScreenX(), metrics.getMaxScreenY()));
        
        if (cs != null)
            cs.update();
    }

    public PModuleContainer getModuleContainer()
    {
        return moduleContainer;
    }
    
    public JTPatch getPatchContainer()
    {
        return patchContainer;
    }
    
    public void setPatchContainer(JTPatch patchContainer)
    {
        this.patchContainer = patchContainer;
    }

    public JTModuleContainerUI getUI()
    {
        return (JTModuleContainerUI) ui;
    }
    
    public void setUI(JTModuleContainerUI ui)
    {
        super.setUI(ui);
    }
    
    public String getUIClassID()
    {
        return uiClassId;
    }

    protected void setCableManager(JTCableManager cableManager)
    {
        this.cableManager = cableManager;
        cableLayer.setCableManager(cableManager);
    }
    
    public JTCableManager getCableManager()
    {
        return  cableManager;
    }
    
    public boolean isOptimizedDrawingEnabled()
    {
        return optimizedDrawing;
    }

    /**
     * Calls {@link #paint(Graphics)}.
     */
    public void update(Graphics g)
    {
        paint(g);
    }
    
    public void setBorder(Border border)
    {
        if (border != null)
            throw new UnsupportedOperationException("border not supported");
    }
    
    protected final void paintBorder(Graphics g)
    {
        // no op
    }
    
    protected void paintChildren(Graphics g)
    {
        super.paintChildren(g);
        if (ui != null)
        {
            getUI().paintChildrenHack(g);
        }
    }

    public boolean isDnDAllowed()
    {
        return getContext().isDnDAllowed();
    }
    
    private class ModuleContainerLayout implements LayoutManager2
    {
        
        private boolean valid = false;
        private Dimension cachedSize = new Dimension();

        public void invalidateLayout(Container target)
        {
            valid = false;
        }

        public void layoutContainer(Container parent)
        {
            JTModuleContainer mc = JTModuleContainer.this;
            // only layout cable layer
            JTLayerRoot cl = mc.cableLayer;
            if (cl != null)
            {
                synchronized(mc.getTreeLock())
                {
                    Insets insets = mc.getInsets();
                    cl.setBounds(0, 0, mc.getWidth()-insets.left-insets.right,
                            mc.getHeight()-insets.top-insets.bottom);
                }
            }
        }

        public Dimension minimumLayoutSize(Container parent)
        {
            if (valid) return new Dimension(cachedSize);

            Dimension dim = new Dimension(0, 0);
            JTModuleContainer mc = JTModuleContainer.this;
            synchronized (mc.getTreeLock())
            {
                Insets insets = mc.getInsets();
                dim.width += insets.left + insets.right;
                dim.height += insets.top + insets.bottom;
                for (int i=mc.getComponentCount()-1;i>=0;i--)
                {
                    Component c = mc.getComponent(i);
                    if (mc.cableLayer == c)
                        continue;

                    dim.width = Math.max(dim.width, c.getX()+c.getWidth());
                    dim.height = Math.max(dim.height, c.getY()+c.getHeight());
                }
            }
            
            cachedSize.setSize(dim);
            valid = true;
            return dim;
        }

        public Dimension maximumLayoutSize(Container target)
        {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public Dimension preferredLayoutSize(Container parent)
        {
            return minimumLayoutSize(parent);
        }

        public float getLayoutAlignmentX(Container target)
        {
            return 0;
        }

        public float getLayoutAlignmentY(Container target)
        {
            return 0;
        }

        public void addLayoutComponent(String name, Component comp)
        {
            // no op
        }

        public void addLayoutComponent(Component comp, Object constraints)
        {
            // no op
        }

        public void removeLayoutComponent(Component comp)
        {
            // no op
        }
        
    }
    
}
