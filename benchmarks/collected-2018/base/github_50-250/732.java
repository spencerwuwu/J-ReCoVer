// https://searchcode.com/api/result/75654715/

package com.zanni.rte.framework.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.mongodb.DBObject;
import com.zanni.rte.framework.MixEnergyRegional;
import com.zanni.rte.framework.utils.ArrayAggregationEnum;
import com.zanni.rte.framework.utils.MixEnergyRegionalFieldAggregateEnum;
import com.zanni.rte.framework.utils.RegionEnum;
import com.zanni.rte.framework.utils.TimedAggregateEnum;

@Service
public class MixEnergyRegionalAggregateServiceImpl implements
		MixEnergyRegionalAggregateService {

	@Resource
	private MongoTemplate template;

	private class MixEnergyRegionalValueObject {
		private Object id;
		private MixEnergyRegional value;

		@SuppressWarnings("unused")
		public Object getId() {
			return id;
		}

		public MixEnergyRegional getValue() {
			return value;
		}

		@SuppressWarnings("unused")
		public void setValue(MixEnergyRegional value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "ValueObject [id=" + id + ", value=" + value + "]";
		}
	}

	private String genericMap(String dateIndex, List<String> fields) {
		StringBuilder fields_str = new StringBuilder();
		boolean first = true;
		for (String field : fields) {
			if (first)
				first = false;
			else
				fields_str.append(",");
			fields_str.append(field);
			fields_str.append(":this.");
			fields_str.append(field);
		}
		StringBuilder map = new StringBuilder();
		map.append("function(){var date = new Date();date.setTime(this.logDate);var temp = new Date(");
		map.append(dateIndex);
		map.append(");emit({date:temp, region:this.region}, {  logDate:temp,");
		map.append(fields_str);
		map.append("});};");
		return map.toString();
	}

	private String mapYear(List<String> field) {
		return this.genericMap("date.getFullYear()", field);
	}

	private String mapMonth(List<String> field) {
		return this.genericMap("date.getFullYear(), date.getMonth()", field);
	}

	private String mapDay(List<String> field) {
		return this.genericMap(
				"date.getFullYear(), date.getMonth(), date.getDate()", field);
	}

	private String mapHour(List<String> field) {
		return this
				.genericMap(
						"date.getFullYear(), date.getMonth(), date.getDate(), date.getHours()",
						field);
	}

	private String mapMinutes(List<String> field, int modulo) {
		return this
				.genericMap(
						"date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes()",
						field);
	}

	private String reduce(List<String> fields) {
		// field0 = 0, ..., fieldN = 0
		boolean first = true;
		StringBuilder fields_init = new StringBuilder();
		// field0+=val.field0, ..., fieldN+=val.fieldN
		StringBuilder fields_agg = new StringBuilder();
		// field0: X, ..., fieldN: X
		// with X : IF SUM fieldN IF MEAN fieldN/value.length
		StringBuilder fields_return = new StringBuilder();
		for (String field : fields) {
			if (first)
				first = false;
			else {
				fields_return.append(",");
			}
			
			fields_init.append("var ");
			fields_init.append(field);
			fields_init.append("=0;");
			fields_agg.append(field);
			fields_agg.append("+=val.");
			fields_agg.append(field);
			fields_agg.append(";");
			fields_return.append(field);
			fields_return.append(":");

			MixEnergyRegionalFieldAggregateEnum fromAbbr = MixEnergyRegionalFieldAggregateEnum
					.fromAbbr(field);
			if (fromAbbr.getAggregate().equals(ArrayAggregationEnum.MEAN)) {
				fields_return.append("Math.round((");
				fields_return.append(field);
				fields_return.append("/");
				fields_return.append("value.length)*100");
				fields_return.append(")/100");
			} else {
				fields_return.append(field);
			}
		}
		StringBuilder reduce = new StringBuilder();
		reduce.append("function(key, value){");
		reduce.append(fields_init);
		reduce.append("value.forEach(function(val){");
		reduce.append(fields_agg);
		reduce.append("});return  {region: key.region, logDate: key.date,");
		reduce.append(fields_return);
		reduce.append("}};");
		return reduce.toString();
	}

	private List<MixEnergyRegional> genericAggregat(Date startDate,
			Date endDate, String map, String reduce, List<RegionEnum> region) {
		List<String> region_str = new ArrayList<String>();
		for(RegionEnum r : region){
			region_str.add(r.toString());
		}
		Query query = Query.query(Criteria
				.where("logDate")
				.gte(startDate)
				.andOperator(
						Criteria.where("logDate")
								.lte(endDate)
								.andOperator(
										Criteria.where("region").in(region_str))
										));

		MapReduceResults<MixEnergyRegionalValueObject> mapReduce = template
				.mapReduce(query, "mixEnergyRegional", map, reduce,
						MixEnergyRegionalValueObject.class);

		List<MixEnergyRegional> list = new ArrayList<MixEnergyRegional>();
		Iterator<MixEnergyRegionalValueObject> it = mapReduce.iterator();
		while (it.hasNext()) {
			MixEnergyRegionalValueObject mix = it.next();
			list.add(mix.getValue());
		}
		return list;
	}

	public List<MixEnergyRegional> aggregate(String agg, Date start, Date end,
			List<String> field, List<RegionEnum> region) {
		TimedAggregateEnum fromAbbr = TimedAggregateEnum.fromAbbr(agg);
		switch (fromAbbr) {
		case DAY:
			return this.aggregateDay(start, end, field, region);
		case HOUR:
			return this.aggregateHour(start, end, field, region);
		case QUARTER:
			return this.aggregateQuarter(start, end, field, region);
		case MONTH:
			return this.aggregateMonth(start, end, field, region);
		case WEEK:
			break;
		case YEAR:
			return this.aggregateYear(start, end, field, region);
		default:
			break;
		}
		return null;
	}

	@Override
	public List<MixEnergyRegional> aggregateQuarter(Date startDate,
			Date endDate, List<String> field, List<RegionEnum> region) {
		return this.genericAggregat(startDate, endDate,
				this.mapMinutes(field, 15), this.reduce(field), region);
	}

	@Override
	public List<MixEnergyRegional> aggregateHour(Date startDate, Date endDate,
			List<String> field, List<RegionEnum> region) {
		return this.genericAggregat(startDate, endDate, this.mapHour(field),
				this.reduce(field), region);
	}

	@Override
	public List<MixEnergyRegional> aggregateDay(Date startDate, Date endDate,
			List<String> field, List<RegionEnum> region) {
		return this.genericAggregat(startDate, endDate, this.mapDay(field),
				this.reduce(field), region);
	}

	@Override
	public List<MixEnergyRegional> aggregateMonth(Date startDate, Date endDate,
			List<String> field, List<RegionEnum> region) {
		return this.genericAggregat(startDate, endDate, this.mapMonth(field),
				this.reduce(field), region);
	}

	@Override
	public List<MixEnergyRegional> aggregateYear(Date startDate, Date endDate,
			List<String> field, List<RegionEnum> region) {
		return this.genericAggregat(startDate, endDate, this.mapYear(field),
				this.reduce(field), region);
	}

}

