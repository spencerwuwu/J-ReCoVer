// https://searchcode.com/api/result/109547021/

package org.atlasapi.remotesite.talktalk;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.ContentGroupWriter;
import org.atlasapi.persistence.content.ResolvedContent;
import org.atlasapi.remotesite.talktalk.TalkTalkClient.TalkTalkTvStructureCallback;
import org.atlasapi.remotesite.talktalk.vod.bindings.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.scheduling.UpdateProgress;

/**
 * {@link ScheduledTask} which retrieves the TV Structure via the provided
 * {@link TalkTalkClient} and processes each {@link ChannelType} in turn using
 * the provided {@link TalkTalkChannelProcessor}.
 */
public class TalkTalkChannelProcessingTask extends ScheduledTask {
    
    public static final String ALL_CONTENT_CONTENT_GROUP_URI = "http://talktalk.net/content-groups/all-content";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final TalkTalkClient client;

    private TalkTalkChannelProcessor<UpdateProgress> channelProcessor;
    private final ContentGroupResolver contentGroupResolver;
    private final ContentGroupWriter contentGroupWriter;

    public TalkTalkChannelProcessingTask(TalkTalkClient talkTalkClient, 
            TalkTalkChannelProcessor<UpdateProgress> channelProcessor,
            ContentGroupResolver contentGroupResolver,
            ContentGroupWriter contentGroupWriter) {
        this.client = checkNotNull(talkTalkClient);
        this.channelProcessor = checkNotNull(channelProcessor);
        this.contentGroupResolver = checkNotNull(contentGroupResolver);
        this.contentGroupWriter = checkNotNull(contentGroupWriter);
    }

    @Override
    protected void runTask() {
        try {
            final ContentGroup contentGroup = createOrGetContentGroup();
            client.processTvStructure(new TalkTalkTvStructureCallback<UpdateProgress>() {

                private UpdateProgress progress = UpdateProgress.START;
                
                @Override
                public UpdateProgress getResult() {
                    return progress;
                }
                
                @Override
                public void process(ChannelType channel) {
                    try {
                        log.debug("processing channel {}", channel.getId());
                        progress = progress.reduce(channelProcessor.process(channel, 
                                Optional.of(contentGroup)));
                        reportStatus(progress.toString());
                    } catch (TalkTalkException tte) {
                        log.error("failed to process " + channel.getId(), tte);
                        progress = progress.reduce(UpdateProgress.FAILURE);
                    }
                }
                
            });
            
            contentGroupWriter.createOrUpdate(contentGroup);
        } catch (TalkTalkException tte) {
            // ensure task is marked failed, the exception is logged by the
            // scheduler.
            throw new RuntimeException(tte);
        }
    }
    
    private ContentGroup createOrGetContentGroup() {
        ContentGroup contentGroup;
        ResolvedContent resolvedContent = contentGroupResolver
                .findByCanonicalUris(ImmutableList.of(ALL_CONTENT_CONTENT_GROUP_URI));
        
        if (resolvedContent.get(ALL_CONTENT_CONTENT_GROUP_URI).hasValue()) {
            contentGroup = (ContentGroup) resolvedContent.get(ALL_CONTENT_CONTENT_GROUP_URI).requireValue();
            contentGroup.setContents(ImmutableList.<ChildRef>of());
        } else {
            contentGroup = new ContentGroup(ALL_CONTENT_CONTENT_GROUP_URI, Publisher.TALK_TALK);
        }
        
        return contentGroup;
    }
    
}

