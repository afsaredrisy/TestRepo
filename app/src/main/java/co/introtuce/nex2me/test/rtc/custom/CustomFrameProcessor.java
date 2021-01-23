package co.introtuce.nex2me.test.rtc.custom;

import android.content.Context;
import android.util.Log;

import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.framework.AppTextureFrame;
import com.google.mediapipe.framework.MediaPipeException;
import com.google.mediapipe.framework.Packet;

public class CustomFrameProcessor extends FrameProcessor {
    private String bGVideoInputStream;
    private String x_center_stream;
    private String y_center_stream;
    private boolean switchseg = false;

    /**
     * Constructor.
     *
     * @param context             an Android {@link Context}.
     * @param parentNativeContext a native handle to a GL context. The GL context(s) used by the
     *                            calculators in the graph will join the parent context's sharegroup, so that textures
     *                            generated by the calculators are available in the parent context, and vice versa.
     * @param graphName           the name of the file containing the binary representation of the graph.
     * @param inputStream         the graph input stream that will receive input video frames to be segmented.
     * @param outputStream        the output stream from which output frames will be produced.
     * @param bGVideoInputStream  the graph input stream that will receive background frames to be overlay.
     */
    public CustomFrameProcessor(Context context, long parentNativeContext, String graphName, String inputStream, String outputStream, String bGVideoInputStream) {
        super(context, parentNativeContext, graphName, inputStream, outputStream);
        this.bGVideoInputStream = bGVideoInputStream;
    }

    public CustomFrameProcessor(Context context, long parentNativeContext, String graphName, String inputStream, String outputStream,
                                String bGVideoInputStream, String x_center_stream, String y_center_stream) {
        this(context, parentNativeContext, graphName, inputStream, outputStream,bGVideoInputStream);
        this.x_center_stream=x_center_stream;
        this.y_center_stream=y_center_stream;
    }
    long bgTimestamp;
    long fgTimestamp;

    public long getBgTimestamp() {
        return bgTimestamp;
    }

    public long getFgTimestamp() {
        return fgTimestamp;
    }

    public void onNewFrame(AppTextureFrame foreground, AppTextureFrame background,
                           float x_center, float y_center) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(
                    TAG,
                    String.format(
                            "Input tex: %d width: %d height: %d",
                            foreground.getTextureName(), foreground.getWidth(), foreground.getHeight()));
        }

        Log.d("FRAME_PROCESS","From subclass 4 frames");
        if (!maybeAcceptNewFrame(foreground.getTimestamp())) {
            foreground.release();
            background.release();
            return;
        }
        if (addFrameListener != null) {
            addFrameListener.onWillAddFrame(foreground.getTimestamp());
            addFrameListener.onWillAddFrame(background.getTimestamp());
        }

        Packet imagePacket = packetCreator.createGpuBuffer(foreground);
        Packet bgPacket = packetCreator.createGpuBuffer(background);
        Packet px_center = packetCreator.createFloat32(x_center);
        Packet py_center = packetCreator.createFloat32(y_center);
        try {
            // addConsumablePacketToInputStream allows the graph to take exclusive ownership of the
            // packet, which may allow for more memory optimizations.
            bgTimestamp = background.getTimestamp();
            fgTimestamp=foreground.getTimestamp();
            if(!switchseg){
                mediapipeGraph.addConsumablePacketToInputStream(
                        bGVideoInputStream , imagePacket, foreground.getTimestamp());
                if(bgPacket!=null)
                    mediapipeGraph.addConsumablePacketToInputStream(videoInputStream,bgPacket,foreground.getTimestamp());

            }
            else{
                mediapipeGraph.addConsumablePacketToInputStream(
                        videoInputStream , imagePacket, foreground.getTimestamp());
                if(bgPacket!=null)
                    mediapipeGraph.addConsumablePacketToInputStream(bGVideoInputStream,bgPacket,foreground.getTimestamp());

            }

            //mediapipeGraph.addConsumablePacketToInputStream(x_center_stream,px_center,foreground.getTimestamp());
            //mediapipeGraph.addConsumablePacketToInputStream(y_center_stream,py_center,foreground.getTimestamp());
        } catch (MediaPipeException e) {
            Log.e(TAG, "Mediapipe error: ", e);
        }

        imagePacket.release();
        px_center.release();
        py_center.release();
        if(bgPacket!=null){
            bgPacket.release();
        }

    }


    public void onNewFrame(AppTextureFrame foreground, AppTextureFrame background) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(
                    TAG,
                    String.format(
                            "Input tex: %d width: %d height: %d",
                            foreground.getTextureName(), foreground.getWidth(), foreground.getHeight()));
        }

        Log.d("FRAME_PROCESS","From subclass");
        if (!maybeAcceptNewFrame(foreground.getTimestamp()) && !maybeAcceptNewFrame(background.getTimestamp())) {
            foreground.release();
            background.release();
            return;
        }
        if (addFrameListener != null) {
            addFrameListener.onWillAddFrame(foreground.getTimestamp());
            addFrameListener.onWillAddFrame(background.getTimestamp());
        }

        Packet imagePacket = packetCreator.createGpuBuffer(foreground);
        Packet bgPacket = packetCreator.createGpuBuffer(background);
        try {
            // addConsumablePacketToInputStream allows the graph to take exclusive ownership of the
            // packet, which may allow for more memory optimizations.
            mediapipeGraph.addConsumablePacketToInputStream(
                    videoInputStream , imagePacket, foreground.getTimestamp());
            if(bgPacket!=null)
                mediapipeGraph.addConsumablePacketToInputStream(bGVideoInputStream,bgPacket,background.getTimestamp());
        } catch (MediaPipeException e) {
            Log.e(TAG, "Mediapipe error: ", e);
        }
        imagePacket.release();
        if(bgPacket!=null){
            bgPacket.release();
        }
    }

    public boolean isSwitchseg() {
        return switchseg;
    }

    public void setSwitchseg(boolean switchseg) {
        this.switchseg = switchseg;
    }

}
