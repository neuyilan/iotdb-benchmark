package cn.edu.tsinghua.iotdb.benchmark.tsdb.summarystore;

import com.samsung.sra.datastore.*;
import com.samsung.sra.datastore.ingest.CountBasedWBMH;
import com.samsung.sra.datastore.aggregates.*;
import cn.edu.tsinghua.iotdb.benchmark.conf.Config;
import cn.edu.tsinghua.iotdb.benchmark.conf.ConfigDescriptor;
import cn.edu.tsinghua.iotdb.benchmark.measurement.Status;
import cn.edu.tsinghua.iotdb.benchmark.tsdb.IDatabase;
import cn.edu.tsinghua.iotdb.benchmark.tsdb.TsdbException;
import cn.edu.tsinghua.iotdb.benchmark.workload.ingestion.Batch;
import cn.edu.tsinghua.iotdb.benchmark.workload.ingestion.Record;
import cn.edu.tsinghua.iotdb.benchmark.workload.query.impl.*;
import cn.edu.tsinghua.iotdb.benchmark.workload.schema.DeviceSchema;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SummaryStoreDB implements IDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(cn.edu.tsinghua.iotdb.benchmark.tsdb.opentsdb.OpenTSDB.class);
    private static Config config = ConfigDescriptor.getInstance().getConfig();
    private SummaryStore store;
    private Windowing windowing;
    private String storeLoc = "./tdstore";
    private long streamNum = 0;
    private Map<String, Long> groupIDMap = new HashMap<>();
    private CountBasedWBMH wbmh;

    /**
     * constructor.
     */
    public SummaryStoreDB() {
        config = ConfigDescriptor.getInstance().getConfig();
    }

    @Override
    public void init() throws TsdbException {
        try {
            //System.out.println("1");
            store = new SummaryStore(storeLoc, new SummaryStore.StoreOptions().setKeepReadIndexes(true));
            //System.out.println("2");
            windowing = new RationalPowerWindowing(config.SS_P, config.SS_Q, config.SS_R, config.SS_S);
            wbmh = new CountBasedWBMH(windowing).setBufferSize(10000);
            //System.out.println("groupIDMAP= " + groupIDMap);
        } catch (Exception e) {
            throw new TsdbException(
                    "Init SummaryStoreDB client failed, the Message is " + e.getMessage());
        }
    }

    // no need for summaryStoreDB
    @Override
    public void cleanup() throws TsdbException {
        /*
        try {
            System.out.println("Delete");
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm -rf " + storeLoc}).waitFor();
        } catch (Exception e) {
            throw new TsdbException(
                    "Cleanup SummaryStoreDB client failed, the Message is " + e.getMessage());
        }*/
    }

    @Override
    public void registerSchema(List<DeviceSchema> schemaList) throws TsdbException {
    }

    @Override
    public Status insertOneBatch(Batch batch) {
        // create dataModel
        try {
            DeviceSchema schema = batch.getDeviceSchema();
            String groupName = schema.getGroup();
            if (!groupIDMap.containsKey(groupName)){
                groupIDMap.put(groupName, streamNum);
                store.registerStream(streamNum, wbmh,
                        new SimpleCountOperator(),
                        new MaxOperator(),
                        new MinOperator(),
                        new SumOperator());
                List<Record> records = batch.getRecords();
                for (Record record : records) {
                    //System.out.println("recordValue=" + record.getRecordDataValue());
                    Object dataValue = castValue(record.getRecordDataValue().get(0), config.DATA_TYPE);
                    store.append(streamNum, record.getTimestamp(), dataValue);
                }
                streamNum += 1;
            } else {
                long streamID = groupIDMap.get(schema.getGroup());
                List<Record> records = batch.getRecords();
                for (Record record : records) {
                    //System.out.println("record=" + record);
                    //System.out.println("recordValue=" + record.getRecordDataValue());
                    Object dataValue = castValue(record.getRecordDataValue().get(0), config.DATA_TYPE);
                    store.append(streamID, record.getTimestamp(), dataValue);
                }
            }
            return new Status(true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Status(false, 0, e, e.toString());
        }
    }

    @Override
    public Status preciseQuery(PreciseQuery preciseQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }

    @Override
    public Status rangeQuery(RangeQuery rangeQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }

    @Override
    public Status valueRangeQuery(ValueRangeQuery valueRangeQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }


    @Override
    public Status aggRangeQuery(AggRangeQuery aggRangeQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }


    @Override
    public Status aggValueQuery(AggValueQuery aggValueQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }


    @Override
    public Status aggRangeValueQuery(AggRangeValueQuery aggRangeValueQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }


    @Override
    public Status groupByQuery(GroupByQuery groupByQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }


    @Override
    public Status latestPointQuery(LatestPointQuery latestPointQuery) {
        Exception e = new TsdbException("OpenTSDB don't support this kind of query");
        return new Status(false, 0, e, e.getMessage());
    }

    @Override
    public void close() throws TsdbException {
        try {
            for (Long value : groupIDMap.values()) {
                store.unloadStream(value);
            }
            store.close();
        } catch (Exception e){
            throw new TsdbException(
                    "Close SummaryStoreDB client failed, the Message is " + e.getMessage());
        }
    }

    private Status executeQueryAndGetStatus(String sql, boolean isLatestPoint) {
        return new Status(true, 0, null, sql);
    }

    private int getOneQueryPointNum(String str, boolean isLatestPoint) {
        return 0;
    }

    private List<Map<String, Object>> getSubQueries(List<DeviceSchema> devices, String aggreFunc) {
        return null;
    }

    private Object castValue(String value, String dataType) {
        if(dataType.equals("LONG")){
            //System.out.println(Double.valueOf(value).longValue());
            return Double.valueOf(value).longValue();
        } else if (dataType.equals("FLOAT")) {
            return Float.valueOf(value);
        } else if (dataType.equals("DOUBLE")) {
            return Double.valueOf(value);
        } else { //The value is String
            return value;
        }
    }
}
