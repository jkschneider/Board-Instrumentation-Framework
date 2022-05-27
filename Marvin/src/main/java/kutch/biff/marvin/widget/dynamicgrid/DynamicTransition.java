/*
 * ##############################################################################
 * #  Copyright (c) 2016 Intel Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * #  you may not use this file except in compliance with the License.
 * #  You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * #  Unless required by applicable law or agreed to in writing, software
 * #  distributed under the License is distributed on an "AS IS" BASIS,
 * #  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * #  See the License for the specific language governing permissions and
 * #  limitations under the License.
 * ##############################################################################
 * #    File Abstract:
 * #
 * #
 * ##############################################################################
 */
package kutch.biff.marvin.widget.dynamicgrid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import kutch.biff.marvin.logger.MarvinLogger;
import kutch.biff.marvin.utility.FrameworkNode;
import kutch.biff.marvin.widget.DynamicImageWidget;

/**
 * @author Patrick Kutch Most of this was based on
 * https://bitbucket.org/hansolo/imagerollover
 */
public class DynamicTransition {

    public enum Transition {
        VERTICAL_AROUND_X, VERTICAL_AROUND_Y, VERTICAL_AROUND_X_AND_Y, HORIZONTAL_AROUND_X, HORIZONTAL_AROUND_Y,
        HORIZONTAL_AROUND_X_AND_Y, RECTANGULAR_AROUND_X, RECTANGULAR_AROUND_Y, RECTANGULAR_AROUND_X_AND_Y,
        DISSOLVING_BLOCKS, CUBE, FLIP_HORIZONTAL, FLIP_VERTICAL, NONE, INVALID

    }

    protected static final Logger LOGGER = Logger.getLogger(MarvinLogger.class.getName());

    public static String names() {
        String strReturn = "";
        int index = 0;
        for (Transition s : Transition.values()) {
            if (index < Transition.values().length - 2) {
                strReturn += s.name() + " ";
            }
            index++;
        }

        return strReturn;
    }

    public static DynamicTransition ReadTransitionInformation(FrameworkNode baseNode) {
        DynamicTransition.Transition transitionType = DynamicTransition.Transition.NONE;
        int transitionXGridCount = 10;
        int transitionYGridCount = 10;
        int transitionDelay = 100;
        Color transitionSnapshotColorBg = Color.TRANSPARENT;
        Duration transitionDuration = Duration.millis(1500);

        for (FrameworkNode node : baseNode.getChildNodes()) {
            if ("#Text".equalsIgnoreCase(node.getNodeName()) || "#Comment".equalsIgnoreCase(node.getNodeName())) {
                continue;
            }

            if ("Transition".equalsIgnoreCase(node.getNodeName())) {
                String strTransition = node.getTextContent();
                transitionType = DynamicTransition.VerifyTransitionType(strTransition);
                if (transitionType == DynamicTransition.Transition.INVALID) {
                    LOGGER.severe("Invalid Transition [" + strTransition + "] specified.  Valid values are: "
                            + DynamicTransition.names());
                    return null;
                }

                if (node.hasAttribute("xGrids")) {
                    transitionXGridCount = node.getIntegerAttribute("xGrids", 0);
                    if (transitionXGridCount <= 0) {
                        LOGGER.severe("Invlid xGrids value for Transition: " + node.getAttribute("xGrids"));
                        return null;
                    }
                }
                if (node.hasAttribute("yGrids")) {
                    transitionYGridCount = node.getIntegerAttribute("yGrids", 0);
                    if (transitionYGridCount <= 0) {
                        LOGGER.severe("Invlid yGrids value for Transition: " + node.getAttribute("yGrids"));
                        return null;
                    }
                }
                if (node.hasAttribute("Duration")) {
                    int Transition_Duration = node.getIntegerAttribute("Duration", 0);
                    if (Transition_Duration <= 0) {
                        LOGGER.severe("Invlid Duration value for Transition: " + node.getAttribute("Duration"));
                        return null;
                    }
                    transitionDuration = Duration.millis(Transition_Duration);
                }
                if (node.hasAttribute("Delay")) {
                    transitionDelay = node.getIntegerAttribute("Delay", 0);
                    if (transitionDelay <= 0) {
                        LOGGER.severe("Invlid Delay value for Transition: " + node.getAttribute("Delay"));
                        return null;
                    }
                }
                // nuking this, s not really needed anymore
//                if (node.hasAttribute("Background"))
//                {
//                    String strColor = node.getAttribute("Background");
//                    try
//                    {
//                        _Transition_Snapshot_Color_bg = Color.web(strColor);
//                    }
//                    catch (Exception e)
//                    {
//                        LOGGER.severe("Invalid Background value for getTransition: " + strColor);
//                        return false;
//                    }
//                }
            }
        }

        DynamicTransition objTransition = new DynamicTransition(transitionType);
        objTransition.setDelay(transitionDelay);
        objTransition.setDuration(transitionDuration);
        objTransition.setNoOfTilesX(transitionXGridCount);
        objTransition.setNoOfTilesY(transitionYGridCount);
        objTransition.setSnapshotColor(transitionSnapshotColorBg);

        // _objTransition = objTransition;
        return objTransition;
    }

    public static Transition VerifyTransitionType(String strTransition) {
        String strUpper = strTransition.toUpperCase();
        try {
            return Transition.valueOf(strUpper);
        } catch (Exception ex) {
        }
        return Transition.INVALID;
    }

    private int noOfTilesX;
    private int noOfTilesY;
    private double stepSizeX;
    private double stepSizeY;
    private Duration duration;
    private int delay;
    private Interpolator interpolator;
    private List<ImageView> imageViewsFront;
    private List<ImageView> imageViewsBack;
    private List<Rectangle2D> viewPorts;
    private List<Timeline> timelines;
    private List<StackPane> tiles;
    private boolean playing;
    private Image _endImage;
    private Image _startImage;
    private GridPane parent;
    // private GridWidget fromGrid, toGrid;
    private Pane pane;
    private Color snapshotBG;
    private DynamicTransition.Transition type;
    private boolean hideBackSize;

    private Node finalObj; // the thing to be displayed when done

    private Node startObj; // the thing to be displayed when done

