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

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwindx.examples.GazetteerPanel;
import gov.nasa.worldwindx.examples.LayerPanel;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.hu_berlin.informatik.spws2014.ImagePositionLocator.GpsPoint;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.IPLSettingsContainer;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.LDMIOEmpty;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.LDMIOTrack;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.LocationDataManager;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.Marker;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.Point2D;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.NoGpsDataAvailableException;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.PointNotInImageBoundsException;
import de.hu_berlin.informatik.spws2014.ImagePositionLocator.TriangleImagePositionLocator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;

public class testloc_world {
	private static class TestlocWorldFrame extends JFrame {
		private static final long serialVersionUID = 1L;
		
		static int BASE_MAX_IMAGE_SIZE = 50;
    	
    	//IO Data
		DefaultListModel<String> trackNames;
    	String mapDirPath;
    	int currMapIndex;
    	
    	//UI Data
    	int mapImageGeoWidth;
    	int mapImageGeoHeight;
    	SurfaceImage mapSurfaceImage;
    	RenderableLayer mapRenderLayer;
    	MapPanel globeMapPanel;
    	MapPanel mapMapPanel;
    	DisplayAdapter globeDisplayAdapter;
    	DisplayAdapter mapDisplayAdapter;
    	BufferedImage mapBufferedImage;
    	JList<String> mapSelectionList;
    	JTextField textFieldStepsize;
    	JButton btnStepToMarker;
    	JButton btnStep;
    	JSlider slider;
    	JTextField textFieldDistFallofExp; 
    	JTextField textFieldTriDissimilarity;
    	JTextField textFieldMinTriAngle;
    	JTextField textFieldBadTriPen;
    	JButton btnDisplayTriangles; 
    	JButton btnRefresh;
    	JToggleButton tglbtnMouseMode;
    	
    	//Simulation Data
    	int dbgPosIndex;
    	int dbgMarkerIndex;
    	LocationDataManager locDataManager;
    	LDMIOTrack trackLDMIO;
        
    	/**
    	 * Finds all .track & .jpg files with the same filename
    	 * in the working directory and saves them to tracknames
    	 */
        private void initFilelist() {
        	trackNames = new DefaultListModel<String>();
        	
    		File f = new File(mapDirPath);
    		File[] files = f.listFiles();

    		for (File file : files) {
    			if (file.isFile() && file.getName().endsWith(".track")) {
    				String projname = (String) file.getName().subSequence(0, file.getName().indexOf(".track"));
    				if (new File(mapDirPath + projname + ".jpg").exists())
    					trackNames.addElement(projname);
    			}
    		}
        }
        
        /**
         * Inserts the dbgPosIndex's value in trackLDMIO
         * into locDataManager, changes slider,
         * globeDisplayAdapter and increments dbgPosIndex
         * @return if the current step was the last currently available
         */
		private boolean debugStep() {
			if (dbgPosIndex == trackLDMIO.getAllGpsPoints().size())
				return true;
			
			GpsPoint newp = trackLDMIO.getAllGpsPoints().get(dbgPosIndex);
			
			locDataManager.addPoint(newp);
			slider.setValue(dbgPosIndex);
			globeDisplayAdapter.addPosition(translateToGlobePosition(newp));
			
			try {
				Marker newm = trackLDMIO.getAllMarkers().get(dbgMarkerIndex);
				
				if (newm.time <= newp.time) {
					try {
						Marker newmarker = locDataManager.addMarker(newm.imgpoint, newm.time);
						globeDisplayAdapter.addMarker(translateToGlobePosition(newmarker.realpoint));
						mapDisplayAdapter.addMarker(translateToImagePosition(newmarker.imgpoint));
						dbgMarkerIndex++;
					} catch (NoGpsDataAvailableException
							| PointNotInImageBoundsException e) {
						e.printStackTrace();
					}
				}
			} catch (IndexOutOfBoundsException e) {
				System.err.println("debugStep called but last marker was already read");
			}
			
			dbgPosIndex++;
			
			return false;
		}

