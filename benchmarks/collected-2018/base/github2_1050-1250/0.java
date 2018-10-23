// https://searchcode.com/api/result/75875207/

package com.allyes.mifcor;

import com.allyes.carpenter.CarpenterLogs.LogType;
import com.allyes.carpenter.CarpenterLogs.RawLog;
import com.allyes.mifcor.constants.A3DBConstants;
import com.allyes.mifcor.constants.Constants;
import com.allyes.mifcor.data.A3MobileAdURL;
import com.allyes.mifcor.data.MIFCSECampaignInfo;
import com.allyes.mifcor.data.ObservReportKeys;
import com.allyes.mifcor.data.SETargetingInfoKeys;
import com.allyes.mifcor.data.TrafficStatus;
import com.allyes.mifcor.utils.ConfigFileParser;
import com.allyes.mifcor.utils.DateUtils;
import com.allyes.mifcor.utils.MIFCETLTaskUtils;
import com.allyes.mifcor.utils.MIFCHadoopUtils;
import com.allyes.mifcor.utils.MIFCUtils;
import com.allyes.taishan.TaishanLogs.TaishanLog;
import com.allyes.taishan.mapreduce.input.LzoTaishanLogProtobufB64LineInputFormat;
import com.allyes.taishan.mapreduce.io.ProtobufTaishanLogWritable;
import com.twitter.elephantbird.mapreduce.io.ProtobufWritable;

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

public class MIFCDELogProcessJob extends Configured implements Tool {

    private static Log LOG = LogFactory.getLog(MIFCDELogProcessJob.class);
    protected static final String MIFC_CAMPAIGNINFO_LOCAL_CACHE_BASE_PATH = "mifc_campaigninfo";

    private static boolean Debug = false;

    public static class MIFCCSDELogProcessMapper extends
            Mapper<LongWritable, ProtobufWritable<TaishanLog>, Text, ProtobufTaishanLogWritable> {

        @Override
        protected void map(LongWritable key, ProtobufWritable<TaishanLog> value, Context context) throws IOException,
                InterruptedException {
            value.setConverter(TaishanLog.class);
            TaishanLog taishanLog = value.get();

            A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
            mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
            String snStr = mobileAdUrl.getUserId();
            if(StringUtils.isBlank(snStr)){
                return;
            }

            Text mapOutKey = generateMapOutKey(taishanLog);
            ProtobufTaishanLogWritable newValue = new ProtobufTaishanLogWritable(taishanLog);

            context.write(mapOutKey, newValue);
        }

        private Text generateMapOutKey(TaishanLog taishanLog) {
            RawLog rawlog = taishanLog.getRawLog();
            String solutionid = rawlog.getSolutionId();
            return new Text(solutionid);
        }

    }

    public static class MIFCCSDELogProcessReducer extends AbstractLogProcessReducer<ProtobufTaishanLogWritable> {
        private static final String TARGETING_INFO_KEYS_DELIMETER = "~";

