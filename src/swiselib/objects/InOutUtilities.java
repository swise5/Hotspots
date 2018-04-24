package swiselib.objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import com.vividsolutions.jts.geom.Coordinate;

import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.field.geo.GeomGridField.GridDataType;
import sim.io.geo.ArcInfoASCGridImporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;

public class InOutUtilities {
	
	public static void readInVectorLayer(GeomVectorField layer, String filename, String layerDescription, Bag attributes){
		try {
			System.out.print("Reading in " + layerDescription + "...");
			File file = new File(filename);
			if(attributes == null || attributes.size() == 0)
				ShapeFileImporter.read(file.toURL(), layer);
			else
				ShapeFileImporter.read(file.toURL(), layer, attributes);
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void readInRasterLayer(GeomGridField layer, String filename, String layerDescription, GridDataType type){
		try {
			System.out.print("Reading in " + layerDescription + "...");
			FileInputStream fstream = new FileInputStream(filename);	
			ArcInfoASCGridImporter.read(fstream, type, layer);
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Coordinate readCoordinateFromFile(String s){
		if(s.equals("")) 
			return null;
		
		
		String [] bits = s.split(" ");
		Double x = Double.parseDouble( bits[1].substring(1) );
		Double y = Double.parseDouble(bits[2].substring(0, bits[2].length() - 2));
		return new Coordinate(x,y);
	}
}