		/**
		 * Calls debugStep up to n times,
		 * if possible
		 * @param n
		 */
		private void nDebugSteps(int n) {
			for (int i = 0; i < n; i++)
				debugStep();
		}
		
		/**
		 * Resets the simulation to desiredIndex
		 * @param desiredIndex
		 */
		private void resetToIndex(int desiredIndex) {
			if (desiredIndex <= trackLDMIO.getAllGpsPoints().size()) {
				if (desiredIndex <= dbgPosIndex) {
					System.err.println("Visited");
					initDebugRun();
				}
				nDebugSteps(desiredIndex - dbgPosIndex);
			}
		}
		
		/**
		 * Resets the simulation to the beginning
		 * Prints CSV's of loaded position data 
		 */
    	private void initDebugRun() {
    		dbgPosIndex = 0;
    		dbgMarkerIndex = 0;
    		slider.setValue(0);
    		globeDisplayAdapter.clear();
    		mapDisplayAdapter.clear();
    		
    		Point2D imageSize = new Point2D(mapBufferedImage.getWidth(), mapBufferedImage.getHeight()); 
    		locDataManager = new LocationDataManager(positionUpdateHandler,
    				new LDMIOEmpty(),
    				imageSize,
    				new TriangleImagePositionLocator(imageSize,
    						new IPLSettingsContainer(
    								Double.parseDouble(textFieldTriDissimilarity.getText()),
    								Double.parseDouble(textFieldDistFallofExp.getText()),
    								Double.parseDouble(textFieldBadTriPen.getText()),
    								Double.parseDouble(textFieldMinTriAngle.getText()),
    								true
    								)
    				)
    		);
    		
    		locDataManager.setSpeedFiltering(false);
    		
    		trackLDMIO.printValuesAsCSV(System.out);
    	}
    	
    	/**
    	 * Loads the image at imageuri into the mapMapPanel
    	 * @param imageuri URI to .jpg file
    	 * @return If loading was successful
    	 */
        private boolean loadNewImage(String imageuri) {
            if (mapSurfaceImage != null) {
            	mapRenderLayer.removeAllRenderables();
            }
            
			try {
				mapBufferedImage = ImageIO.read(new URL(imageuri));
			} catch (IOException e) {
				return false;
			}
			
			int max = (mapBufferedImage.getHeight() > mapBufferedImage.getWidth())
					? mapBufferedImage.getHeight() : mapBufferedImage.getWidth();
			mapImageGeoHeight = BASE_MAX_IMAGE_SIZE * mapBufferedImage.getWidth() / max;
			mapImageGeoWidth = BASE_MAX_IMAGE_SIZE * mapBufferedImage.getHeight()/ max;

			mapSurfaceImage = new SurfaceImage(mapBufferedImage, Sector.fromDegrees(0, mapImageGeoWidth, 0, mapImageGeoHeight));
			Polyline boundary = new Polyline(mapSurfaceImage.getCorners(), 0);
            boundary.setFollowTerrain(true);
            boundary.setClosed(true);
            boundary.setPathType(Polyline.RHUMB_LINE);
            boundary.setColor(new Color(0, 255, 0));
            
            mapRenderLayer.addRenderable(mapSurfaceImage);
            mapRenderLayer.addRenderable(boundary);
            mapMapPanel.wwd.redraw();
            return true;
        }
		