        @Override
        protected void reduce(Text key, Iterable<ProtobufTaishanLogWritable> values, Context context)
                throws IOException, InterruptedException {
            Configuration conf = context.getConfiguration();

            // key is A3's solutionid. and adnwCampaign is solution
            String adnwCampaignId = key.toString();
            if (Debug && adnwCampaignId.equals("196")) {
                throw new IOException("----------------adnwCampaignId : " + adnwCampaignId);
            }
            if (Debug) {
                StringBuilder sb = new StringBuilder();
                for (String adnwCampaignid : mifcSECampaignInfoMap_.keySet()) {
                    sb.append(adnwCampaignid).append(",");
                }
                throw new IOException("----------------adnwCampaignIds : " + sb.toString());
            }

            // validate campaignid exists
            if (!checkCampaignId(adnwCampaignId, mifcSECampaignInfoMap_)) {
                // throw new
                // IOException("xxxxxxxxxx  campaignid doesn't exist: " +
                // solutionid + "; mifcSECampaignInfoMap_ keysets:  " +
                // mifcSECampaignInfoMap_.keySet());
                // LOG.error("---------------key.toString():" + key.toString());
                return;
            }

            String seCampaignId = getSECampaignId(adnwCampaignId, mifcSECampaignInfoMap_);
            MIFCSECampaignInfo mifcSECampaignInfo = mifcSECampaignInfoMap_.get(adnwCampaignId);

            // targetinfo attribute value's stats map
            Map<String, TrafficStatus> targetingAttributeValueStatsMap = new HashMap<String, TrafficStatus>();

            // count targetingspace's stats
            Map<String, ArrayList<String>> targetingInfoMap = mifcSECampaignInfo.getTargetingInfo();
            if (Debug) {
                throw new IOException("xxxxxxxxxx  targetingInfoMap: " + targetingInfoMap);
            }

            List<TaishanLog> taishanLogList = new ArrayList<TaishanLog>();
            Set<Long> logTimestampSet = new TreeSet<Long>();
            // count total stats: clicks ,shows, ..., besides count the total
            // cost
            int showCount = 0;
            int clickCount = 0;
            long totalCost = 0;
            for (ProtobufTaishanLogWritable value : values) {
                ProtobufWritable<TaishanLog> valuePW = (ProtobufWritable<TaishanLog>) value;
                TaishanLog taishanLog = valuePW.get();
                taishanLogList.add(taishanLog);
                if (Debug) {
                    String url = taishanLog.getRawLog().getRequestUrl();
                    throw new IOException("xxxxxxxxxx  url: " + url);
                }

                // log type
                int logType = taishanLog.getRawLog().getType().getNumber();
                if (logType == LogType.SHOW.getNumber()) {
                    showCount++;
                } else if (logType == LogType.CLICK.getNumber()) {
                    clickCount++;
                }

                long cost = generateSellPrice(taishanLog);
                totalCost += cost;
                logTimestampSet.add(taishanLog.getRawLog().getTimestamp());

                // collect margin info
//                collectMarginInfo(targetingInfoMap, taishanLog, targetingAttributeValueStatsMap);
            }

            // resolve totalCost
            String spent = generateIncome(String.valueOf(totalCost));

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
                    resolveTargetingADNW(taishanLogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AppCate.getName())) {
                    // resolveTargetingAppCate(taishanLogList,
                    // targetingAttributeValueStatsMap, seTargetKey,
                    // seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.App.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AdSpace.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.HourTargeting.getName())) {
                    resolveTargetingHour(taishanLogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.ProtocolVersion.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.DeviceBrand.getName())) {
                    resolveTargetingDeviceBrand(taishanLogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.GEO.getName())) {
                    resolveTargetingGEO(taishanLogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Carrier.getName())) {
                    resolveTargetingCarrier(taishanLogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Network.getName())) {
                    resolveTargetingNetwork(taishanLogList, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.startsWith(SETargetingInfoKeys.OS.getName())) {
                    resolveTargetingOS(taishanLogList, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                }

            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx  targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
            if (Debug) {
                throw new IOException("xxxxxxxxxx  logTimestampSet's size: " + logTimestampSet.size());
            }

//            Object[]  ogTimestampArr = logTimestampSet.toArray();
//            long startTime = (Long)ogTimestampArr[0];
//            long endTime = (Long)ogTimestampArr[ogTimestampArr.length -1];

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

            JSONObject orSingleCampaignInfoJSON = new JSONObject();
            // CampaignId
            orSingleCampaignInfoJSON.put(ObservReportKeys.CampaignId.getName(), seCampaignId);
            // Spent
            double spentDouble = Double.valueOf(spent);
            orSingleCampaignInfoJSON.put(ObservReportKeys.Spent.getName(), spentDouble);
            // StartTime
//            orSingleCampaignInfoJSON.put(ObservReportKeys.StartTime.getName(),
//                    conf.getLong(ObservReportKeys.StartTime.getName(), System.currentTimeMillis() / 1000L));
            orSingleCampaignInfoJSON.put(ObservReportKeys.StartTime.getName(),startTime);

            if (Debug) {
                long current = conf.getLong(Constants.MIFC_STATISTIC_STARTTIME, 111111L);
                throw new IOException("xxxxxxxxxx  current: " + current);
            }
            // EndTime
//            orSingleCampaignInfoJSON.put(ObservReportKeys.EndTime.getName(), System.currentTimeMillis() / 1000L);
            orSingleCampaignInfoJSON.put(ObservReportKeys.EndTime.getName(), endTime);

            // TrafficStatus
            TrafficStatus totalTrafficStatus = new TrafficStatus();
            totalTrafficStatus.setImps(showCount);
            totalTrafficStatus.setClicks(clickCount);
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

        private void collectMarginInfo(Map<String, ArrayList<String>> targetingInfoMap, TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap) throws IOException {
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
                    resolveTargetingADNW(taishanLog, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AppCate.getName())) {
                    // do nothing
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.App.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.AdSpace.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.HourTargeting.getName())) {
                    resolveTargetingHour(taishanLog, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.ProtocolVersion.getName())) {

                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.DeviceBrand.getName())) {
                    resolveTargetingDeviceBrand(taishanLog, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.GEO.getName())) {
                    resolveTargetingGEO(taishanLog, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Carrier.getName())) {
                    resolveTargetingCarrier(taishanLog, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.equalsIgnoreCase(SETargetingInfoKeys.Network.getName())) {
                    resolveTargetingNetwork(taishanLog, targetingAttributeValueStatsMap, seTargetKey,
                            seTargetValueList);
                } else if (seTargetKey.startsWith(SETargetingInfoKeys.OS.getName())) {
                    resolveTargetingOS(taishanLog, targetingAttributeValueStatsMap, seTargetKey, seTargetValueList);
                }

            }
            
        }

