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

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Ellipsoid;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.ShapeAttributes;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.concurrent.Callable;

public class DisplayAdapter extends AVListImpl
{	
    final WorldWindow wwd;
    
    boolean armed = false;
    boolean active = false;
    
    ArrayList<Position> positions = new ArrayList<Position>();
    Polyline line;
    RenderableLayer layer;
    
    Callable<Void> drawCallback;
    int listenMouseButton;
    int markerRadius;
    
    public Position lastPos;

    MouseAdapter mouseAdapter = new MouseAdapter() {
        public void mousePressed(MouseEvent mouseEvent) {
            if (armed && mouseEvent.getButton() == listenMouseButton && !mouseEvent.isControlDown()) {
            	active = true;
                signalAddPosition();
                mouseEvent.consume();
            }
        }

        public void mouseReleased(MouseEvent mouseEvent) {
            if (armed && mouseEvent.getButton() == listenMouseButton) {
                active = false;
                mouseEvent.consume();
            }
        }

        public void mouseClicked(MouseEvent mouseEvent) {
            if (!mouseEvent.isControlDown())
                mouseEvent.consume();
        }
    };
    
    MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent mouseEvent) {
            if (armed && active) {
            	mouseEvent.consume();
            }
        }
    };
    
    PositionListener positionListener = new PositionListener()
    {
    	private final int SKIP_STPS_NUM = 1;
    	int i;
    	
        public void moved(PositionEvent event)
        {
        	if (i == SKIP_STPS_NUM + 1)
        		i = 0;
        	
            if (active && ++i == SKIP_STPS_NUM)
        		signalAddPosition();
        }
    };

    /**
     * @param inpwwd WorldWindow to operate on
     * @param callback Called whenever a input event happened
     * @param inpListenMouseButton Mouse button to listen for
     * @param markerRadius ballMarker radius size
     * @param allowLineFeedback When moving with inpListenMouseButton should callback be called
     */
    public DisplayAdapter(final WorldWindow inpwwd, Callable<Void> callback, int inpListenMouseButton, int markerRadius, boolean allowLineFeedback)
    {
        wwd = inpwwd;
        drawCallback = callback;
        listenMouseButton = inpListenMouseButton;
        this.markerRadius = markerRadius;

        line = new Polyline();
        line.setFollowTerrain(true);
        
        layer = new RenderableLayer();
        layer.addRenderable(this.line);
        wwd.getModel().getLayers().add(this.layer);

        wwd.getInputHandler().addMouseListener(mouseAdapter);
        wwd.getInputHandler().addMouseMotionListener(mouseMotionAdapter);
        
        if (allowLineFeedback)
	        wwd.addPositionListener(positionListener);
    }
    
    /**
     * Signal to host application that a input event occured
     */
    private void signalAddPosition() {
    	lastPos = wwd.getCurrentPosition();
    	if (lastPos != null) {
	    	try {
	    		drawCallback.call();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    }

    /**
     * Resets this DisplayAdapter
     */
    public void clear()
    {
    	layer.removeAllRenderables();
    	layer.firePropertyChange(AVKey.LAYER, null, null);
    	
    	positions.clear();
    	line.setPositions(positions);
        layer.addRenderable(this.line);
        
        wwd.redraw();
    }
    
    /**
     * Set whether input interception is enabled 
     */
    public void setArmed(boolean setArmed)
    {
        armed = setArmed;
    }
    
    /**
     * Adds curPos as the last Point of line
     */
    public void addPosition(Position curPos) {
    	if (curPos == null)
            return;
    	
        this.positions.add(curPos);
        this.line.setPositions(this.positions);
        this.firePropertyChange("DisplayAdapter.AddPosition", null, curPos);
        this.wwd.redraw();
    }
    
    /**
     * Draws a ballMarker at destPos
     */
    public void addMarker(Position destPos) {
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setInteriorMaterial(Material.YELLOW);
        attributes.setInteriorOpacity(0.7);
        attributes.setDrawInterior(true);
        attributes.setEnableLighting(true);
        attributes.setOutlineMaterial(Material.RED);
        attributes.setOutlineWidth(2d);
        attributes.setDrawOutline(false);
    	
        Ellipsoid ballMarker = new Ellipsoid(destPos, markerRadius, markerRadius, markerRadius);
        ballMarker.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        ballMarker.setAttributes(attributes);
        ballMarker.setVisible(true);
        layer.addRenderable(ballMarker);
        
        this.wwd.redraw();
    }
}