    public DynamicTransition(DynamicTransition.Transition which) {
        type = which;
        playing = false;
        hideBackSize = true; // need this for images (like of widgets) with transparent parts
        // _Type = DynamicTransition.getTransition.NONE;
    }

    /**
     * All tiles with an index larger than VISIBLE_UP_TO will be set invisible
     *
     * @param VISIBLE_UP_TO
     */
    private void adjustTilesVisibility(final int visibleUpTo) {
        for (int i = 0; i < (noOfTilesX * noOfTilesY); i++) {
            tiles.get(i).setVisible(i >= visibleUpTo ? false : true);
            tiles.get(i).getTransforms().clear();

            imageViewsFront.get(i).setOpacity(1);
            imageViewsFront.get(i).setTranslateX(0);
            imageViewsFront.get(i).setTranslateY(0);
            imageViewsFront.get(i).setTranslateZ(0);
            imageViewsFront.get(i).setRotate(0);
            imageViewsFront.get(i).setScaleX(1);
            imageViewsFront.get(i).setScaleY(1);

            imageViewsBack.get(i).setOpacity(1);
            imageViewsBack.get(i).setTranslateX(0);
            imageViewsBack.get(i).setTranslateY(0);
            imageViewsBack.get(i).setTranslateZ(0);
            imageViewsBack.get(i).setRotate(0);
            imageViewsBack.get(i).setScaleX(1);
            imageViewsBack.get(i).setScaleY(1);
        }
    }

