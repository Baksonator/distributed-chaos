package servent.handler;

import app.AppConfig;
import app.Job;
import app.Point;
import mutex.LogicalTimestamp;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.MutexReleaseMessage;
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

//                Job myJob = AppConfig.jobs.stream().filter(job -> job.getName().equals(AppConfig.pendingResultJobName)).findFirst().get();
                Job myJob = resultReplyMessage.getJob();
                BufferedImage image = new BufferedImage(myJob.getWidth(), myJob.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                WritableRaster writableRaster = image.getRaster();
                int[] rgb = new int[3];
                rgb[0] = 255;

                for (Point p : results) {
                    writableRaster.setPixel((int) p.getX(), (int) p.getY(), rgb);
                }

                BufferedImage newImage = new BufferedImage(writableRaster.getWidth(), writableRaster.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                newImage.setData(writableRaster);

                String fractalId = "";
                if (resultReplyMessage.isFlag()) {
                    fractalId = resultReplyMessage.getFractalId();
                }

                try {
                    if (fractalId.equals("")) {
                        ImageIO.write(newImage, "PNG", new File(myJob.getName() + ".png"));
                    } else {
                        ImageIO.write(newImage, "PNG", new File(fractalId + ".png"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AppConfig.lamportClock.tick();
                AppConfig.requestQueue.poll();
                MutexReleaseMessage mutexReleaseMessage = new MutexReleaseMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodePort(), Integer.toString(AppConfig.myServentInfo.getUuid()),
                        new LogicalTimestamp(AppConfig.lamportClock.getValue(), AppConfig.myServentInfo.getUuid()), false);
                MessageUtil.sendMessage(mutexReleaseMessage);

                AppConfig.localSemaphore.release();
            } else {
                ResultReplyMessage resultReplyMessageNew = new ResultReplyMessage(AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeForKey(requestorId).getListenerPort(), Integer.toString(requestorId),
                        resultReplyMessage.getResults(), resultReplyMessage.getJob(), resultReplyMessage.isFlag(), resultReplyMessage.getFractalId());
                MessageUtil.sendMessage(resultReplyMessageNew);
            }
        }
    }
}
