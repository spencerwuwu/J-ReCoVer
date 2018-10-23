// https://searchcode.com/api/result/109548227/

package org.atlasapi.remotesite.netflix;

import nu.xom.Document;
import nu.xom.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

public class NetflixUpdater extends ScheduledTask {

    private final NetflixXmlElementHandler elementHandler;
    private static final Logger log = LoggerFactory.getLogger(NetflixUpdater.class);
    private final NetflixFileUpdater fileUpdater;
    
    public NetflixUpdater(NetflixFileUpdater fileUpdater, NetflixXmlElementHandler elementHandler) {
        this.fileUpdater = fileUpdater;
        this.elementHandler = elementHandler;
    }

    @Override
    protected void runTask() {
        try {
            Document netflixData = fileUpdater.updateFile();
            
            Element rootElement = netflixData.getRootElement();
            NetflixDataProcessor<UpdateProgress> processor = processor();
            
            elementHandler.prepare();

            for (int i = 0; i < rootElement.getChildElements().size(); i++) {
                processor.process(rootElement.getChildElements().get(i));
            }
            
            elementHandler.finish();
            
            reportStatus(processor.getResult().toString());
            
        } catch (Exception e) {
            reportStatus(e.getMessage());
            Throwables.propagate(e);
        }
    }
    
    NetflixDataProcessor<UpdateProgress> processor() {
        return new NetflixDataProcessor<UpdateProgress>() {
            
            UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public boolean process(Element element) {
                try {
                    elementHandler.handle(element);
                    progress = progress.reduce(UpdateProgress.SUCCESS);
                } catch (Exception e) {
                    log.warn(element.getLocalName() , e);
                    progress = progress.reduce(UpdateProgress.FAILURE);
                }
                reportStatus(progress.toString());
                return shouldContinue();
            }

            @Override
            public UpdateProgress getResult() {
                return progress;
            }
        };
    }
}