        /**
         * Loads .track file from newTrackFile to trackLDMIO,
         * inits debug run, zooms to newest GpsPoint
         * @param newTrackFile URI to track file
         * @return if loading the trackfile was successful
         */
    	private boolean loadNewData(String newTrackFile) {
    		LDMIOTrack tmpTrackLDMIO;
    		try {
    			tmpTrackLDMIO = new LDMIOTrack(newTrackFile);
			} catch (IOException e) {
				return false;
			}
    		
    		save();
    		trackLDMIO = tmpTrackLDMIO;
    		
    		enableSettingsMode(true);
    		initDebugRun();
    		
    		if (trackLDMIO.getAllGpsPoints().size() > 0) {
	            View view = globeMapPanel.wwd.getView();
	            view.goTo(new Position(translateToGlobePosition(trackLDMIO.getLastGpsPoint()), 0), 4000);
    		}
    		
    		slider.setMaximum((trackLDMIO.getAllGpsPoints() == null) ? 0 : trackLDMIO.getAllGpsPoints().size());
    		setTitle("testloc.world + " + newTrackFile);
			return true;
    	}
    	
		private void save() {
			if (trackLDMIO != null) {
				trackLDMIO.save();
			}
		}
		
		private void addGpsPoint(GpsPoint newpoint) {
			trackLDMIO.addGpsPoint(newpoint);
			slider.setMaximum(slider.getMaximum()+1);
			debugStep();
		}
    	
    	private Position translateToImagePosition(Point2D inp) {
    		double newx = ((double) (inp.x * mapImageGeoWidth)) / mapBufferedImage.getWidth();
    		double newy = ((double) (inp.y * mapImageGeoHeight)) / mapBufferedImage.getHeight();
    		
    		Position tmp = Position.fromDegrees(newy, newx);
    		return tmp;
    	}
    	
    	private Position translateToGlobePosition(GpsPoint inp) {
    		return Position.fromDegrees(inp.latitude, inp.longitude);
    	}
    	
    	private Point2D translateToPoint2D(Position inp) {
    		double newx = inp.getLongitude().degrees / mapImageGeoWidth * mapBufferedImage.getWidth();
    		double newy = inp.getLatitude().degrees / mapImageGeoHeight * mapBufferedImage.getHeight();
    		
    		return new Point2D(newx, newy);
    	}
    	
    	/**
    	 * Enable or disable various UI elements
    	 * @param enable
    	 */
        private void enableSettingsMode(boolean enable) {
        	textFieldStepsize.setEnabled(enable);
    		btnStepToMarker.setEnabled(enable);
    		btnStep.setEnabled(enable);
    		slider.setEnabled(enable);
    		textFieldDistFallofExp.setEnabled(enable);
    		textFieldTriDissimilarity.setEnabled(enable);
    		textFieldMinTriAngle.setEnabled(enable);
    		textFieldBadTriPen.setEnabled(enable);
    		btnDisplayTriangles.setEnabled(enable);
    		btnRefresh.setEnabled(enable);
    		tglbtnMouseMode.setEnabled(enable);
        }
    	
