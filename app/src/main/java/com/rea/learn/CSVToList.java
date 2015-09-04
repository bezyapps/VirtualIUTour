package com.rea.learn;

import android.content.Context;
import android.content.res.AssetManager;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import java.util.StringTokenizer;

import org.ddogleg.struct.FastQueue;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.feature.UtilFeature;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.ImageFloat32;


public class CSVToList {

	
	String[] names;
	String basePath;
	FastQueue<SurfFeature> listSrc;
	Context context;
	DetectDescribePoint<ImageFloat32,SurfFeature > detDesc;
	public CSVToList(String basePath, String... names)
	{
		this.basePath = basePath;
		this.names = names;
		 detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageFloat32.class);
		listSrc = UtilFeature.createQueue(detDesc, 10);
		listSrc.setDeclareInstances(true);
		
	//	TupleDesc<Desc> desc = new 
	//	listHelmetSrc.add();
	}

	public CSVToList(Context context)
	{
		detDesc = CreateDetectorDescriptor.create(CreateDetectorDescriptor.DETECT_FH, CreateDetectorDescriptor.DESC_SURF, ImageFloat32.class);
		listSrc = UtilFeature.createQueue(detDesc, 10);
		listSrc.setDeclareInstances(true);
		this.context = context;
	}
	
	public void convertCSVsToList(String path) throws IOException
	{
		/*FileInputStream fileInputStream = new FileInputStream(new File(basePath + names[i] + ".csv"));
		byte[] bytes = new byte[4096];
		StringBuffer buffer = new StringBuffer();
		while(fileInputStream.read(bytes) != -1) {
			buffer.append(bytes);
		}*/
        listSrc.reset();
			AssetManager assetManager = context.getAssets();
		InputStream csvStream = assetManager.open(path);
		InputStreamReader csvStreamReader = new InputStreamReader(csvStream);
		CSVReader csvReader = new CSVReader(csvStreamReader);

			int lines = 0;
		String[] line;
		while ((line = csvReader.readNext()) != null) {
				SurfFeature feature = new SurfFeature(64);
				int count = 0;
				boolean should_add = true;
				//System.out.println("line: " + lines);
				for (String part : line) {
					if(part.equals("")) {
						should_add = false; 
						break;
					}
					float data = Float.parseFloat(part);
					feature.value[count] = data;
					count++;
				}
				if(should_add) listSrc.add(feature);
				lines++;
				
			}
		
		}

}

/*StringTokenizer stringTokenizerLines = new StringTokenizer(buffer.toString(), "\n");
while (stringTokenizerLines.hasMoreElements()) {
	String row = (String) stringTokenizerLines.nextToken();
	{
		StringTokenizer stringTokenizerRowData = new StringTokenizer(row, ",");
		Desc desc = new Desc(64);
		int count = 0;
		while (stringTokenizerRowData.hasMoreElements()) {
			String test = stringTokenizerRowData.nextToken();
			float data = Float.parseFloat(test);
			desc.value[count] = data;
			count++;
		}
		listHelmetSrc.add(desc);
	}
}*/
	

