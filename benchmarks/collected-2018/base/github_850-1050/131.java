// https://searchcode.com/api/result/75875180/

package com.allyes.mifcor;

import com.allyes.carpenter.CarpenterLogs.LogType;
import com.allyes.carpenter.CarpenterLogsMIFC;
import com.allyes.mifcor.constants.Constants;
import com.allyes.mifcor.data.IPStore;
import com.allyes.mifcor.data.MIFCSECampaignInfo;
import com.allyes.mifcor.data.MIFCZ1;
import com.allyes.mifcor.data.ObservReportKeys;
import com.allyes.mifcor.data.SETargetingInfoKeys;
import com.allyes.mifcor.data.TrafficStatus;
import com.allyes.mifcor.utils.Base64Utils;
import com.allyes.mifcor.utils.ConfigFileParser;
import com.allyes.mifcor.utils.DateUtils;
import com.allyes.mifcor.utils.MIFCUtils;
import com.allyes.taishan.TaishanLogs.TaishanLog;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MIFCLogProcessJob extends Configured implements Tool {

    private static Log LOG = LogFactory.getLog(MIFCLogProcessJob.class);
    protected static final String MIFC_CAMPAIGNINFO_LOCAL_CACHE_BASE_PATH = "mifc_campaigninfo";

    private static boolean Debug = false;

    public static class MIFCCSLogProcessMapper extends Mapper<LongWritable, Text, Text, Text> {

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // hadoop fs -rm ./mifc_or/scribe_timestamp/*
            String valueStr = value.toString();
            byte[] valueByteArr = Base64Utils.decode(valueStr);
            if (Debug) {
                throw new IOException("xxxxxxxxxx  valueStr: " + valueStr);
            }
            CarpenterLogsMIFC.RawLog rawlog = CarpenterLogsMIFC.RawLog.parseFrom(valueByteArr);
            if (Debug) {
                throw new IOException("xxxxxxxxxx  valueStr: " + valueStr + "; rawlog: " + rawlog);
            }

            String z1 = rawlog.getZ1();
            MIFCZ1 mifcZ1 = new MIFCZ1(z1);
            String campaignid = mifcZ1.getCampaignId();
            Text mapOutKey = new Text(campaignid);
            Text mapOutValue = new Text(z1);
            if (Debug) {
                throw new IOException("xxxxxxxxxx  mapOutKey: " + mapOutKey + "; mapOutValue: " + mapOutValue);
            }
            context.write(mapOutKey, mapOutValue);
        }

    }

    public static class MIFCCSLogProcessReducer extends AbstractLogProcessReducer<Text> {

        private static final String TARGETING_INFO_KEYS_DELIMETER = "~";

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException,
                InterruptedException {
            Configuration conf = context.getConfiguration();

            // StringBuilder outputSB = new StringBuilder();
            String adnwCampaignId = key.toString();

            // XXX test data
//            adnwCampaignId = "153";
            // validate campaignid exists
            if (!checkCampaignId(adnwCampaignId, mifcSECampaignInfoMap_)) {
                // throw new
                // IOException("xxxxxxxxxx  campaignid doesn't exist: " +
                // solutionid + "; mifcSECampaignInfoMap_ keysets:  " +
                // mifcSECampaignInfoMap_.keySet());
                return;
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx  mifcSECampaignInfoMap_'s size: " + mifcSECampaignInfoMap_.size());
            }
             String seCampaignId = getSECampaignId(adnwCampaignId, mifcSECampaignInfoMap_);
             MIFCSECampaignInfo mifcSECampaignInfo = mifcSECampaignInfoMap_.get(adnwCampaignId);

            List<MIFCZ1> z1LogList = new ArrayList<MIFCZ1>();

            Set<Long> logTimestampSet = new TreeSet<Long>();
            logTimestampSet.add(System.currentTimeMillis()/1000L);
            // count total stats: clicks ,shows, ..., besides count the total
            // cost
            int showCount = 0;
            int clickCount = 0;
            int activationCount = 0;
            double totalCost = 0;
            for (Text value : values) {
                String z1 = value.toString();
                MIFCZ1 mifcZ1 = new MIFCZ1(z1);
                z1LogList.add(mifcZ1);

                // log type
                int logType = mifcZ1.getLogType();
                if (logType == LogType.SHOW.getNumber()) {
                    showCount++;
                } else if (logType == LogType.CLICK.getNumber()) {
                    clickCount++;
                } else if (logType == LogType.IDIGGER.getNumber()) {
                    activationCount++;
                }

                double cost = generatePrice(mifcZ1);
                totalCost += cost;
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx  showCount=: " + showCount + "; clickCount=" + clickCount
                        + "; totalCost=" + totalCost);
            }

            // resolve totalCost
            String spent = generateIncome(String.valueOf(totalCost));

            // targetinfo attribute value's stats map
            Map<String, TrafficStatus> targetingAttributeValueStatsMap = new HashMap<String, TrafficStatus>();

            // count targetingspace's stats
            Map<String, ArrayList<String>> targetingInfoMap = mifcSECampaignInfo.getTargetingInfo();
            if (Debug) {
                throw new IOException("xxxxxxxxxx  targetingInfoMap: " + targetingInfoMap);
            }
            if (Debug) {
                ArrayList<String> geoList = targetingInfoMap.get(SETargetingInfoKeys.GEO.getName());
                throw new IOException("xxxxxxxxxx  targetingInfoMap.keySet(): " + targetingInfoMap.keySet() + "xxxxxxxxxx  geoList: " + geoList.size() + ";geoList.get(0):" +  geoList.get(0));
            }

            // resolve each targeting space
            for (String seTargetKey : targetingInfoMap.keySet()) {
                // seTargetKey: ADNW, seTargetValueList: ["A3", "A3MSN"]
                ArrayList<String> seTargetValueList = targetingInfoMap.get(seTargetKey);
                if (null == seTargetValueList || seTargetValueList.size() == 0) {
                    continue;
                }
                if (Debug) {
                    throw new IOException("xxxxxxxxxx  seTargetKey: " + seTargetKey + "; adnw enum name: "
                            + SETargetingInfoKeys.ADNW.getName());
                }
                

                if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.ADNW.getName())) {
                    resolveTargetingADNW(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AppCate.getName())) {
                    resolveTargetingAppCate(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.App.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AdSpace.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.HourTargeting.getName())) {
                    resolveTargetingHour(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.ProtocolVersion.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.DeviceBrand.getName())) {
                    resolveTargetingDeviceBrand(z1LogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.GEO.getName())) {
                    resolveTargetingGEO(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Carrier.getName())) {
                    resolveTargetingCarrier(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Network.getName())) {
                    resolveTargetingNetwork(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.startsWith(SETargetingInfoKeys.OS.getName())) {
                    resolveTargetingOS(z1LogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                }

            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx  targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }

            JSONObject orSingleCampaignInfoJSON = new JSONObject();
            // CampaignId
            orSingleCampaignInfoJSON.put(ObservReportKeys.CampaignId.getName(), seCampaignId);
            // Spent
            double spentDouble = Double.valueOf(spent);
            orSingleCampaignInfoJSON.put(ObservReportKeys.Spent.getName(), spentDouble);
            
            long startTime = 0L;
            long endTime = 0L;
            int index = 0;
            int size = logTimestampSet.size();
            if (size == 0) {
                return;
            }
            Iterator<Long> iterator = logTimestampSet.iterator();
            while (iterator.hasNext()) {
                Long item = iterator.next();
                if (index == 0) {
                    startTime = item;
                } else if (index == (size - 1)) {
                    endTime = item;
                }
                index++;
            }
            if(startTime == 0){
                endTime = System.currentTimeMillis()/1000L;
                startTime = endTime - 15*60;
            }
            if(endTime == startTime){
                endTime = startTime + 1L;
            }
            // StartTime
//            orSingleCampaignInfoJSON.put(ObservReportKeys.StartTime.getName(),
//                    conf.getLong(ObservReportKeys.StartTime.getName(), System.currentTimeMillis()/1000L));
            orSingleCampaignInfoJSON.put(ObservReportKeys.StartTime.getName(), startTime);
            if (Debug) {
                long current = conf.getLong(Constants.MIFC_STATISTIC_STARTTIME, 111111L);
                throw new IOException("xxxxxxxxxx  current: " + current);
            }
            // EndTime
//            orSingleCampaignInfoJSON.put(ObservReportKeys.EndTime.getName(), System.currentTimeMillis()/1000L);
            orSingleCampaignInfoJSON.put(ObservReportKeys.EndTime.getName(), endTime);
            // TrafficStatus
            TrafficStatus totalTrafficStatus = new TrafficStatus();
            totalTrafficStatus.setImps(showCount);
            totalTrafficStatus.setClicks(clickCount);
            totalTrafficStatus.setActivations(activationCount);
            JSONObject trafficStatusJSON = totalTrafficStatus.toJson();
            orSingleCampaignInfoJSON.put(ObservReportKeys.TrafficStatus.getName(), trafficStatusJSON);

            // Margin
            JSONObject marginJsonObj = new JSONObject();
            for (String keys : targetingAttributeValueStatsMap.keySet()) {
                TrafficStatus stats = targetingAttributeValueStatsMap.get(keys);
                String[] keysArr = StringUtils.splitPreserveAllTokens(keys, TARGETING_INFO_KEYS_DELIMETER);
                if (keysArr.length == 2) {
                    String key1 = keysArr[0];
                    if (key1.startsWith("OS")) {
                        key1 = "OS";
                    }
                    String key2 = keysArr[1];
                    JSONObject key1ValueObject = null;
                    if (!marginJsonObj.has(key1)) {
                        key1ValueObject = new JSONObject();
                        marginJsonObj.put(key1, key1ValueObject);
                    }
                    key1ValueObject = (JSONObject) marginJsonObj.get(key1);
                    key1ValueObject.put(key2, stats.toJson());
                }
            }
            if (Debug) {
                throw new IOException("xxxxxxxxxx  marginJsonObj: " + marginJsonObj + ";\\n ");
            }
            orSingleCampaignInfoJSON.put(ObservReportKeys.Margin.getName(), marginJsonObj);

            String resultStr = orSingleCampaignInfoJSON.toString();

            context.write(NullWritable.get(), new Text(resultStr));

        }

        private double generatePrice(MIFCZ1 mifcZ1) {
            String priceStr = mifcZ1.getPrice();
            double price = 0;
            try{
                price = Double.valueOf(priceStr);
            } catch(Exception e){
                e.printStackTrace();
            }
            return price;
        }

        protected long generateSellPrice(TaishanLog taishanlog) {
            int type = taishanlog.getRawLog().getType().getNumber();

            long sellingPrice = taishanlog.getRawLog().getSellingPrice();
            long clickSellingPrice = taishanlog.getRawLog().getClickSellingPrice();

            if (LogType.SHOW.getNumber() == type && clickSellingPrice > 0) {
                sellingPrice = 0;
            }

            return sellingPrice;
        }

        // the price should be reduced 1000000 times
        private String generateIncome(String sellprice) {
            //XXX
            return devide(sellprice, String.valueOf(1000000L), 10);
        }

        public static String devide(String dividend, String divisor, int precision) {
            BigDecimal dividendBD = new BigDecimal(dividend);
            BigDecimal divisorBD = new BigDecimal(divisor);
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMinimumFractionDigits(precision);
            String result = format.format(dividendBD.divide(divisorBD));
            return result.replaceAll(",", "");
        }

        /**
         * 
         * @param z1LogList   all the z1logs belong to the same campaign
         * @param targetingAttributeValueStatsMap   (key, value): (OS~Android, {clicks:100,shows:200})
         * @param seTargetKey   for example : OS
         * @param seTargetValueList   for example : [Android, WindowsPhone)
         * @throws IOException
         */
        private void resolveTargetingADNW(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus adnwStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                int activations = 0;
                for (MIFCZ1 z1log : z1LogList) {
                    String adNetworkName = z1log.getAdNetworkname();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  adNetworkName: " + adNetworkName + "; seTargetValue: "
                                + seTargetValue);
                    }
                    if (adNetworkName.equals(seTargetValue)) {
                        // log type
                        int logType = z1log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if(logType == LogType.IDIGGER.getNumber()){
                            activations++;
                        }
                    }
                }
                adnwStats.setClicks(clicks);
                adnwStats.setImps(shows);
                adnwStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        adnwStats);
            }
        }

        private void resolveTargetingAppCate(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    String appType = z1Log.getAppType();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  appType: " + appType + "; seTargetValue: " + seTargetValue);
                    }
                    if(StringUtils.isBlank(appType)){
                        continue;
                    }
                    if (appType.equalsIgnoreCase(seTargetValue)) {
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if(logType == LogType.IDIGGER.getNumber()){
                            activations++;
                        }
                    }
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }
        }

        private void resolveTargetingHour(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus adnwStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    // XXX
                    long timestamp = 0;
                    int hour = DateUtils.getHourFromTimestamp(timestamp);
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  hour: " + hour + "; seTargetValue: " + seTargetValue);
                    }
                    if (String.valueOf(hour).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  hour: " + hour);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + shows
                            + "; shows: " + clicks);
                }
                adnwStats.setClicks(clicks);
                adnwStats.setImps(shows);
                adnwStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        adnwStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        private void resolveTargetingDeviceBrand(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {

                    String deviceBrand = z1Log.getDeviceBrand();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  deviceBrand: " + deviceBrand + "; seTargetValue: "
                                + seTargetValue);
                    }
                    if (String.valueOf(deviceBrand).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  deviceBrand: " + deviceBrand);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + shows
                            + "; shows: " + clicks);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        private void resolveTargetingCarrier(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    // A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    // mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String carrier = z1Log.getCarrier();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  carrier: " + carrier + "; seTargetValue: " + seTargetValue);
                    }
                    if (String.valueOf(carrier).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  carrier: " + carrier);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
        }

        private void resolveTargetingNetwork(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    // A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    // mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String network = z1Log.getNetwork();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  network: " + network + "; seTargetValue: " + seTargetValue);
                    }
                    // network = 2 or 2G
                    if (String.valueOf(network).equals(seTargetValue) || seTargetValue.startsWith(network)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  network: " + network);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        private void resolveTargetingOS(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    // A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    // mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String os = z1Log.getOSVersion();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  os: " + os + "; seTargetValue: " + seTargetValue);
                    }

                    if (String.valueOf(os).equalsIgnoreCase(seTargetValue) || os.contains(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  os: " + os);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;  
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
        }

        private void resolveTargetingGEO(List<MIFCZ1> z1LogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            if (Debug) {
                throw new IOException("xxxxxxxxxx  enter resolveTargetingGEO, seTargetKey:" + seTargetKey + "; z1LogList.get(0):" + z1LogList.get(0));
            }
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                int activations = 0;
                for (MIFCZ1 z1Log : z1LogList) {
                    // A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    // mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    // String network = mobileAdUrl.getConnectionType();
                    // XXX
                    String ip = z1Log.getIP();
                    if(Debug){
                        ip = "117.36.52.202"; // shanxi xian
                    }
                    IPStore.IPInfo ipinfo = ipStore.getIPInfo(joinByColon("", ""), ip);
                    String regionCode = ipinfo.getRegionId();
                    String geo = regionCode;
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  geo: " + geo + "; seTargetValue: " + seTargetValue + "; z1Log: " + z1Log.getZ1jsonstr());
                    }
                    // String.valueOf(geo).equals(seTargetValue) ||
                    // seTargetValue.startsWith(geo)
                    if (!checkGEO(seTargetValue, geo)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  geo: " + geo);
                        }
                        // log type
                        int logType = z1Log.getLogType();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        } else if (logType == LogType.IDIGGER.getNumber()) {
                            activations++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                trafficStats.setActivations(activations);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
        }

        private boolean checkGEO(String seTargetValue, String geo) {
            // seTargetValue: province_CNZJ or city_CNAHAQ; geo: CNAHAQ
            if (geo.length() == 8) {
                geo = geo.substring(0, 6);
            }
            String[] seTargetValueArr = seTargetValue.split("_");
            String seTargetValuePrefix = seTargetValueArr[0];
            seTargetValue = seTargetValueArr[1];

            if (seTargetValuePrefix.equals("province")) {
                return geo.contains(seTargetValue);
            } else if (seTargetValuePrefix.equals("city")) {
                return geo.equalsIgnoreCase(seTargetValue);
            }
            return false;
        }

        protected boolean checkCampaignId(String solutionid, Map<String, MIFCSECampaignInfo> mifcSECampaignInfoMap) {
            return mifcSECampaignInfoMap.keySet().contains(solutionid);
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            super.cleanup(context);
        }

    }

    private void writeContentIntoHdfs(FileSystem fs, String dirName, String fileName, String content)
            throws IOException {
        if (content == null || content.trim().equals("")) {
            return;
        }
        Path dir = new Path(dirName);
        Path filePath = new Path(dir, fileName);
        MIFCUtils.writeContentToHDFS(fs, filePath, content);
    }

    /**
     * launch log process job
     * 
     * @param conf
     * @param querySqlConfKey
     * @param jobName
     * @param mapperclass
     * @param reducerclass
     * @param inputFormatClass
     * @return
     * @throws Exception
     */
    protected int launchLogProcessJob(Configuration conf, String querySqlConfKey, String jobName, Class mapperclass,
            Class reducerclass, Class inputFormatClass) throws Exception {
        Job job = new Job(this.getConf());
        FileSystem fs = FileSystem.get(job.getConfiguration());

        String hdfsBaseUrl = conf.get(Constants.NAMENODE);
        String jobTrackerHost = conf.get(Constants.JOB_TRACKER);
        String input = conf.get("mifc_input");
        String output = conf.get("mifc_output");
        String scribeTimestamp = conf.get(Constants.MIFC_SCRIBE_TIMESTAMP_BATH_PATH);
        int num = Integer.valueOf(conf.get(Constants.MIFC_MAPRED_REDUCE_TASKS));

        if (Debug) {
            throw new IOException("xxxxxxxxxx MIFC log process config parameters: = " + hdfsBaseUrl
                    + "; jobTrackerHost:" + jobTrackerHost + "; input:" + input + "; output:" + output
                    + "; reducer num: " + num);
        }

        // get input lzo
        Path inputPath = new Path(hdfsBaseUrl + input);
        String prefix = Constants.MIFC_PREFIX;
        Path scribeTimestampPath = new Path(hdfsBaseUrl + scribeTimestamp);
        InputProvider inputProvider = new ScribeLogInputProvider(prefix);
        inputProvider.generateMRInput(fs, inputPath, scribeTimestampPath);
        String inputLzoSBStr = inputProvider.getLzoPaths();
        String etltaskIdStr = inputProvider.getEtlTaskIds();

        if (StringUtils.isBlank(inputLzoSBStr)) {
            return 0;
        }

        if (StringUtils.isNotBlank(etltaskIdStr)) {
            writeContentIntoHdfs(fs, generateEtlTaskHDFSDirname(conf, prefix), generateEtlTaskFileName(jobName),
                    etltaskIdStr);
        }

        if (Debug) {
            return 0;
        }

        job.getConfiguration().set("fs.default.name", hdfsBaseUrl);
        job.getConfiguration().set("mapred.job.tracker", jobTrackerHost);
        job.setJarByClass(MIFCLogProcessJob.class);
        job.setJobName(jobName);
        job.setMapperClass(mapperclass);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setReducerClass(reducerclass);
        job.setNumReduceTasks(num);
        job.setInputFormatClass(inputFormatClass);

        FileInputFormat.setInputPaths(job, inputLzoSBStr);
        FileOutputFormat.setOutputPath(job, new Path(output));

        Configuration jobConf = job.getConfiguration();

        // set starttime
        jobConf.setLong(Constants.MIFC_STATISTIC_STARTTIME, System.currentTimeMillis()/1000L);

        DistributedCache.createSymlink(jobConf);

        // campaigninfo list file base path
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.MIFC_CAMPAIGNINFO_BASE_PATH,
                MIFC_CAMPAIGNINFO_LOCAL_CACHE_BASE_PATH);

        // ipstore
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.DEFAULT_IP_DB_FILENAME_PROP,
                IPSTORE_DB_LOCAL_CACHE_BASE_PATH);

        int returnCode = job.waitForCompletion(true) ? 0 : 1;

        if(0 == returnCode){
            ((ScribeLogInputProvider)inputProvider).putScribeTSMap2HDFS(fs, scribeTimestampPath);
        }

        return returnCode;

    }

    protected static final String IPSTORE_DB_LOCAL_CACHE_BASE_PATH = "mifc_ipstore";

    private String generateEtlTaskHDFSDirname(Configuration conf, String dbname) {
        return conf.get(dbname + "_" + EtlTaskInputProvider.ETLTASKID_2_HDFS_BASE_PATH);
    }

    private String generateEtlTaskFileName(String jobName) {
        return StringUtils.lowerCase(jobName) + ".txt";
    }

    public int run(String[] args) throws Exception {
        if (Debug) {
            throw new IOException("xxxxxxxxxx enter run()");
        }
        ConfigFileParser.loadMIFCProperties(args);
        Configuration conf = this.getConf();

        launchLogProcessJob(conf, "", Constants.MIFC_LOG_PROCESS_JOBNAME, MIFCCSLogProcessMapper.class,
                MIFCCSLogProcessReducer.class, TextInputFormat.class);

        return 0;
    }

    protected String getDataStr(String path_filename) {
        String[] arr = path_filename.split("/");
        return arr[arr.length - 2];
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            String usage = MIFCLogProcessJob.class.getSimpleName() + "<in> <out>";
            System.out.println(usage);
            return;
        }

        int res = ToolRunner.run(new MIFCLogProcessJob(), args);
        System.exit(res);
    }
}

