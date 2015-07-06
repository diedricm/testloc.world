/* Copyright (C) 2014,2015  Maximilian Diedrich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package de.hu_berlin.informatik.spws2014.testloc_world;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwindx.examples.ClickAndGoSelectListener;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;

public class MapPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public WorldWindowGLCanvas wwd;

    public MapPanel(Model m, boolean includeStatusBar) {
        super(new BorderLayout());

        wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new Dimension(800, 600));

        setMinimumSize(new Dimension(0, 0));

        wwd.setModel(m);

        wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));
        
        add(wwd, BorderLayout.CENTER);

        if (includeStatusBar)
        {
            StatusBar statusBar = new StatusBar();
            add(statusBar, BorderLayout.PAGE_END);
            statusBar.setEventSource(wwd);
        }
    }
}