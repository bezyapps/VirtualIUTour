package com.rea.learn;

import android.util.Log;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTrackerKltPyramid;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_I16;

/**
 * Created by ericbhatti on 10/14/15.
 * <p/>
 * <p/>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p/>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p/>
 *         Jira Ticket: NULL
 * @since 14 October, 2015
 */


/**
 * Constructor which specified the KLT track manager and how the image pyramids are computed.
 *
 * @param config         KLT tracker configuration
 * @param templateRadius
 * @param pyramid
 * @param detector
 * @param gradient       Computes gradient image pyramid.
 * @param interpInput
 * @param interpDeriv
 * @param derivType
 */

public class VirtualTourPointTracker<I extends ImageSingleBand,D extends ImageSingleBand> extends PointTrackerKltPyramid {


    // list of corners which should be ignored by the corner detector
    private QueueCorner excludeList = new QueueCorner(10);

    // number of features tracked so far
    private long totalFeatures = 0;
    QueueCorner found ;

    public VirtualTourPointTracker(KltConfig config, int templateRadius, PyramidDiscrete pyramid, ImageGradient gradient,
                                   InterpolateRectangle interpInput, InterpolateRectangle interpDeriv, Class derivType) {
        super(config, templateRadius, pyramid, gradient, interpInput, interpDeriv, derivType);
        this.derivType = ImageFloat32.class;
        found = new QueueCorner();
    }




  /*  public VirtualTourPointTracker(KltConfig config, int templateRadius, PyramidDiscrete p, ImageGradient g,
                                   InterpolateRectangle i, InterpolateRectangle r, Class derivType) {

        PkltConfig pkltConfig = new PkltConfig();
        pkltConfig.templateRadius = 3;
        pkltConfig.pyramidScaling = new int[]{1,2,4,8};
        InterpolateRectangle<ImageFloat32> interpInput = FactoryInterpolation.<ImageFloat32>bilinearRectangle(ImageFloat32.class);
        InterpolateRectangle<ImageFloat32> interpDeriv = FactoryInterpolation.<ImageFloat32>bilinearRectangle(ImageFloat32.class);

        ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel(ImageFloat32.class, ImageFloat32.class);

        PyramidDiscrete<ImageFloat32> pyramid = FactoryPyramid.discreteGaussian(pkltConfig.pyramidScaling, -1, 2, true, ImageFloat32.class);

    }
*/


    public void spawnTracks(QueueCorner found ) {
        spawned.clear();

        // used to convert it from the scale of the bottom layer into the original image
        float scaleBottom = (float) basePyramid.getScale(0);

        /*// exclude active tracks
        excludeList.reset();
        for (int i = 0; i < active.size(); i++) {
            PyramidKltFeature f = (PyramidKltFeature) active.get(i);
            excludeList.add((int) (f.x / scaleBottom), (int) (f.y / scaleBottom));
        }*/



        // extract the features


        // grow the number of tracks if needed
        while( unused.size() < found.size() )
            addTrackToUnused();

        for (int i = 0; i < found.size() && !unused.isEmpty(); i++) {
            Point2D_I16 pt = found.get(i);

            // set up pyramid description
            PyramidKltFeature t = (PyramidKltFeature) unused.remove(unused.size() - 1);
            t.x = pt.x * scaleBottom;
            t.y = pt.y * scaleBottom;

            tracker.setDescription(t);

            // set up point description
            PointTrack p = t.getCookie();
            p.set(t.x,t.y);

            if( checkValidSpawn(p) ) {
                p.featureId = totalFeatures++;

                // add to appropriate lists
                active.add(t);
                spawned.add(t);
            } else {
                unused.add(t);
            }
        }
    }


    @Override
    public void spawnTracks() {
        spawned.clear();

        // used to convert it from the scale of the bottom layer into the original image
        float scaleBottom = (float) basePyramid.getScale(0);

        /*// exclude active tracks
        excludeList.reset();
        for (int i = 0; i < active.size(); i++) {
            PyramidKltFeature f = (PyramidKltFeature) active.get(i);
            excludeList.add((int) (f.x / scaleBottom), (int) (f.y / scaleBottom));
        }*/



        // extract the features

        Log.e("VIU: Unused:" , String.valueOf(unused.size()));
        Log.e("VIU: Active:" , String.valueOf(active.size()));
        Log.e("VIU: Spawned:" , String.valueOf(spawned.size()));
        Log.e("VIU: Dropped:" , String.valueOf(dropped.size()));


        // grow the number of tracks if needed
        while( unused.size() < found.size() )
            addTrackToUnused();

        for (int i = 0; i < found.size() && !unused.isEmpty(); i++) {
            Point2D_I16 pt = found.get(i);

            // set up pyramid description
            PyramidKltFeature t = (PyramidKltFeature) unused.remove(unused.size() - 1);
            t.x = pt.x * scaleBottom;
            t.y = pt.y * scaleBottom;

            tracker.setDescription(t);

            // set up point description
            PointTrack p = t.getCookie();
            p.set(t.x,t.y);

            if( checkValidSpawn(p) ) {
                p.featureId = totalFeatures++;

                // add to appropriate lists
                active.add(t);
                spawned.add(t);
            } else {
                unused.add(t);
            }
        }
    }

    private void addTrackToUnused() {
        int numLayers = basePyramid.getNumLayers();
        PyramidKltFeature t = new PyramidKltFeature(numLayers, templateRadius);

        PointTrack p = new PointTrack();
        p.setDescription(t);
        t.cookie = p;

        unused.add(t);
    }

}
