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
 * Created on Jun 22, 2006
 */
package net.sf.nmedit.nomad.main.ui.sidebar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.nmedit.nomad.core.application.Application;
import net.sf.nmedit.nomad.main.Nomad;
import net.sf.nmedit.nomad.main.resources.AppIcons;
import net.sf.nmedit.nomad.main.ui.fix.StripeEnabledListCellRenderer;
import net.sf.nmedit.nomad.main.ui.fix.TreeStripes;

public class PatchLibSidebar extends JPanel implements Sidebar, SidebarListener
{

    public ImageIcon getIcon()
    {
        return AppIcons.IC_FILE_FOLDER;
    }

    public String getDescription()
    {
        return "Patch file browser";
    }

    public JComponent createView()
    {
        return this;
    }

    public void disposeView()
    {
        // nothing to do
    }

    public void sidebarActivated( SidebarEvent e )
    {
        // nothing to do
    }

    public void sidebarDeactivated( SidebarEvent e )
    {
        setPreferredSize(getSize());
    }
    
    private Nomad nomad;
    private SidebarControl sbcontrol;
    private File directory;
    private File[] files ;
    private JList listView;

    public final static String KEY_CURRENT_DIRECTORY = "custom.patch.directory.default";
    
    public static String getDefaultPatchDirectory()
    {
        String value = Application.getProperty(KEY_CURRENT_DIRECTORY);
        return value == null ? "data/patch" : value;
    }

    public static File getDefaultPatchDirectoryFile()
    {
        return (new File(getDefaultPatchDirectory())).getAbsoluteFile();
    }

    public static void setDefaultPatchDirectory( File currentDirectory )
    {
        if (currentDirectory.isDirectory())
            Application.setProperty(KEY_CURRENT_DIRECTORY, currentDirectory.getAbsolutePath());
    }
    
    public PatchLibSidebar( Nomad nomad, SidebarControl sbcontrol  )
    {
        this.nomad = nomad;
        if (nomad==null) throw new NullPointerException();
        
        this.sbcontrol = sbcontrol;
        sbcontrol.addSidebarListener(this);


        setPreferredSize(new Dimension(200,0));
        setLayout(new BorderLayout());
        listView = new JList();
        add(new JScrollPane(listView), BorderLayout.CENTER);

        listView.setModel(new PatchFileListModel());
        listView.addMouseListener(new ListMouseAction());
        listView.setCellRenderer(new PatchFileCellRenderer());
        
        setDirectory(getDefaultPatchDirectoryFile());
    }
    
    public File getDirectory()
    {
        return directory;
    }
    
    public void setDirectory(File dir)
    {
        if (dir==null) return;
        this.directory = dir;
        this.files = dir.listFiles(new PatchFileFilter());
        Arrays.<File>sort(files, new FileComparator());
        
        // TODO how to update list ???
        //listView.setModel(listView.getModel());
        listView.repaint();
        listView.revalidate();
    }
    
    private class FileComparator implements Comparator<File>
    {

        public int compare( File a, File b )
        {
            if (a.isDirectory() && !(b.isDirectory()))
            {
                return -1;
            }
            else if ((!a.isDirectory()) && b.isDirectory())
            {
                return 1;
            }
            else
            {
                return a.toString().compareTo(b.toString());
            }
        }
        
    }
    
    private class PatchFileFilter implements FileFilter
    {
        public boolean accept( File pathname )
        {
            return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".pch"));
        }
    }
    
    private class PatchFileListModel extends AbstractListModel 
    {

        public int getSize()
        {
            return files.length+1;
        }

        public Object getElementAt( int index )
        {
            return index==0 ? directory.getParentFile() : files[index-1];
        }
        
    }

    private class ListMouseAction extends MouseAdapter
    {     
        public void mouseClicked(MouseEvent e)
        {
            if(e.getClickCount() == 2)
            {
                int index = listView.locationToIndex(e.getPoint());
                if (index>=0 && index<listView.getModel().getSize())
                {
                    listView.ensureIndexIsVisible(index);
                    if (index == 0)
                    {
                        setDirectory(directory.getAbsoluteFile().getParentFile());
                    }
                    else
                    {
                        File file = files[index-1];
                        if (file.isDirectory())
                        {
                            setDirectory(file);
                        }
                        else
                        {
                            nomad.openPatchFiles(new File[]{file});
                        }
                    }
                }
            }
      }
    }
    
    private class PatchFileCellRenderer extends StripeEnabledListCellRenderer
    {

        public PatchFileCellRenderer( )
        {
            super( TreeStripes.AlternatingStripes.createSoftBlueStripes() );
        }

        private ImageIcon fileIcon = AppIcons.IC_DOCUMENT_NEW; // TODO use other constant
        private ImageIcon dirIcon = AppIcons.IC_FILE_FOLDER;
        
        public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus )
        {
            if (value == null)
            {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            File file = (File) value;
            String label = file.getName();
            int sep = label.lastIndexOf('/');
            if (sep<0) sep = label.lastIndexOf('\\');
            if (sep>0) label = label.substring(sep);
            /*if (label.toLowerCase().endsWith(".pch"))
                label = label.substring(0, label.length()-4);*/
            
            super.getListCellRendererComponent(list, index == 0 ? ".." : label, index, isSelected, cellHasFocus);
            
            setIcon((index == 0 || file.isDirectory())?dirIcon:fileIcon);
            
            /*if (value !=null)
            return new PatchView((Patch)value, isSelected|cellHasFocus);
            else throw new NullPointerException();*/
            
            return this;
        }
        
    }

}