		MouseAdapter parameterRefreshListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				resetToIndex(dbgPosIndex);
			}
		};
		
		MouseAdapter notImplementedListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JOptionPane.showMessageDialog(null, "Currently not implemented!");
				initDebugRun();
			}
		};
    	
		MouseAdapter toggleAction = new MouseAdapter() {
			int resumeValue;
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tglbtnMouseMode.isSelected()) {
					tglbtnMouseMode.setText("Mode: Draw");
					globeDisplayAdapter.setArmed(true);
					mapDisplayAdapter.setArmed(true);
					mapSelectionList.setEnabled(false);
					slider.setEnabled(false);
					btnStep.setEnabled(false);
					btnStepToMarker.setEnabled(false);
					
					resumeValue = -1;
					int diff = slider.getMaximum() - dbgPosIndex;
					if (diff != 0) {
						resumeValue = dbgPosIndex;
						nDebugSteps(diff);
					}
				} else {
					tglbtnMouseMode.setText("Mode: Move");
					globeDisplayAdapter.setArmed(false);
					mapDisplayAdapter.setArmed(false);
					mapSelectionList.setEnabled(true);
					slider.setEnabled(true);
					btnStep.setEnabled(true);
					btnStepToMarker.setEnabled(true);
					
					if (resumeValue != -1)
						resetToIndex(resumeValue);
				}
			}
		};
    	
		MouseAdapter stepListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				nDebugSteps(Integer.parseInt(textFieldStepsize.getText()));
			}
		};
		
		MouseAdapter sliderListener = new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!slider.getValueIsAdjusting() && slider.isEnabled()) {
					resetToIndex(slider.getValue());
				}
			}
		};
		
		ListSelectionListener listListener = new ListSelectionListener() {
			@SuppressWarnings("unchecked")
			public void valueChanged(ListSelectionEvent listSelectionEvent) {
				boolean adjust = listSelectionEvent.getValueIsAdjusting();
				if (!adjust) {
					JList<String> tmplist = (JList<String>) listSelectionEvent.getSource();
					currMapIndex = tmplist.getSelectedIndex();

					if (!loadNewImage("file://" + mapDirPath + trackNames.get(currMapIndex) + ".jpg")
							|| !loadNewData(mapDirPath + trackNames.get(currMapIndex) + ".track")) {
						JOptionPane.showMessageDialog(null, "Could not load .track or image.");
						return;
					}
				}
			}
		};
		
		MouseAdapter newFileListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				String testName = JOptionPane.showInputDialog(null, "New Test Name:", null);
				String imageUrl = JOptionPane.showInputDialog(null, "Full Image URL:", null);
				
				if (trackNames.contains(testName)) {
					JOptionPane.showMessageDialog(null, "Please choose a different filename");
					return;
				}
				
				try {
					ImageIO.write(ImageIO.read(new URL(imageUrl)), "jpg", new File(mapDirPath + testName + ".jpg"));
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(null, "Could not load image");
					e1.printStackTrace();
					return;
				}
				
				trackNames.addElement(testName);
				mapSelectionList.setSelectedIndex(trackNames.getSize() - 1);
			}
		};
		
		MouseAdapter saveFileListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				save();
			}
		};
		
		Callable<Void> pointBuilderListener = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				long nexttime = (trackLDMIO.getAllGpsPoints().size() == 0) ?
						1000 : trackLDMIO.getLastGpsPoint().time + 1000;
				GpsPoint newpoint = new GpsPoint(globeDisplayAdapter.lastPos.getLongitude().degrees, globeDisplayAdapter.lastPos.getLatitude().degrees, nexttime);
				addGpsPoint(newpoint);
				
	            return null;
			}
        };      
        
        Callable<Void> markerBuilderListener = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				GpsPoint tmp = trackLDMIO.getLastGpsPoint();
				Point2D newpoint = translateToPoint2D(mapDisplayAdapter.lastPos);
				trackLDMIO.addMarker(new Marker(newpoint, tmp.time + 500, tmp));
				
				tmp.time += 500;
				addGpsPoint(tmp);
				
	            return null;
			}
        };
        
    	Callable<Void> positionUpdateHandler = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
	    		Point2D p = locDataManager.getLastImagePoint();

	    		if (p != null)
	    			mapDisplayAdapter.addPosition(translateToImagePosition(p));
				return null;
			}
		};
		
        public TestlocWorldFrame(String wdPath)
        {
        	mapDirPath = wdPath;
        	initFilelist();
        	
            Model roundModel = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            globeMapPanel = new MapPanel(roundModel, true);
            roundModel.getLayers().getLayerByName("Bing Imagery").setEnabled(true);
            LayerPanel layerPanel = new LayerPanel(globeMapPanel.wwd);
            globeDisplayAdapter = new DisplayAdapter(globeMapPanel.wwd, pointBuilderListener, MouseEvent.BUTTON1, 5, true);
            
            mapRenderLayer = new RenderableLayer();
            mapRenderLayer.setName("Surface Images");
            mapRenderLayer.setPickEnabled(false);
            LayerList layers = new LayerList();
            layers.add(mapRenderLayer);
            
            Model flatModel = new BasicModel(new EarthFlat(), layers);
            ((EarthFlat) flatModel.getGlobe()).setProjection(FlatGlobe.PROJECTION_LAT_LON);
            mapMapPanel = new MapPanel(flatModel, false);
            mapDisplayAdapter = new DisplayAdapter(mapMapPanel.wwd, markerBuilderListener, MouseEvent.BUTTON3, 50000, false);
            
        	try {
				globeMapPanel.add(new GazetteerPanel(globeMapPanel.wwd, null), BorderLayout.NORTH);
			} catch (IllegalAccessException
					| InstantiationException
					| ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
            
			mapSelectionList = new JList<String>(trackNames);
			mapSelectionList.addListSelectionListener(listListener);
            
            JLabel lblStepsize = new JLabel("Stepsize:");
            textFieldStepsize = new JTextField();
    		textFieldStepsize.setText("5");
    		
    		btnStep = new JButton("Step");
    		btnStep.addMouseListener(stepListener);

    		btnStepToMarker = new JButton("to Marker");
    		btnStepToMarker.addMouseListener(notImplementedListener);
    		
    		slider = new JSlider();
    		slider.setValue(0);
    		slider.addMouseListener(sliderListener);
        	
    		JLabel lblDstFallofExp = new JLabel("Distance Fallof:");
    		textFieldDistFallofExp = new JTextField();
    		textFieldDistFallofExp.setText(String.valueOf(7));
    		
    		JLabel lblTriDissimilarity = new JLabel("Tri Dissimilarity:");
    		textFieldTriDissimilarity = new JTextField();
    		textFieldTriDissimilarity.setText(String.valueOf(0.50));
    		
    		JLabel lblMinTriAngle = new JLabel("Min Tri Angle:");
    		textFieldMinTriAngle = new JTextField();
    		textFieldMinTriAngle.setText(String.valueOf(4.2));
    		
    		JLabel lblBadTriPen = new JLabel("Bad Tri Penalty:");
    		textFieldBadTriPen = new JTextField();
    		textFieldBadTriPen.setText(String.valueOf(0.10));
    		
    		btnDisplayTriangles = new JButton("Display Triangles");
    		btnDisplayTriangles.addMouseListener(notImplementedListener);
    		
    		btnRefresh = new JButton("Refresh");
    		btnRefresh.addMouseListener(parameterRefreshListener);
    		
    		tglbtnMouseMode = new JToggleButton("Mode: Move");
    		tglbtnMouseMode.addMouseListener(toggleAction);
    		
    		JButton btnNewFile = new JButton("New File");
    		btnNewFile.addMouseListener(newFileListener);
    		
    		JButton btnSaveFile = new JButton("Save File");
    		btnSaveFile.addMouseListener(saveFileListener);
    		
    		//Window Builder generated stuff
    		JPanel settings = new JPanel();
    		GroupLayout groupLayout = new GroupLayout(settings);
    		groupLayout.setHorizontalGroup(
    			groupLayout.createParallelGroup(Alignment.TRAILING)
    				.addGroup(groupLayout.createSequentialGroup()
    					.addContainerGap()
    					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
    						.addGroup(groupLayout.createSequentialGroup()
    							.addComponent(lblStepsize)
    							.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
    								.addGroup(groupLayout.createSequentialGroup()
    									.addGap(421)
    									.addComponent(lblTriDissimilarity)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(textFieldTriDissimilarity, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(lblBadTriPen)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(textFieldBadTriPen, GroupLayout.PREFERRED_SIZE, 34, GroupLayout.PREFERRED_SIZE)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(btnRefresh)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(btnNewFile)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(btnSaveFile))
    								.addGroup(groupLayout.createSequentialGroup()
    									.addGap(12)
    									.addComponent(textFieldStepsize, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(lblDstFallofExp)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(textFieldDistFallofExp, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(lblMinTriAngle)
    									.addPreferredGap(ComponentPlacement.RELATED)
    									.addComponent(textFieldMinTriAngle, GroupLayout.PREFERRED_SIZE, 42, GroupLayout.PREFERRED_SIZE))))
    						.addGroup(groupLayout.createSequentialGroup()
    							.addComponent(btnStep, GroupLayout.PREFERRED_SIZE, 67, GroupLayout.PREFERRED_SIZE)
    							.addPreferredGap(ComponentPlacement.RELATED)
    							.addComponent(btnStepToMarker)
    							.addPreferredGap(ComponentPlacement.RELATED)
    							.addComponent(tglbtnMouseMode, GroupLayout.PREFERRED_SIZE, 128, GroupLayout.PREFERRED_SIZE)
    							.addPreferredGap(ComponentPlacement.RELATED)
    							.addComponent(btnDisplayTriangles, GroupLayout.PREFERRED_SIZE, 161, GroupLayout.PREFERRED_SIZE)
    							.addPreferredGap(ComponentPlacement.RELATED)
    							.addComponent(slider, GroupLayout.DEFAULT_SIZE, 1062, Short.MAX_VALUE)))
    					.addContainerGap())
    		);
    		groupLayout.setVerticalGroup(
    			groupLayout.createParallelGroup(Alignment.LEADING)
    				.addGroup(groupLayout.createSequentialGroup()
    					.addContainerGap()
    					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
    						.addComponent(lblStepsize)
    						.addComponent(textFieldStepsize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    						.addComponent(lblDstFallofExp)
    						.addComponent(textFieldDistFallofExp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    						.addComponent(lblMinTriAngle)
    						.addComponent(textFieldMinTriAngle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    						.addComponent(lblTriDissimilarity)
    						.addComponent(textFieldTriDissimilarity, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    						.addComponent(lblBadTriPen)
    						.addComponent(textFieldBadTriPen, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
    						.addComponent(btnRefresh)
    						.addComponent(btnNewFile)
    						.addComponent(btnSaveFile))
    					.addPreferredGap(ComponentPlacement.RELATED)
    					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
    						.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
    							.addComponent(btnStep)
    							.addComponent(btnStepToMarker)
    							.addComponent(tglbtnMouseMode)
    							.addComponent(btnDisplayTriangles))
    						.addComponent(slider, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
    		);
    		settings.setLayout(groupLayout);
            
            JSplitPane globeSplit = new JSplitPane();
            globeSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            globeSplit.setLeftComponent(layerPanel);
            globeSplit.setRightComponent(globeMapPanel);
            globeSplit.setOneTouchExpandable(true);
            globeSplit.setContinuousLayout(true);
            globeSplit.setDividerLocation(0);

            JSplitPane imgSplit = new JSplitPane();
            imgSplit.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            imgSplit.setLeftComponent(mapMapPanel);
            imgSplit.setRightComponent(mapSelectionList);
            imgSplit.setOneTouchExpandable(true);
            imgSplit.setContinuousLayout(true);
            
            JSplitPane layer2Split = new JSplitPane();
            layer2Split.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            layer2Split.setLeftComponent(globeSplit);
            layer2Split.setRightComponent(imgSplit);
            layer2Split.setOneTouchExpandable(true);
            layer2Split.setContinuousLayout(true);
            layer2Split.setResizeWeight(0.5);
    		
            this.add(settings, BorderLayout.NORTH);
            this.add(layer2Split, BorderLayout.CENTER);
            this.pack();
            
            this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            
            enableSettingsMode(false);
        }
    }
    
    public static void main(String[] args)
    {
    	if (args.length != 1) {
			System.out.println("Provide track folder path!");
			return;
		}
    	String rootDirPath = (args[0].endsWith("/")) ? args[0] : args[0] + "/";
    	
    	System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    	
    	final TestlocWorldFrame frame = new TestlocWorldFrame(rootDirPath);
        frame.setTitle("testloc.world");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                frame.setVisible(true);
            }
        });
    }
}