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

import java.awt.Event;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;

public interface ETreeNode extends TreeNode
{

    Icon getIcon();
    void notifyDropChildren();
    
    public void processEvent(Event event);
    
    String getToolTipText();
    void processEvent(MouseEvent e);
    boolean isActionCommandPossible(ExplorerTree tree, String command);
    void actionCommandPerformed(ExplorerTree tree, String command);
    
}
