package servent.handler;

import app.AppConfig;
import app.Job;
import app.Point;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ResultReplyMessage;
import servent.message.util.MessageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ResultReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public ResultReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.RESULT_REPLY) {
            int requestorId = Integer.parseInt(clientMessage.getMessageText());
            ResultReplyMessage resultReplyMessage = (ResultReplyMessage) clientMessage;
            if (requestorId == AppConfig.myServentInfo.getUuid()) {
                List<Point> results = resultReplyMessage.getResults();

                Job myJob = AppConfig.jobs.stream().filter(job -> job.getName().equals(AppConfig.pendingResultJobName)).findFirst().get();
                BufferedImage image = new BufferedImage(myJob.getWidth(), myJob.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                WritableRaster writableRaster = image.getRaster();
                int[] rgb = new int[3];
                rgb[0] = 255;

                for (Point p : results) {
                    writableRaster.setPixel((int) p.getX(), (int) p.getY(), rgb);
                }

                BufferedImage newImage = new BufferedImage(writableRaster.getWidth(), writableRaster.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                newImage.setData(writableRaster);

                try {
                    ImageIO.write(newImage, "PNG", new File(myJob.getName() + ".png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                ResultReplyMessage resultReplyMessageNew = new ResultReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                        resultReplyMessage.getResults());
                MessageUtil.sendMessage(resultReplyMessageNew);
            }
        }
    }
}