    /**
     * Check which side of the tile is visible when rotating around one axis
     *
     * @param ROTATE
     * @param INDEX
     */
    private void checkVisibility(final Rotate rotate, final int INDEX) {
        if (hideBackSize) {
            imageViewsBack.get(INDEX).setVisible(false);
        }

        rotate.angleProperty().addListener((ObservableValue<? extends Number> ov, Number oldAngle, Number newAngle) ->
        {
            if (newAngle.doubleValue() > 360) {
                imageViewsFront.get(INDEX).toFront();
            } else if (newAngle.doubleValue() > 270) {
                imageViewsFront.get(INDEX).toFront();
            } else if (newAngle.doubleValue() > 180) {
                imageViewsBack.get(INDEX).toFront();
                if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                    imageViewsBack.get(INDEX).setVisible(true);
                    imageViewsFront.get(INDEX).setVisible(false);
                }
            } else if (newAngle.doubleValue() > 90) {
                imageViewsBack.get(INDEX).toFront();
                if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                    imageViewsBack.get(INDEX).setVisible(true);
                    imageViewsFront.get(INDEX).setVisible(false);
                }
            }
        });
    }

    /**
     * Check which side of the tile is visible when rotating around x and y axis
     *
     * @param ROTATE_X
     * @param ROTATE_Y
     * @param INDEX
     */
    private void checkVisibility(final Rotate rotate_x, final Rotate rotate_y, final int INDEX) {
        if (hideBackSize) {
            imageViewsBack.get(INDEX).setVisible(false); // need this for images (like of widgets) with transparent
            // parts
        }

        rotate_x.angleProperty().addListener((Observable observable) ->
        {
            int angleX = (int) rotate_x.getAngle();
            int angleY = (int) rotate_y.getAngle();
            if (angleX > 0 && angleX < 90) {
                if (angleY > 0 && angleY < 90) {
                    imageViewsFront.get(INDEX).toFront();
                } else if (angleY > 90 && angleY < 270) {
                    imageViewsBack.get(INDEX).toFront();
                    if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                        imageViewsBack.get(INDEX).setVisible(true);
                        imageViewsFront.get(INDEX).setVisible(false);
                    }

                } else if (angleY > 270 && angleY < 360) {
                    imageViewsFront.get(INDEX).toFront();
                }
            } else if (angleX > 90 && angleX < 270) {
                if (angleY > 0 && angleY < 90) {
                    imageViewsBack.get(INDEX).toFront();
                    if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                        imageViewsBack.get(INDEX).setVisible(true);
                        imageViewsFront.get(INDEX).setVisible(false);
                    }
                } else if (angleY > 90 && angleY < 270) {
                    imageViewsFront.get(INDEX).toFront();
                } else if (angleY > 270 && angleY < 360) {
                    imageViewsBack.get(INDEX).toFront();
                    if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                        imageViewsBack.get(INDEX).setVisible(true);
                        imageViewsFront.get(INDEX).setVisible(false);
                    }
                }
            } else {
                if (angleY > 0 && angleY < 90) {
                    imageViewsFront.get(INDEX).toFront();
                } else if (angleY > 90 && angleY < 270) {
                    imageViewsBack.get(INDEX).toFront();
                    if (hideBackSize && !imageViewsBack.get(INDEX).isVisible()) {
                        imageViewsBack.get(INDEX).setVisible(true);
                        imageViewsFront.get(INDEX).setVisible(false);
                    }
                } else if (angleY > 270 && angleY < 360) {
                    imageViewsFront.get(INDEX).toFront();
                }
            }
        });
    }

    // do some memory cleaning, lots of images and stuff floating around otherwise
    private void Cleanup() {
        imageViewsFront = null;
        imageViewsBack = null;
        viewPorts = null;
        timelines = null;
        tiles = null;
        pane = null;
        _startImage = null;
        _endImage = null;
    }

    /**
     * Cube transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void cube(final Image frontImage, final Image backImage, final Interpolator INTERPOLATOR,
                      final Duration DURATION, final int DELAY) {
        adjustTilesVisibility(1);
        viewPorts.clear();

        final Rectangle2D viewPort = new Rectangle2D(0, 0, frontImage.getWidth(), frontImage.getHeight());
        for (int i = 0; i < (noOfTilesX * noOfTilesY); i++) {
            imageViewsFront.get(i).setViewport(viewPort);
            imageViewsBack.get(i).setViewport(viewPort);
        }

        imageViewsFront.get(0).setImage(frontImage);
        imageViewsBack.get(0).setImage(backImage);

        imageViewsFront.get(0).setTranslateZ(-0.5 * frontImage.getWidth());

        imageViewsBack.get(0).setTranslateX(0.5 * frontImage.getWidth());
        imageViewsBack.get(0).setRotationAxis(Rotate.Y_AXIS);
        // imageViewsBack.get(0).setRotate(90);
        imageViewsBack.get(0).setRotate(270); // to grid would be reversed if didn't do this

        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        rotateY.setPivotX(frontImage.getWidth() * 0.5);
        rotateY.setPivotZ(frontImage.getWidth() * 0.5);
        rotateY.angleProperty().addListener((ObservableValue<? extends Number> ov, Number oldAngle, Number newAngle) ->
        {
            if (73 < newAngle.intValue()) {
                imageViewsBack.get(0).toFront();
            }
        });

        Translate translateZ = new Translate(0, 0, frontImage.getWidth() * 0.5);
        tiles.get(0).getTransforms().setAll(rotateY, translateZ);

        KeyValue kvRotateBegin = new KeyValue(rotateY.angleProperty(), 0, INTERPOLATOR);
        KeyValue kvRotateEnd = new KeyValue(rotateY.angleProperty(), 90, INTERPOLATOR);

        KeyValue kvOpacityFrontBegin = new KeyValue(imageViewsFront.get(0).opacityProperty(), 1.0, INTERPOLATOR);
        KeyValue kvOpacityBackBegin = new KeyValue(imageViewsBack.get(0).opacityProperty(), 0.0, INTERPOLATOR);

        KeyValue kvScaleXBegin = new KeyValue(tiles.get(0).scaleXProperty(), 1.0, INTERPOLATOR);
        KeyValue kvScaleYBegin = new KeyValue(tiles.get(0).scaleYProperty(), 1.0, INTERPOLATOR);

        KeyValue kvScaleXMiddle = new KeyValue(tiles.get(0).scaleXProperty(), 0.85, INTERPOLATOR);
        KeyValue kvScaleYMiddle = new KeyValue(tiles.get(0).scaleYProperty(), 0.85, INTERPOLATOR);

        KeyValue kvOpacityFrontMiddle = new KeyValue(imageViewsFront.get(0).opacityProperty(), 1.0, INTERPOLATOR);
        KeyValue kvOpacityBackMiddle = new KeyValue(imageViewsBack.get(0).opacityProperty(), 1.0, INTERPOLATOR);

        KeyValue kvScaleXEnd = new KeyValue(tiles.get(0).scaleXProperty(), 1.0, INTERPOLATOR);
        KeyValue kvScaleYEnd = new KeyValue(tiles.get(0).scaleYProperty(), 1.0, INTERPOLATOR);

        KeyValue kvOpacityFrontEnd = new KeyValue(imageViewsFront.get(0).opacityProperty(), 0.0, INTERPOLATOR);
        KeyValue kvOpacityBackEnd = new KeyValue(imageViewsBack.get(0).opacityProperty(), 1.0, INTERPOLATOR);

        KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvRotateBegin, kvScaleXBegin, kvScaleYBegin, kvOpacityFrontBegin,
                kvOpacityBackBegin);
        KeyFrame kf1 = new KeyFrame(DURATION.multiply(0.5), kvScaleXMiddle, kvScaleYMiddle, kvOpacityFrontMiddle,
                kvOpacityBackMiddle);
        KeyFrame kf2 = new KeyFrame(DURATION, kvRotateEnd, kvScaleXEnd, kvScaleYEnd, kvOpacityFrontEnd,
                kvOpacityBackEnd);

        timelines.get(0).setDelay(Duration.millis(DELAY));
        timelines.get(0).getKeyFrames().setAll(kf0, kf1, kf2);

        // Listen for the last timeline to finish and switch the images
        timelines.get(0).setOnFinished(observable -> transitionFinished());
    }

    // ******************** Other transitions *********************************

    /**
     * Dissolving tiles transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void dissolvingBlocks(final Image front_image, final Image back_image, final Interpolator INTERPOLATOR,
                                  final Duration DURATION, final int DELAY) {
        splitImageXY(front_image, back_image);

        int count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                // Layout the tiles in grid
                tiles.get(count).setTranslateX(x * stepSizeX);
                tiles.get(count).setTranslateY(y * stepSizeY);

                tiles.get(count).getTransforms().clear();
                imageViewsFront.get(count).getTransforms().clear();
                imageViewsBack.get(count).getTransforms().clear();

                // Create the key-values and key-frames and add them to the timelines
                KeyValue kvFrontOpacityBegin = new KeyValue(imageViewsFront.get(count).opacityProperty(), 1,
                        INTERPOLATOR);
                KeyValue kvFrontOpacityEnd = new KeyValue(imageViewsFront.get(count).opacityProperty(), 0,
                        INTERPOLATOR);

                KeyValue kvFrontScaleXBegin = new KeyValue(imageViewsFront.get(count).scaleXProperty(), 1,
                        INTERPOLATOR);
                KeyValue kvFrontScaleXEnd = new KeyValue(imageViewsFront.get(count).scaleXProperty(), 0, INTERPOLATOR);

                KeyValue kvFrontScaleYBegin = new KeyValue(imageViewsFront.get(count).scaleYProperty(), 1,
                        INTERPOLATOR);
                KeyValue kvFrontScaleYEnd = new KeyValue(imageViewsFront.get(count).scaleYProperty(), 0, INTERPOLATOR);

                KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvFrontOpacityBegin, kvFrontScaleXBegin, kvFrontScaleYBegin);
                KeyFrame kf2 = new KeyFrame(DURATION, kvFrontOpacityEnd, kvFrontScaleXEnd, kvFrontScaleYEnd);

                timelines.get(count).setDelay(Duration.millis(DELAY * (x + y)));
                timelines.get(count).getKeyFrames().setAll(kf0, kf2);

                count++;
            }
        }
        // Listen for the last timeline to finish and switch the images
        timelines.get((noOfTilesX * noOfTilesY) - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesX * noOfTilesY);
    }

    private void doTransition(Image currentImage, Image nextImage) {
        _startImage = currentImage;
        _endImage = nextImage;
        if (null == _startImage || null == _endImage) {
            transitionFinished();
            return;

        }
        stepSizeX = _endImage.getWidth() / noOfTilesX;
        stepSizeY = _endImage.getHeight() / noOfTilesY;

        playing = true;

        if (null != pane) {
            double width;
            double height;
            width = _endImage.getWidth();
            height = _endImage.getHeight();
            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);
            pane.setMinSize(width, height);
        }

        switch (type) {
            case VERTICAL_AROUND_X:
                rotateVerticalTilesAroundX(_startImage, _endImage, interpolator, duration, delay);
                break;
            case VERTICAL_AROUND_Y:
                rotateVerticalTilesAroundY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case VERTICAL_AROUND_X_AND_Y:
                rotateVerticalTilesAroundXandY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case HORIZONTAL_AROUND_X:
                rotateHorizontalTilesAroundX(_startImage, _endImage, interpolator, duration, delay);
                break;
            case HORIZONTAL_AROUND_Y:
                rotateHorizontalTilesAroundY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case HORIZONTAL_AROUND_X_AND_Y:
                rotateHorizontalTilesAroundXandY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case RECTANGULAR_AROUND_X:
                rotateRectangularTilesAroundX(_startImage, _endImage, interpolator, duration, delay);
                break;
            case RECTANGULAR_AROUND_Y:
                rotateRectangularTilesAroundY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case RECTANGULAR_AROUND_X_AND_Y:
                rotateRectangularTilesAroundXandY(_startImage, _endImage, interpolator, duration, delay);
                break;
            case DISSOLVING_BLOCKS:
                dissolvingBlocks(_startImage, _endImage, interpolator, duration, delay);
                break;
            case CUBE:
                cube(_startImage, _endImage, interpolator, duration, delay);
                break;
            case FLIP_HORIZONTAL:
                flipHorizontal(_startImage, _endImage, interpolator, duration, delay);
                break;
            case FLIP_VERTICAL:
                flipVertical(_startImage, _endImage, interpolator, duration, delay);
                break;

            // break;
            case NONE:
                // break;
            default:
                transitionFinished();
                return;
        }

        for (Timeline timeline : timelines) {
            timeline.play();
        }
    }

    private void flipHorizontal(final Image FRONT_IMAGE, final Image BACK_IMAGE, final Interpolator INTERPOLATOR,
                                final Duration DURATION, final int DELAY) {
        viewPorts.clear();

        // PreTransform backside imageview
        Rotate preRotateX = new Rotate(180, 0, -FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);
        imageViewsBack.get(0).getTransforms().setAll(preRotateX);

        // Create the animations
        Rotate rotateX = new Rotate(0, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);
        checkVisibility(rotateX, 0);

        imageViewsFront.get(0).getTransforms().setAll(rotateX);
        imageViewsBack.get(0).getTransforms().addAll(rotateX);

        KeyValue kvXBegin = new KeyValue(rotateX.angleProperty(), 0, INTERPOLATOR);
        KeyValue kvXEnd = new KeyValue(rotateX.angleProperty(), 180, INTERPOLATOR);

        KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
        KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

        timelines.get(0).getKeyFrames().setAll(kf0, kf1);

        timelines.get(0).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(1);
    }

    private void flipVertical(final Image FRONT_IMAGE, final Image BACK_IMAGE, final Interpolator INTERPOLATOR,
                              final Duration DURATION, final int DELAY) {
        viewPorts.clear();

        // PreTransform backside imageview
        Rotate preRotateX = new Rotate(180, 0, -FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.Y_AXIS);
        imageViewsBack.get(0).getTransforms().setAll(preRotateX);

        // Create the animations
        Rotate rotateX = new Rotate(0, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.Y_AXIS);
        checkVisibility(rotateX, 0);

        imageViewsFront.get(0).getTransforms().setAll(rotateX);
        imageViewsBack.get(0).getTransforms().addAll(rotateX);

        KeyValue kvXBegin = new KeyValue(rotateX.angleProperty(), 0, INTERPOLATOR);
        KeyValue kvXEnd = new KeyValue(rotateX.angleProperty(), 180, INTERPOLATOR);

        KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
        KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

        timelines.get(0).getKeyFrames().setAll(kf0, kf1);

        timelines.get(0).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(1);
    }

    public int getDelay() {
        return delay;
    }

    public Duration getDuration() {
        return duration;
    }

    public int getNoOfTilesX() {
        return noOfTilesX;
    }

    public int getNoOfTilesY() {
        return noOfTilesY;
    }

    public Color getSnapshotColor() {
        return snapshotBG;
    }

    // ******************** Methods for horizontal tiles **********************

    /**
     * Rotate horizontal tiles around x transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateHorizontalTilesAroundX(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                              final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageY(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        for (int i = 0; i < noOfTilesY; i++) {
            Rotate rotateX = new Rotate(180, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
            imageViewsBack.get(i).getTransforms().setAll(rotateX);
        }

        for (int i = 0; i < noOfTilesY; i++) {
            // Create the animations
            Rotate rotateX = new Rotate(0, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
            Translate translateY1 = new Translate(0, 0, 0);
            Translate translateY2 = new Translate(0, 0, 0);

            checkVisibility(rotateX, i);

            imageViewsFront.get(i).getTransforms().setAll(rotateX, translateY1);
            imageViewsBack.get(i).getTransforms().addAll(rotateX, translateY2);

            // Layout the tiles vertical
            tiles.get(i).setTranslateX(0);
            tiles.get(i).setTranslateY(i * stepSizeY);

            KeyValue kvXBegin = new KeyValue(rotateX.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvXEnd = new KeyValue(rotateX.angleProperty(), 180, INTERPOLATOR);
            KeyValue kvTranslate1Begin = new KeyValue(translateY1.yProperty(), 0, INTERPOLATOR);
            KeyValue kvTranslate2Begin = new KeyValue(translateY2.yProperty(), 0, INTERPOLATOR);

            KeyValue kvTranslate1Middle = new KeyValue(translateY1.yProperty(), -stepSizeY * 0.25, INTERPOLATOR);
            KeyValue kvTranslate2Middle = new KeyValue(translateY2.yProperty(), stepSizeY * 0.25, INTERPOLATOR);

            KeyValue kvTranslate1End = new KeyValue(translateY1.yProperty(), 0, INTERPOLATOR);
            KeyValue kvTranslate2End = new KeyValue(translateY2.yProperty(), 0, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin, kvTranslate1Begin, kvTranslate2Begin);
            KeyFrame kf1 = new KeyFrame(DURATION.multiply(0.25), kvTranslate1Middle, kvTranslate2Middle);
            KeyFrame kf2 = new KeyFrame(DURATION, kvXEnd, kvTranslate1End, kvTranslate2End);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1, kf2);
        }
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesY);
    }

    /**
     * Rotate horizontal tiles around x and y transition between front- and
     * backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateHorizontalTilesAroundXandY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                                  final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageY(FRONT_IMAGE, BACK_IMAGE);

        for (int i = 0; i < noOfTilesY; i++) {
            // Create the rotation objects
            Rotate rotateXFront = new Rotate(0, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
            Rotate rotateYFront = new Rotate(0, FRONT_IMAGE.getWidth() * 0.5, 0, 0, Rotate.Y_AXIS);

            Rotate rotateXBack = new Rotate(180, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
            Rotate rotateYBack = new Rotate(0, FRONT_IMAGE.getWidth() * 0.5, 0, 0, Rotate.Y_AXIS);

            // Add a listener to the rotation objects
            checkVisibility(rotateXFront, rotateYFront, i);

            // Add the rotations to the image views
            imageViewsFront.get(i).getTransforms().setAll(rotateXFront, rotateYFront);
            imageViewsBack.get(i).getTransforms().setAll(rotateXBack, rotateYBack);

            // Layout the tiles vertical
            tiles.get(i).setTranslateX(0);
            tiles.get(i).setTranslateY(i * stepSizeY);

            // Create the key-values and key-frames and add them to the timelines
            KeyValue kvXFrontBegin = new KeyValue(rotateXFront.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvXFrontEnd = new KeyValue(rotateXFront.angleProperty(), 180, INTERPOLATOR);
            KeyValue kvXBackBegin = new KeyValue(rotateXBack.angleProperty(), -180, INTERPOLATOR);
            KeyValue kvXBackEnd = new KeyValue(rotateXBack.angleProperty(), 0, INTERPOLATOR);

            KeyValue kvYFrontBegin = new KeyValue(rotateYFront.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvYFrontEnd = new KeyValue(rotateYFront.angleProperty(), 360, INTERPOLATOR);
            KeyValue kvYBackBegin = new KeyValue(rotateYBack.angleProperty(), 360, INTERPOLATOR);
            KeyValue kvYBackEnd = new KeyValue(rotateYBack.angleProperty(), 0, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXFrontBegin, kvXBackBegin, kvYFrontBegin, kvYBackBegin);
            KeyFrame kf1 = new KeyFrame(DURATION, kvXFrontEnd, kvXBackEnd, kvYFrontEnd, kvYBackEnd);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1);
        }
        // Listen for the last timeline to finish and switch the images
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesY);
    }

    /**
     * Rotate horizontal tiles around y transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateHorizontalTilesAroundY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                              final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageY(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        for (int i = 0; i < noOfTilesY; i++) {
            Rotate rotateY = new Rotate(180, FRONT_IMAGE.getWidth() * 0.5, 0, 0, Rotate.Y_AXIS);
            imageViewsBack.get(i).getTransforms().setAll(rotateY);
        }

        for (int i = 0; i < noOfTilesX; i++) {
            // Create the animations
            Rotate rotateY = new Rotate(0, FRONT_IMAGE.getWidth() * 0.5, 0, 0, Rotate.Y_AXIS);
            checkVisibility(rotateY, i);

            imageViewsFront.get(i).getTransforms().setAll(rotateY);
            imageViewsBack.get(i).getTransforms().addAll(rotateY);

            // Layout the tiles vertical
            tiles.get(i).setTranslateX(0);
            tiles.get(i).setTranslateY(i * stepSizeY);

            KeyValue kvXBegin = new KeyValue(rotateY.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvXEnd = new KeyValue(rotateY.angleProperty(), 180, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
            KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1);
        }
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesY);
    }

    // ******************** Methods for rectangular tiles *********************

    /**
     * Rotating tiles around x transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateRectangularTilesAroundX(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                               final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageXY(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        int count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                Rotate rotateX = new Rotate(180, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
                imageViewsBack.get(count).getTransforms().setAll(rotateX);
                count++;
            }
        }

        count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                // Create the animations
                Rotate rotateX = new Rotate(0, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
                checkVisibility(rotateX, count);

                imageViewsFront.get(count).getTransforms().setAll(rotateX);
                imageViewsBack.get(count).getTransforms().addAll(rotateX);

                // Layout the tiles in grid
                tiles.get(count).setTranslateX(x * stepSizeX);
                tiles.get(count).setTranslateY(y * stepSizeY);

                KeyValue kvXBegin = new KeyValue(rotateX.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvXEnd = new KeyValue(rotateX.angleProperty(), 180, INTERPOLATOR);

                KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
                KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

                timelines.get(count).setDelay(Duration.millis(DELAY * (2 * x + y)));
                timelines.get(count).getKeyFrames().setAll(kf0, kf1);

                count++;
            }
        }
        timelines.get((noOfTilesX * noOfTilesY) - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesX * noOfTilesY);
    }

    /**
     * Rotating tiles in x and y transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateRectangularTilesAroundXandY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                                   final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageXY(FRONT_IMAGE, BACK_IMAGE);

        int count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                // Create the rotation objects
                Rotate rotateXFront = new Rotate(0, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
                Rotate rotateYFront = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);

                Rotate rotateXBack = new Rotate(180, 0, stepSizeY * 0.5, 0, Rotate.X_AXIS);
                Rotate rotateYBack = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);

                Translate translateZFront = new Translate();
                Translate translateZBack = new Translate();

                // Add a listener to the rotation objects
                checkVisibility(rotateXFront, rotateYFront, count);

                // Add the rotations to the image views
                imageViewsFront.get(count).getTransforms().setAll(rotateXFront, rotateYFront, translateZFront);
                imageViewsBack.get(count).getTransforms().setAll(rotateXBack, rotateYBack, translateZBack);

                // Layout the tiles in grid
                tiles.get(count).setTranslateX(x * stepSizeX);
                tiles.get(count).setTranslateY(y * stepSizeY);

                // Create the key-values and key-frames and add them to the timelines
                KeyValue kvXFrontBegin = new KeyValue(rotateXFront.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvXFrontEnd = new KeyValue(rotateXFront.angleProperty(), 180, INTERPOLATOR);
                KeyValue kvXBackBegin = new KeyValue(rotateXBack.angleProperty(), -180, INTERPOLATOR);
                KeyValue kvXBackEnd = new KeyValue(rotateXBack.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvZFrontBegin = new KeyValue(translateZFront.zProperty(), 0, INTERPOLATOR);
                KeyValue kvzBackBegin = new KeyValue(translateZBack.zProperty(), 0, INTERPOLATOR);

                KeyValue kvZFrontMiddle = new KeyValue(translateZFront.zProperty(), 50, INTERPOLATOR);
                KeyValue kvZBackMiddle = new KeyValue(translateZBack.zProperty(), -50, INTERPOLATOR);

                KeyValue kvYFrontBegin = new KeyValue(rotateYFront.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvYFrontEnd = new KeyValue(rotateYFront.angleProperty(), 360, INTERPOLATOR);
                KeyValue kvYBackBegin = new KeyValue(rotateYBack.angleProperty(), 360, INTERPOLATOR);
                KeyValue kvYBackEnd = new KeyValue(rotateYBack.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvZFrontEnd = new KeyValue(translateZFront.zProperty(), 0, INTERPOLATOR);
                KeyValue kvZBackEnd = new KeyValue(translateZBack.zProperty(), 0, INTERPOLATOR);

                KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXFrontBegin, kvXBackBegin, kvYFrontBegin, kvYBackBegin,
                        kvZFrontBegin, kvzBackBegin);
                KeyFrame kf1 = new KeyFrame(DURATION.multiply(0.5), kvZFrontMiddle, kvZBackMiddle);
                KeyFrame kf2 = new KeyFrame(DURATION, kvXFrontEnd, kvXBackEnd, kvYFrontEnd, kvYBackEnd, kvZFrontEnd,
                        kvZBackEnd);

                timelines.get(count).setDelay(Duration.millis(DELAY * (x + y)));
                timelines.get(count).getKeyFrames().setAll(kf0, kf1, kf2);

                count++;
            }
        }
        // Listen for the last timeline to finish and switch the images
        timelines.get((noOfTilesX * noOfTilesY) - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesX * noOfTilesY);
    }

    /**
     * Rotating tiles around y transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateRectangularTilesAroundY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                               final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageXY(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        int count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                Rotate rotateY = new Rotate(180, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);
                imageViewsBack.get(count).getTransforms().setAll(rotateY);
                count++;
            }
        }

        count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                // Create the animations
                Rotate rotateY = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);

                checkVisibility(rotateY, count);

                imageViewsFront.get(count).getTransforms().setAll(rotateY);
                imageViewsBack.get(count).getTransforms().addAll(rotateY);

                // Layout the tiles in grid
                tiles.get(count).setTranslateX(x * stepSizeX);
                tiles.get(count).setTranslateY(y * stepSizeY);

                KeyValue kvXBegin = new KeyValue(rotateY.angleProperty(), 0, INTERPOLATOR);
                KeyValue kvXEnd = new KeyValue(rotateY.angleProperty(), 180, INTERPOLATOR);

                KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
                KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

                timelines.get(count).setDelay(Duration.millis(DELAY * (x + 2 * y)));
                timelines.get(count).getKeyFrames().setAll(kf0, kf1);

                count++;
            }
        }
        timelines.get((noOfTilesX * noOfTilesY) - 1).setOnFinished(observable -> transitionFinished());

        adjustTilesVisibility(noOfTilesX * noOfTilesY);
    }

    // ******************** Methods for vertical tiles ************************

    /**
     * Rotate vertical tiles around x transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateVerticalTilesAroundX(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                            final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageX(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        for (int i = 0; i < noOfTilesX; i++) {
            Rotate rotateX = new Rotate(180, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);
            imageViewsBack.get(i).getTransforms().setAll(rotateX);
        }

        for (int i = 0; i < noOfTilesX; i++) {
            // Create the animations
            Rotate rotateX = new Rotate(0, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);

            checkVisibility(rotateX, i);

            imageViewsFront.get(i).getTransforms().setAll(rotateX);
            imageViewsBack.get(i).getTransforms().addAll(rotateX);

            // Layout the tiles horizontal
            tiles.get(i).setTranslateX(i * stepSizeX);
            tiles.get(i).setTranslateY(0);

            KeyValue kvXBegin = new KeyValue(rotateX.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvXEnd = new KeyValue(rotateX.angleProperty(), 180, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXBegin);
            KeyFrame kf1 = new KeyFrame(DURATION, kvXEnd);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1);
        }
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());
        adjustTilesVisibility(noOfTilesX);
    }

    /**
     * Rotate vertical tiles around x and y transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateVerticalTilesAroundXandY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                                final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageX(FRONT_IMAGE, BACK_IMAGE);

        for (int i = 0; i < noOfTilesX; i++) {
            // Create the rotation objects
            Rotate rotateXFront = new Rotate(0, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);
            Rotate rotateYFront = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);

            Rotate rotateXBack = new Rotate(180, 0, FRONT_IMAGE.getHeight() * 0.5, 0, Rotate.X_AXIS);
            Rotate rotateYBack = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);

            // Add a listener to the rotation objects
            checkVisibility(rotateXFront, rotateYFront, i);

            // Add the rotations to the image views
            imageViewsFront.get(i).getTransforms().setAll(rotateXFront, rotateYFront);
            imageViewsBack.get(i).getTransforms().setAll(rotateXBack, rotateYBack);

            // Layout the tiles horizontal
            tiles.get(i).setTranslateX(i * stepSizeX);
            tiles.get(i).setTranslateY(0);

            // Create the key-values and key-frames and add them to the timelines
            KeyValue kvXFrontBegin = new KeyValue(rotateXFront.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvXFrontEnd = new KeyValue(rotateXFront.angleProperty(), 180, INTERPOLATOR);
            KeyValue kvXBackBegin = new KeyValue(rotateXBack.angleProperty(), -180, INTERPOLATOR);
            KeyValue kvXBackEnd = new KeyValue(rotateXBack.angleProperty(), 0, INTERPOLATOR);

            KeyValue kvYFrontBegin = new KeyValue(rotateYFront.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvYFrontEnd = new KeyValue(rotateYFront.angleProperty(), 360, INTERPOLATOR);
            KeyValue kvYBackBegin = new KeyValue(rotateYBack.angleProperty(), 360, INTERPOLATOR);
            KeyValue kvYBackEnd = new KeyValue(rotateYBack.angleProperty(), 0, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvXFrontBegin, kvXBackBegin, kvYFrontBegin, kvYBackBegin);
            KeyFrame kf1 = new KeyFrame(DURATION, kvXFrontEnd, kvXBackEnd, kvYFrontEnd, kvYBackEnd);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1);
        }
        // Listen for the last timeline to finish and switch the images
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());
        adjustTilesVisibility(noOfTilesX);
    }

    /**
     * Rotate vertical tiles around y transition between front- and backimage
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     * @param INTERPOLATOR spline that is used for the animation
     * @param DURATION     oneSecond for the transition
     * @param DELAY        delay in milliseconds between each tile animation
     */
    private void rotateVerticalTilesAroundY(final Image FRONT_IMAGE, final Image BACK_IMAGE,
                                            final Interpolator INTERPOLATOR, final Duration DURATION, final int DELAY) {
        splitImageX(FRONT_IMAGE, BACK_IMAGE);

        // PreTransform backside imageviews
        for (int i = 0; i < noOfTilesX; i++) {
            Rotate rotateY = new Rotate(180, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);
            imageViewsBack.get(i).getTransforms().setAll(rotateY);
        }

        for (int i = 0; i < noOfTilesX; i++) {
            // Create the animations
            Rotate rotateY = new Rotate(0, stepSizeX * 0.5, 0, 0, Rotate.Y_AXIS);
            Translate translateX1 = new Translate(0, 0, 0);
            Translate translateX2 = new Translate(0, 0, 0);
            checkVisibility(rotateY, i);

            imageViewsFront.get(i).getTransforms().setAll(rotateY, translateX1);
            imageViewsBack.get(i).getTransforms().addAll(rotateY, translateX2);

            // Layout the tiles horizontal
            tiles.get(i).setTranslateX(i * stepSizeX);
            tiles.get(i).setTranslateY(0);

            KeyValue kvRotateBegin = new KeyValue(rotateY.angleProperty(), 0, INTERPOLATOR);
            KeyValue kvRotateEnd = new KeyValue(rotateY.angleProperty(), 180, INTERPOLATOR);
            KeyValue kvTranslate1Begin = new KeyValue(translateX1.xProperty(), 0, INTERPOLATOR);
            KeyValue kvTranslate2Begin = new KeyValue(translateX2.xProperty(), 0, INTERPOLATOR);

            KeyValue kvTranslate1Middle = new KeyValue(translateX1.xProperty(), -stepSizeX * 0.5, INTERPOLATOR);
            KeyValue kvTranslate2Middle = new KeyValue(translateX2.xProperty(), stepSizeX * 0.5, INTERPOLATOR);

            KeyValue kvTranslate1End = new KeyValue(translateX1.xProperty(), 0, INTERPOLATOR);
            KeyValue kvTranslate2End = new KeyValue(translateX2.xProperty(), 0, INTERPOLATOR);

            KeyFrame kf0 = new KeyFrame(Duration.ZERO, kvRotateBegin, kvTranslate1Begin, kvTranslate2Begin);
            KeyFrame kf1 = new KeyFrame(DURATION.multiply(0.5), kvTranslate1Middle, kvTranslate2Middle);
            KeyFrame kf2 = new KeyFrame(DURATION, kvRotateEnd, kvTranslate1End, kvTranslate2End);

            timelines.get(i).setDelay(Duration.millis(DELAY * i));
            timelines.get(i).getKeyFrames().setAll(kf0, kf1, kf2);
        }
        timelines.get(noOfTilesX - 1).setOnFinished(observable -> transitionFinished());
        adjustTilesVisibility(noOfTilesX);
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public void setNoOfTilesX(int noOfTilesX) {
        this.noOfTilesX = noOfTilesX;
    }

    public void setNoOfTilesY(int noOfTilesY) {
        this.noOfTilesY = noOfTilesY;
    }

    public void setSnapshotColor(Color color) {
        snapshotBG = color;
    }

    @SuppressWarnings("incomplete-switch")
    private void Setup(int xLoc, int yLoc) {
        switch (type) {
            case VERTICAL_AROUND_X:
            case VERTICAL_AROUND_Y:
            case HORIZONTAL_AROUND_Y:
                interpolator = Interpolator.EASE_BOTH;
                break;
            case VERTICAL_AROUND_X_AND_Y:
            case HORIZONTAL_AROUND_X:
            case HORIZONTAL_AROUND_X_AND_Y:
            case RECTANGULAR_AROUND_X:
            case RECTANGULAR_AROUND_Y:
            case RECTANGULAR_AROUND_X_AND_Y:
            case DISSOLVING_BLOCKS:
            case CUBE:
            case FLIP_HORIZONTAL:
            case FLIP_VERTICAL:
                interpolator = Interpolator.SPLINE(0.7, 0, 0.3, 1);
                break;

            // break;
            case NONE:
                // break;
            default:
                return;
        }

        if (type == DynamicTransition.Transition.FLIP_HORIZONTAL) {
            type = DynamicTransition.Transition.RECTANGULAR_AROUND_Y;
            noOfTilesX = 1;
            noOfTilesY = 1;
        } else if (type == DynamicTransition.Transition.FLIP_VERTICAL) {
            type = DynamicTransition.Transition.RECTANGULAR_AROUND_X;
            noOfTilesX = 1;
            noOfTilesY = 1;
        }

        imageViewsFront = new ArrayList<>(noOfTilesX * noOfTilesY);
        imageViewsBack = new ArrayList<>(noOfTilesX * noOfTilesY);
        viewPorts = new ArrayList<>(noOfTilesX * noOfTilesY);
        timelines = new ArrayList<>(noOfTilesX * noOfTilesY);
        tiles = new ArrayList<>(noOfTilesX * noOfTilesY);

        pane = new Pane();

        int count = 0;
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                imageViewsFront.add(new ImageView());
                imageViewsBack.add(new ImageView());

                timelines.add(new Timeline());

                tiles.add(new StackPane(imageViewsBack.get(count), imageViewsFront.get(count)));
                count++;
            }
        }
        pane.getChildren().setAll(tiles);

        parent.add(pane, xLoc, yLoc); // TODO: I think may need to set col and row span here too......
    }

    /**
     * Split the given images into vertical tiles defined by noOfTilesX
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     */
    private void splitImageX(final Image FRONT_IMAGE, final Image BACK_IMAGE) {
        viewPorts.clear();
        for (int i = 0; i < noOfTilesX; i++) {
            // Create the viewports
            viewPorts.add(new Rectangle2D(i * stepSizeX, 0, stepSizeX, FRONT_IMAGE.getHeight()));

            // Update the frontside imageviews
            imageViewsFront.get(i).getTransforms().clear();
            imageViewsFront.get(i).toFront();
            imageViewsFront.get(i).setImage(FRONT_IMAGE);
            imageViewsFront.get(i).setViewport(viewPorts.get(i));

            // Update the backside imageviews
            imageViewsBack.get(i).getTransforms().clear();
            imageViewsBack.get(i).setImage(BACK_IMAGE);
            imageViewsBack.get(i).setViewport(viewPorts.get(i));
        }
    }

    /**
     * Split the given images into rectangular tiles defined by noOfTilesX and
     * noOfTilesY
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     */
    private void splitImageXY(final Image FRONT_IMAGE, final Image BACK_IMAGE) {
        int count = 0;
        viewPorts.clear();
        for (int y = 0; y < noOfTilesY; y++) {
            for (int x = 0; x < noOfTilesX; x++) {
                // Create the viewports
                viewPorts.add(new Rectangle2D(x * stepSizeX, y * stepSizeY, stepSizeX, stepSizeY));

                // Update the frontside imageviews
                imageViewsFront.get(count).getTransforms().clear();
                imageViewsFront.get(count).toFront();
                imageViewsFront.get(count).setImage(FRONT_IMAGE);
                imageViewsFront.get(count).setViewport(viewPorts.get(count));

                // Update the backside imageviews
                imageViewsBack.get(count).getTransforms().clear();
                imageViewsBack.get(count).setImage(BACK_IMAGE);
                imageViewsBack.get(count).setViewport(viewPorts.get(count));

                count++;
            }
        }
    }

    /**
     * Split the given images into horizontal tiles defined by noOfTilesY
     *
     * @param FRONT_IMAGE
     * @param BACK_IMAGE
     */
    private void splitImageY(final Image FRONT_IMAGE, final Image BACK_IMAGE) {
        viewPorts.clear();
        for (int i = 0; i < noOfTilesY; i++) {
            // Create the viewports
            viewPorts.add(new Rectangle2D(0, i * stepSizeY, FRONT_IMAGE.getWidth(), stepSizeY));

            // Update the frontside imageviews
            imageViewsFront.get(i).getTransforms().clear();
            imageViewsFront.get(i).toFront();
            imageViewsFront.get(i).setImage(FRONT_IMAGE);
            imageViewsFront.get(i).setViewport(viewPorts.get(i));

            // Update the backside imageviews
            imageViewsBack.get(i).getTransforms().clear();
            imageViewsBack.get(i).setImage(BACK_IMAGE);
            imageViewsBack.get(i).setViewport(viewPorts.get(i));
        }
    }

    public boolean stillPlaying() {
        return playing;
    }

    public void stopTransition() {
        if (!stillPlaying()) {
            return;
        }
        playing = false;

        for (Timeline timeline : timelines) {
            timeline.stop();
        }
        transitionFinished();
    }

    public void Transition(DynamicGrid fromGridObj, DynamicGrid toGridObj, GridPane objParent) {
        parent = objParent;

        int x = GridPane.getColumnIndex(fromGridObj.getBasePane());
        int y = GridPane.getRowIndex(fromGridObj.getBasePane());

        Setup(x, y);

        Image startImage = fromGridObj.getImage(fromGridObj.getBackgroundColorForTransition());
        if (null == startImage) {
            LOGGER.severe("Unable to take snapshot for 1st image in Transition.  Likely not enough memory.");
            // return;
        }
        toGridObj.getBasePane().setVisible(true); // needs to be visable for snapshot to work

        Image endImage = toGridObj.getImage(toGridObj.getBackgroundColorForTransition());
        if (null == endImage) {
            LOGGER.severe("Unable to take snapshot for 2nd image in Transition.  Likely not enough memory.");
            // return;
        }

        toGridObj.getBasePane().setVisible(false);
        fromGridObj.getBasePane().setVisible(false);
        startObj = fromGridObj.getBasePane();
        finalObj = toGridObj.getBasePane();

        doTransition(startImage, endImage);

    }

    public void Transition(DynamicImageWidget objDynamicImage, ImageView objImageViewStart, ImageView objImageViewFinal) {
        if (type == DynamicTransition.Transition.NONE) {
            transitionFinished();
            return;
        }

        int x = objDynamicImage.getColumn();
        int y = objDynamicImage.getRow();
        parent = objDynamicImage.GetContainerPane();
        Setup(x, y);
        // pane.prefHeight(objImageViewFinal.destHeight());
        // pane.prefWidth(objImageViewFinal.getFitWidth());

        startObj = objImageViewStart;
        finalObj = objImageViewFinal;

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(getSnapshotColor());
        // params.setFill(Color.TRANSPARENT);
        Image startImage = objDynamicImage.GetContainerPane().snapshot(params, null);

        startObj.setVisible(false);
        finalObj.setVisible(true);

        Image finalImage = objDynamicImage.GetContainerPane().snapshot(params, null);

        // _startObj.setVisible(true);
        finalObj.setVisible(false);

        if (null == startImage || finalImage == null) {
            LOGGER.severe("Unable to do transition for DynamicImage - likely due to low memory.");
            startObj.setVisible(false);
            finalObj.setVisible(true);
            return;
        }

        doTransition(startImage, finalImage);

    }

    private void transitionFinished() {
        playing = false;
        if (null != startObj) {
            startObj.setVisible(false);
        }
        if (null != finalObj) {
            finalObj.setVisible(true);
        }

        if (null != pane) {
            pane.setVisible(false);
            pane.getChildren().clear();
            parent.getChildren().remove(pane);
        }
        Cleanup();
    }

}