        private void resolveTargetingOS(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingNetwork(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingCarrier(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingGEO(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingDeviceBrand(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingHour(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private void resolveTargetingADNW(TaishanLog taishanLog,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) {
            // TODO Auto-generated method stub
            
        }

        private long generateSellPrice(TaishanLog taishanlog) {
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

        private void resolveTargetingADNW(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus adnwStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    String dbName = taishanLog.getRawLog().getDbName();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  dbName: " + dbName + "; seTargetValue: " + seTargetValue);
                    }
                    if (checkA3DBname(dbName, seTargetValue)) {
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                adnwStats.setClicks(clicks);
                adnwStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        adnwStats);
            }
        }

        private boolean checkA3DBname(String dbName, String seTargetValue) {
            /*
             * dbName : a3, a3test, a3mstest, a3mstest93 ADNW : A3, A3MSN
             */
            String upperDBName = StringUtils.upperCase(dbName);
            if (upperDBName.startsWith(A3DBConstants.A3MS_DB_NAME_UPPER_PREFIX)) {
                return seTargetValue.equals(A3DBConstants.A3MS_ADNW_NAME);
            } else if (upperDBName.startsWith(A3DBConstants.A3_DB_NAME_UPPER_PREFIX)) {
                return seTargetValue.equals(A3DBConstants.A3_ADNW_NAME);
            }
            return false;
        }

        private void resolveTargetingAppCate(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    String appType = taishanLog.getRawLog().getAppType().name();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  appType: " + appType + "; seTargetValue: " + seTargetValue);
                    }
                    if (appType.equalsIgnoreCase(seTargetValue)) {
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }
        }

        private void resolveTargetingHour(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            if (seTargetValueList == null) {
                return;
            }
            // iterate seTargetValueList : 0,1,2,3,...,22,23
            if (checkTargetAllValues(seTargetValueList)) {
                // seTargetValueList = null;
            }

            for (String seTargetValue : seTargetValueList) {
                TrafficStatus adnwStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    long timestamp = taishanLog.getRawLog().getTimestamp();
                    int hour = DateUtils.getHourFromTimestamp(timestamp);
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  hour: " + hour + "; seTargetValue: " + seTargetValue);
                    }
                    if (String.valueOf(hour).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  hour: " + hour);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + shows
                            + "; shows: " + clicks);
                }
                adnwStats.setClicks(clicks);
                adnwStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        adnwStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        /**
         * check if target space is "AllValues"
         * 
         * @param seTargetValueList
         * @return
         */
        protected boolean checkTargetAllValues(ArrayList<String> seTargetValueList) {
            if (seTargetValueList != null && seTargetValueList.size() == 1) {
                return seTargetValueList.get(0).equals("AllValues");
            }
            return false;
        }

        private void resolveTargetingDeviceBrand(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            // iterate seTargetValueList : "A3", "A3MSN"
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int shows = 0;
                int clicks = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String deviceBrand = mobileAdUrl.getBrand();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  devicebrand: " + deviceBrand + "; seTargetValue: "
                                + seTargetValue);
                    }
                    if (String.valueOf(deviceBrand).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  devicebrand: " + deviceBrand);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + shows
                            + "; shows: " + clicks);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        private void resolveTargetingCarrier(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String carrier = mobileAdUrl.getOperator();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  carrier: " + carrier + "; seTargetValue: " + seTargetValue);
                    }
                    if (String.valueOf(carrier).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  carrier: " + carrier);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
        }

        private void resolveTargetingNetwork(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String network = mobileAdUrl.getConnectionType();
                    network = transferNetwork2TaxomyValue(network);
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  network: " + network + "; seTargetValue: " + seTargetValue);
                    }
                    if (String.valueOf(network).equals(seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  network: " + network);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);

            }
        }

        private String transferNetwork2TaxomyValue(String network) {
            String taxonomyNetwork = network;
            // 0:2G ; 1:3G; 2:WIFI
            String networkTrim = network.trim();
            if(networkTrim.equals("0")){
                taxonomyNetwork = "2G";
            } else if(networkTrim.equals("1")){
                taxonomyNetwork = "3G";
            } else if(networkTrim.equals("2")){
                taxonomyNetwork = "WIFI";
            }
            return taxonomyNetwork;
        }

        private void resolveTargetingOS(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    String os = mobileAdUrl.getOs();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  os: " + os + "; seTargetValue: " + seTargetValue);
                    }

                    if (checkOS(os, seTargetValue)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  os: " + os);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
                targetingAttributeValueStatsMap.put(seTargetKey + TARGETING_INFO_KEYS_DELIMETER + seTargetValue,
                        trafficStats);
            }

            if (Debug) {
                throw new IOException("xxxxxxxxxx targetingAttributeValueStatsMap: " + targetingAttributeValueStatsMap);
            }
        }

        private boolean checkOS(String os, String seTargetValue) {
            if(os.replace("microsoft windows ce", "Windows Phone").equalsIgnoreCase(seTargetValue)){
                return true;
            }
            return false;
        }

        private void resolveTargetingGEO(List<TaishanLog> taishanLogList,
                Map<String, TrafficStatus> targetingAttributeValueStatsMap, String seTargetKey,
                ArrayList<String> seTargetValueList) throws IOException {
            for (String seTargetValue : seTargetValueList) {
                TrafficStatus trafficStats = new TrafficStatus();
                int clicks = 0;
                int shows = 0;
                for (TaishanLog taishanLog : taishanLogList) {
                    // A3MobileAdURL mobileAdUrl = new A3MobileAdURL();
                    // mobileAdUrl.parse(taishanLog.getRawLog().getRequestUrl().toLowerCase());
                    // String network = mobileAdUrl.getConnectionType();
                    String geo = taishanLog.getRegionId();
                    if (Debug) {
                        throw new IOException("xxxxxxxxxx  geo: " + geo + "; seTargetValue: " + seTargetValue);
                    }
                    // String.valueOf(geo).equals(seTargetValue) ||
                    // seTargetValue.startsWith(geo)
                    if (!checkGEO(seTargetValue, geo)) {
                        if (Debug) {
                            throw new IOException("xxxxxxxxxx  geo: " + geo);
                        }
                        // log type
                        int logType = taishanLog.getRawLog().getType().getNumber();
                        if (logType == LogType.SHOW.getNumber()) {
                            shows++;
                        } else if (logType == LogType.CLICK.getNumber()) {
                            clicks++;
                        }
                    }
                }
                if (Debug && seTargetValue.equals("5")) {
                    throw new IOException("xxxxxxxxxx seTargetValue: " + seTargetValue + ";clicks: " + clicks
                            + "; shows: " + shows);
                }
                trafficStats.setClicks(clicks);
                trafficStats.setImps(shows);
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
        // fs.deleteOnExit(filePath);
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
    protected int launchDELogProcessJob(Configuration conf, String querySqlConfKey, String jobName, Class mapperclass,
            Class reducerclass, Class inputFormatClass) throws Exception {
        Job job = new Job(this.getConf());
        FileSystem fs = FileSystem.get(job.getConfiguration());

        String hdfsBaseUrl = conf.get(Constants.NAMENODE);
        String jobTrackerHost = conf.get(Constants.JOB_TRACKER);
        String output = conf.get("a3mifc_output");
        int num = Integer.valueOf(conf.get(Constants.MIFC_DE_MAPRED_REDUCE_TASKS));

        if (Debug) {
            throw new IOException("xxxxxxxxxx MIFC log process config parameters: = " + hdfsBaseUrl
                    + ";jobTrackerHost:" + jobTrackerHost + ";output:" + output + "reducer num: " + num);
        }

        // get input lzo
        String prefix = Constants.MIFC_A3_PREFIX;
        InputProvider inputProvider = new EtlTaskInputProvider(prefix);
        inputProvider.generateMRInput(fs, conf, null);
        String inputLzoSBStr = inputProvider.getLzoPaths();
        String etltaskIdStr = inputProvider.getEtlTaskIds();

        if (Debug) {
            throw new IOException("xxxxxxxxxx MIFC DE log process inputLzoSBStr= " + inputLzoSBStr + "; etltaskIdStr="
                    + etltaskIdStr);
        }

        if (StringUtils.isBlank(inputLzoSBStr)) {
            return 0;
        }

        if (StringUtils.isNotBlank(etltaskIdStr)) {
            writeContentIntoHdfs(fs, generateEtlTaskHDFSDirname(conf, prefix), generateEtlTaskFileName(jobName),
                    etltaskIdStr);
        }

        job.getConfiguration().set("fs.default.name", hdfsBaseUrl);
        job.getConfiguration().set("mapred.job.tracker", jobTrackerHost);
        job.setJarByClass(MIFCDELogProcessJob.class);
        job.setJobName(jobName);
        job.setMapperClass(mapperclass);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ProtobufTaishanLogWritable.class);
        job.setReducerClass(reducerclass);
        job.setNumReduceTasks(num);
        job.setInputFormatClass(inputFormatClass);

        FileInputFormat.setInputPaths(job, inputLzoSBStr);
        FileOutputFormat.setOutputPath(job, new Path(output));

        Configuration jobConf = job.getConfiguration();

        // set starttime
        jobConf.setLong(Constants.MIFC_STATISTIC_STARTTIME, System.currentTimeMillis() / 1000L);

        DistributedCache.createSymlink(jobConf);

        if (Debug) {
            String testCampaignPath = jobConf.get(Constants.MIFC_CAMPAIGNINFO_BASE_PATH);
            throw new IOException("xxxxxxxxxx MIFC DE log testCampaignPath= " + testCampaignPath);
        }
        // campaigninfo list file base path
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.MIFC_CAMPAIGNINFO_BASE_PATH,
                MIFC_CAMPAIGNINFO_LOCAL_CACHE_BASE_PATH);

        // ipstore
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.DEFAULT_IP_DB_FILENAME_PROP,
                IPSTORE_DB_LOCAL_CACHE_BASE_PATH);

        return job.waitForCompletion(true) ? 0 : 1;

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
    protected int launchDELogProcessJobDirectedly(Configuration conf, String querySqlConfKey, String jobName,
            Class mapperclass, Class reducerclass, Class inputFormatClass) throws Exception {
        Job job = new Job(this.getConf());
        FileSystem fs = FileSystem.get(job.getConfiguration());

        String hdfsBaseUrl = conf.get(Constants.NAMENODE);
        String jobTrackerHost = conf.get(Constants.JOB_TRACKER);
        String input = conf.get("a3mifc_input");
        String output = conf.get("a3mifc_output");
        int reduceTaskNum = Integer.valueOf(conf.get(Constants.MIFC_DE_MAPRED_REDUCE_TASKS));

        if (Debug) {
            throw new IOException("xxxxxxxxxx MIFC log process config parameters: = " + hdfsBaseUrl
                    + ";jobTrackerHost:" + jobTrackerHost + "; output:" + output + "; reducer num: " + reduceTaskNum);
        }

        // get input lzo
        Path inputPath = new Path(hdfsBaseUrl + input);
        String prefix = Constants.MIFC_A3_PREFIX;
        InputProvider inputProvider = new CarpenterLogInputProvider(prefix);
        inputProvider.generateMRInput(fs, conf, inputPath);
        String inputLzoSBStr = inputProvider.getLzoPaths();
        if(Debug){
            throw new IOException("xxxxxxxxxx inputLzoStr: " + inputLzoSBStr);
        }

        if (StringUtils.isBlank(inputLzoSBStr)) {
            return 0;
        }

        job.getConfiguration().set("fs.default.name", hdfsBaseUrl);
        job.getConfiguration().set("mapred.job.tracker", jobTrackerHost);
        job.setJarByClass(MIFCDELogProcessJob.class);
        job.setJobName(jobName);
        job.setMapperClass(mapperclass);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(ProtobufTaishanLogWritable.class);
        job.setReducerClass(reducerclass);
        job.setNumReduceTasks(reduceTaskNum);
        job.setInputFormatClass(inputFormatClass);

        FileInputFormat.setInputPaths(job, inputLzoSBStr);
        FileOutputFormat.setOutputPath(job, new Path(output));

        Configuration jobConf = job.getConfiguration();

        // set starttime
        jobConf.setLong(Constants.MIFC_STATISTIC_STARTTIME, System.currentTimeMillis() / 1000L);

        DistributedCache.createSymlink(jobConf);

        if (Debug) {
            String testCampaignPath = jobConf.get(Constants.MIFC_CAMPAIGNINFO_BASE_PATH);
            throw new IOException("xxxxxxxxxx MIFC DE log testCampaignPath= " + testCampaignPath);
        }
        // campaigninfo list file base path
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.MIFC_CAMPAIGNINFO_BASE_PATH,
                MIFC_CAMPAIGNINFO_LOCAL_CACHE_BASE_PATH);

        // ipstore
        MIFCUtils.addDistributedCacheFile(job, fs, Constants.DEFAULT_IP_DB_FILENAME_PROP,
                IPSTORE_DB_LOCAL_CACHE_BASE_PATH);

        return job.waitForCompletion(true) ? 0 : 1;
    }

    protected static final String MIFC_DE_SOLUTION_TO_CAMPAIGN_LOCAL_CACHE_BASE_PATH = "a3_solution2campaign";
    protected static final String IPSTORE_DB_LOCAL_CACHE_BASE_PATH = "mifc_ipstore";

    private String generateEtlTaskHDFSDirname(Configuration conf, String prefix) {
        return conf.get(prefix + EtlTaskInputProvider.ETLTASKID_2_HDFS_BASE_PATH);
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

        // int resultCode = launchDELogProcessJob(conf, "",
        // Constants.MIFC_DE_LOG_PROCESS_JOBNAME,
        // MIFCCSDELogProcessMapper.class,
        // MIFCCSDELogProcessReducer.class,
        // LzoTaishanLogProtobufB64LineInputFormat.class);
        // if(resultCode == 0){
        // // update etl_task
        // String prefix = Constants.MIFC_A3_PREFIX;
        // String url = conf.get(prefix + EtlTaskInputProvider.ETLTASK_CONNECT);
        // String user = conf.get(prefix +
        // EtlTaskInputProvider.ETLTASK_USERNAME);
        // String password = conf.get(prefix +
        // EtlTaskInputProvider.ETLTASK_PASSWORD);
        // String sql = conf.get(prefix +
        // EtlTaskInputProvider.ETLTASK_UPDATE_SQL);
        // String etlTaskIds = getEtltaskIds(conf, prefix);
        // if(StringUtils.isBlank(etlTaskIds)) {
        // return 0;
        // }
        // sql = sql.replace("?", etlTaskIds);
        // MIFCETLTaskUtils.execute(url, user, password, sql);
        // }
        // int resultCode = launchDELogProcessJob(conf, "",
        // Constants.MIFC_DE_LOG_PROCESS_JOBNAME,
        // MIFCCSDELogProcessMapper.class,
        // MIFCCSDELogProcessReducer.class,
        // LzoTaishanLogProtobufB64LineInputFormat.class);
        int resultCode = launchDELogProcessJobDirectedly(conf, "", Constants.MIFC_DE_LOG_PROCESS_JOBNAME, MIFCCSDELogProcessMapper.class, MIFCCSDELogProcessReducer.class, LzoTaishanLogProtobufB64LineInputFormat.class);
        if (resultCode == 0) {
              // update etl_task
//            String prefix = Constants.MIFC_A3_PREFIX;
//            String url = conf.get(prefix + EtlTaskInputProvider.ETLTASK_CONNECT);
//            String user = conf.get(prefix + EtlTaskInputProvider.ETLTASK_USERNAME);
//            String password = conf.get(prefix + EtlTaskInputProvider.ETLTASK_PASSWORD);
//            String sql = conf.get(prefix + EtlTaskInputProvider.ETLTASK_UPDATE_SQL);
//            String etlTaskIds = getEtltaskIds(conf, prefix);
//            if (StringUtils.isBlank(etlTaskIds)) {
//                return 0;
//            }
//            sql = sql.replace("?", etlTaskIds);
//            MIFCETLTaskUtils.execute(url, user, password, sql);

            // update latestLzoBathPath : ./mifc_or/utils/a3mifc_latest_lzo
//            String prefix = Constants.MIFC_A3_PREFIX;
//            String hdfsBaseUrl = conf.get(Constants.NAMENODE);
//            String latestLzoBathPathStr = conf.get(prefix + CarpenterLogInputProvider.LASTEST_LZO_BASE_PATH);
//            Path latestLzoBathPath = new Path(hdfsBaseUrl + latestLzoBathPathStr);
//            Path lastestLzoFileLocationPath = new Path(latestLzoBathPath, CarpenterLogInputProvider.LATEST_LZO_FILENAME);
//            String latestLzoBathPathStrTemp = conf.get(prefix + CarpenterLogInputProvider.LASTEST_LZO_BASE_PATH_TEMP);
//            Path latestLzoBathPathTemp = new Path(hdfsBaseUrl + latestLzoBathPathStrTemp);
//            Path lastestLzoFileLocationPathTemp = new Path(latestLzoBathPathTemp, CarpenterLogInputProvider.LATEST_LZO_FILENAME);
        }

        return 0;
    }

    private String getEtltaskIds(Configuration conf, String prefix) throws IOException {
        String hdfsHostUrl = conf.get("fs.default.name");
        String etltaskidHDFSPath = conf.get(prefix + EtlTaskInputProvider.ETLTASKID_2_HDFS_BASE_PATH);
        Path inputPath = new Path(hdfsHostUrl + etltaskidHDFSPath);
        List<String> etltaskList = MIFCHadoopUtils.loadLines(conf, inputPath);
        if (Debug) {
            throw new IOException("xxxxxxxxxx etltaskList's size =" + etltaskList.size());
        }
        if (Debug) {
            throw new IOException("xxxxxxxxxx etltaskList =" + etltaskList.get(0));
        }
        if (etltaskList != null && !etltaskList.isEmpty()) {
            return etltaskList.get(0);
        }
        return null;
    }

    protected String getDataStr(String path_filename) {
        String[] arr = path_filename.split("/");
        return arr[arr.length - 2];
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            String usage = MIFCDELogProcessJob.class.getSimpleName() + "<in> <out>";
            System.out.println(usage);
            return;
        }

        int res = ToolRunner.run(new MIFCDELogProcessJob(), args);
        System.exit(res);
    }
}

