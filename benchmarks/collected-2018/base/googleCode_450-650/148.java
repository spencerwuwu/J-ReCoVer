// https://searchcode.com/api/result/12143282/

package org.zigabyte.quantdesk;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yccheok.jstock.engine.AbstractYahooStockHistoryServer;
import org.yccheok.jstock.engine.Code;
import org.yccheok.jstock.engine.Country;
import org.yccheok.jstock.engine.Duration;
import org.yccheok.jstock.engine.SimpleDate;
import org.yccheok.jstock.engine.Stock;
import org.yccheok.jstock.engine.StockHistoryNotFoundException;
import org.yccheok.jstock.engine.StockHistoryServer;
import org.yccheok.jstock.engine.StockNotFoundException;
import org.yccheok.jstock.engine.StockServer;
import org.yccheok.jstock.engine.Symbol;
import org.yccheok.jstock.engine.Utils;

import com.tictactec.ta.lib.MInteger;

public class MyYahooStockHistoryServer implements StockHistoryServer {

	private static final String YAHOO_ICHART_BASED_URL = "http://ichart.yahoo.com/table.csv?s=";
	private static final Log log = LogFactory.getLog(AbstractYahooStockHistoryServer.class);
	private static final Duration DEFAULT_HISTORY_DURATION = Duration.getTodayDurationByYears(10);
	public Map<SimpleDate, Stock> historyDatabase = new HashMap<SimpleDate, Stock>();
	private List<SimpleDate> simpleDates = new ArrayList<SimpleDate>();
	private List<SimpleDate> dividendDates = new ArrayList<SimpleDate>();
	private Map<SimpleDate, Double> dividendDatabase = new HashMap<SimpleDate, Double>();
	private Country country = Country.UnitedState;
	private Code code;
	private Duration duration;
	private int avgDaysBetweenDividend = 0;
	private static MyYahooStockHistoryServer correlation = null;
	private static String correlationSymbol = null;

	public MyYahooStockHistoryServer(Country country, Code code) throws StockHistoryNotFoundException {
		this(country, code, DEFAULT_HISTORY_DURATION);
	}

	public MyYahooStockHistoryServer(Country country, Code code, Duration duration) throws StockHistoryNotFoundException {
		if (code == null || duration == null) {
			throw new IllegalArgumentException("Code or duration cannot be null");
		}

		this.country = country;
		this.code = Utils.toYahooFormat(code, country);
		this.duration = duration;
		try {
			buildHistory(this.code);
			getDividendInfo();
		} catch (java.lang.OutOfMemoryError exp) {
			// Thrown from method.getResponseBodyAsString
			log.error(null, exp);
			throw new StockHistoryNotFoundException("Out of memory", exp);
		}
	}

	@Override
	public Calendar getCalendar(int index) {
		return simpleDates.get(index).getCalendar();
	}

	@Override
	public long getMarketCapital() {
		return 0; // TODO Auto-generated method stub
	}

	@Override
	public int getNumOfCalendar() {
		return simpleDates.size();
	}

	@Override
	public long getSharesIssued() {
		return 0; // TODO Auto-generated method stub
	}

	@Override
	public Stock getStock(Calendar calendar) {
		SimpleDate simpleDate = new SimpleDate(calendar);
		return historyDatabase.get(simpleDate);
	}

	private void buildHistory(Code code) throws StockHistoryNotFoundException {
		final StringBuilder stringBuilder = new StringBuilder(YAHOO_ICHART_BASED_URL);

		final String symbol;
		try {
			symbol = java.net.URLEncoder.encode(code.toString(), "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new StockHistoryNotFoundException("code.toString()=" + code.toString(), ex);
		}

		stringBuilder.append(symbol);

		final int endMonth = duration.getEndDate().getMonth();
		final int endDate = duration.getEndDate().getDate();
		final int endYear = duration.getEndDate().getYear();
		final int startMonth = duration.getStartDate().getMonth();
		final int startDate = duration.getStartDate().getDate();
		final int startYear = duration.getStartDate().getYear();

		final StringBuilder formatBuilder = new StringBuilder("&d=");
		formatBuilder.append(endMonth).append("&e=").append(endDate).append("&f=").append(endYear).append("&g=d&a=").append(startMonth).append("&b=").append(startDate).append("&c=").append(startYear).append("&ignore=.csv");

		final String location = stringBuilder.append(formatBuilder).toString();

		boolean success = false;

		for (int retry = 0; retry < 2; retry++) {
			final String respond = getResponseBodyAsStringBasedOnProxyAuthOption(location);

			if (respond == null) {
				continue;
			}

			success = parse(respond, code);

			if (success) {
				break;
			}
		}

		if (success == false) {
			throw new StockHistoryNotFoundException(code.toString());
		}
	}

	private boolean parse(String respond, Code code) {
		historyDatabase.clear();
		simpleDates.clear();

		java.text.SimpleDateFormat dateFormat = (java.text.SimpleDateFormat) java.text.DateFormat.getInstance();
		dateFormat.applyPattern("yyyy-MM-dd");
		final Calendar calendar = Calendar.getInstance();

		String[] stockDatas = respond.split("\r\n|\r|\n");

		// There must be at least two lines : header information and history information.
		final int length = stockDatas.length;

		if (length <= 1) {
			return false;
		}

		Symbol symbol = Symbol.newInstance(code.toString());
		String name = symbol.toString();
		Stock.Board board = Stock.Board.Unknown;
		Stock.Industry industry = Stock.Industry.Unknown;

		try {
			Stock stock = getStockServer(this.country).getStock(code);
			symbol = stock.getSymbol();
			name = stock.getName();
			board = stock.getBoard();
			industry = stock.getIndustry();
		} catch (StockNotFoundException exp) {
			log.error(null, exp);
		}

		double previousClosePrice = Double.MAX_VALUE;

		for (int i = length - 1; i > 0; i--) {
			// Use > instead of >=, to avoid header information (Date,Open,High,Low,Close,Volume,Adj Close)
			String[] fields = stockDatas[i].split(",");

			// Date,Open,High,Low,Close,Volume,Adj Close
			if (fields.length < 7) {
				continue;
			}

			try {
				calendar.setTime(dateFormat.parse(fields[0]));
			} catch (ParseException ex) {
				log.error(null, ex);
				continue;
			}

			double prevPrice = 0.0;
			double openPrice = 0.0;
			double highPrice = 0.0;
			double lowPrice = 0.0;
			double closePrice = 0.0;
			// TODO: CRITICAL LONG BUG REVISED NEEDED.
			long volume = 0;
			//double adjustedClosePrice = 0.0;

			try {
				prevPrice = (previousClosePrice == Double.MAX_VALUE) ? 0 : previousClosePrice;
				openPrice = Double.parseDouble(fields[1]);
				highPrice = Double.parseDouble(fields[2]);
				lowPrice = Double.parseDouble(fields[3]);
				closePrice = Double.parseDouble(fields[4]);
				// TODO: CRITICAL LONG BUG REVISED NEEDED.
				volume = Long.parseLong(fields[5]);
				//adjustedClosePrice = Double.parseDouble(fields[6]);
			} catch (NumberFormatException exp) {
				log.error(null, exp);
			}

			double changePrice = (previousClosePrice == Double.MAX_VALUE) ? 0 : closePrice - previousClosePrice;
			double changePricePercentage = ((previousClosePrice == Double.MAX_VALUE) || (previousClosePrice == 0.0)) ? 0 : changePrice / previousClosePrice * 100.0;

			SimpleDate simpleDate = new SimpleDate(calendar);

			Stock stock = new QDStock(
				code,
				symbol,
				name,
				board,
				industry,
				prevPrice,
				openPrice,
				closePrice, /* Last Price. */
				highPrice,
				lowPrice,
				volume,
				changePrice,
				changePricePercentage,
				0,
				0.0,
				0,
				0.0,
				0,
				0.0,
				0,
				0.0,
				0,
				0.0,
				0,
				0.0,
				0,
				simpleDate.getCalendar());

			historyDatabase.put(simpleDate, stock);
			simpleDates.add(simpleDate);
			previousClosePrice = closePrice;
		}
		return (historyDatabase.size() > 1);
	}

	protected StockServer getStockServer(Country country) {
		return new MyYahooStockServer(country);
	}

	public static String getResponseBodyAsStringBasedOnProxyAuthOption(String request) {
		org.apache.commons.httpclient.HttpClient httpClient = new HttpClient();
		org.yccheok.jstock.engine.Utils.setHttpClientProxyFromSystemProperties(httpClient);

		final HttpMethod method = new GetMethod(request);
		String respond = null;
		try {
			httpClient.executeMethod(method);
			//respond = method.getResponseBodyAsString();
			InputStream stream = method.getResponseBodyAsStream();
			StringBuffer buffer = new StringBuffer();
			int c;
			while ((c = stream.read()) != -1) {
				buffer.append((char) c);
			}
			respond = buffer.toString();
		} catch (HttpException exp) {
			log.error(null, exp);
			return null;
		} catch (IOException exp) {
			log.error(null, exp);
			return null;
		} finally {
			method.releaseConnection();
		}
		return respond;
	}

	public double max(int period) {
		double max = Double.MIN_VALUE;
		int arraySize = simpleDates.size();
		if (period > arraySize) {
			period = arraySize;
		}
		for (int i = arraySize - period; i < arraySize; i++) {
			double val = historyDatabase.get(simpleDates.get(i)).getLastPrice();
			if (val > max) {
				max = val;
			}
		}
		System.out.println("\tMax: " + max);
		return max;
	}

	public double min(int period) {
		double min = Double.MAX_VALUE;
		int arraySize = simpleDates.size();
		if (period > arraySize) {
			period = arraySize;
		}
		for (int i = arraySize - period; i < arraySize; i++) {
			double val = historyDatabase.get(simpleDates.get(i)).getLastPrice();
			if (val < min) {
				min = val;
			}
		}
		System.out.println("\tMin: " + min);
		return min;
	}

	public double macd(int period) {
		return 0.0;
	}

	public int getDividendLength() {
		return avgDaysBetweenDividend;
	}

	public double getAvgDividendPayment() {
		double total = 0;
		int size = dividendDates.size();
		for (int i = 0; i < size; i++) {
			total += dividendDatabase.get(dividendDates.get(i));
		}
		return size > 0 ? total / size : 0;
	}

	public double getLastDividendPayment() {
		int size = dividendDates.size();
		return size > 0 ? dividendDatabase.get(dividendDates.get(size - 1)) : 0;
	}

	private void getDividendInfo() {
		StringBuilder buffer = new StringBuilder(YAHOO_ICHART_BASED_URL).append(code.toString());
		final int endMonth = duration.getEndDate().getMonth();
		final int endDate = duration.getEndDate().getDate();
		final int endYear = duration.getEndDate().getYear();
		final int startMonth = duration.getStartDate().getMonth();
		final int startDate = duration.getStartDate().getDate();
		final int startYear = duration.getStartDate().getYear();

		final StringBuilder formatBuilder = new StringBuilder("&d=");
		formatBuilder.append(endMonth).append("&e=").append(endDate).append("&f=").append(endYear).append("&g=v&a=").append(startMonth).append("&b=").append(startDate).append("&c=").append(startYear).append("&ignore=.csv");

		buffer.append(formatBuilder);
		for (int i = 0; i < 2; i++) {
			String respond = getResponseBodyAsStringBasedOnProxyAuthOption(buffer.toString());
			if (respond != null) {
				parseDividend(respond);
			}
		}
	}

	private void parseDividend(String response) {
		String[] lines = response.split("\r\n|\n|\r");
		SimpleDateFormat format = (SimpleDateFormat) DateFormat.getInstance();
		format.applyPattern("yyy-MM-dd");
		dividendDates.clear();
		dividendDatabase.clear();
		// There will be a 404 if there is no dividend info.
		if (response.indexOf("404 Not Found") == -1) {
			for (int i = 1; i < lines.length; i++) {
				String line = lines[i];
				String[] fields = line.split(",");
				if (fields.length > 2) {
					continue;
				}
				Calendar c = Calendar.getInstance();
				try {
					c.setTime(format.parse(fields[0]));
				} catch (ParseException pe) {
					pe.printStackTrace();
				}
				Double dividend = Double.valueOf(fields[1]);
				SimpleDate date = new SimpleDate(c);
				dividendDates.add(date);
				dividendDatabase.put(date, dividend);
			}
		}
		int count = 0;
		long diff = 0;
		int size = dividendDates.size() - 1;
		Collections.reverse(dividendDates);
		for (int i = 0; i < size; i++) {
			SimpleDate date1 = dividendDates.get(i);
			SimpleDate date2 = dividendDates.get(i + 1);
			long days = (date2.getCalendar().getTimeInMillis() - date1.getCalendar().getTimeInMillis()) / 86400000L;
			diff += days;
			count++;
		}
		avgDaysBetweenDividend = count > 0 ? (int) (diff / count) : Integer.MAX_VALUE;
	}

	public double getMean() {
		double sum = 0.0;
		int size = this.simpleDates.size();
		for (int i = 0; i < size; i++) {
			sum += this.getStock(this.getCalendar(i)).getLastPrice();
		}
		return sum / size;
	}

	public double getStddev() {
		int size = this.simpleDates.size();
		double mean = this.getMean();
		double total = 0.0;
		for (int i = 0; i < size; i++) {
			total += Math.pow(this.getStock(this.getCalendar(i)).getLastPrice() - mean, 2);
		}
		return Math.sqrt(total / (size - 1));
	}

	public double correlation(String symbol) {
		// Store the data for the symbol we are comparing to. Reduce time needed to compare and less server hits.
		if (symbol != null && !symbol.equals(correlationSymbol)) {
			correlationSymbol = symbol;
			try {
				correlation = new MyYahooStockHistoryServer(Country.UnitedState, Code.newInstance(symbol), this.duration);
			} catch (StockHistoryNotFoundException e) {
				e.printStackTrace();
				return 0.0;
			}
		}
		double mean1 = this.getMean();
		double mean2 = correlation.getMean();
		int size = this.simpleDates.size();
		double sum = 0.0;
		double[] data1 = new double[this.getNumOfCalendar()];
		double[] data2 = new double[correlation.getNumOfCalendar()];
		for (int i = 0; i < this.getNumOfCalendar(); i++) {
			data1[i] = this.getStock(this.getCalendar(i)).getLastPrice();
		}
		for (int i = 0; i < correlation.getNumOfCalendar(); i++) {
			data2[i] = correlation.getStock(correlation.getCalendar(i)).getLastPrice();
		}
		int len = Math.min(this.getNumOfCalendar(), correlation.getNumOfCalendar());
		MInteger outBegIdx = new MInteger();
		MInteger outNBElement = new MInteger();
		double[] output = new double[len];
		com.tictactec.ta.lib.Core c = new com.tictactec.ta.lib.Core();
		c.correl(0, len - 1, data1, data2, len - 1, outBegIdx, outNBElement, output);
		System.out.print("TALIB (" + correlationSymbol + ", " + this.code.toString() + "): " + output[0]);
		for (int i = 1; i < output.length; i++) {
			System.out.print(", " + output[i]);
		}
		System.out.println();
		for (int i = 0, j = 0; i < size; i++, j++) {
			Calendar d1 = this.getCalendar(i);
			Calendar d2 = correlation.getCalendar(j);
			int compare = d1.compareTo(d2);
			// If the dates don't match, move ahead to find a matching date
			while (compare != 0) {
				if (compare < 0) {
					i++;
				} else {
					j++;
				}
				if (i > this.getNumOfCalendar() - 1 || j > correlation.getNumOfCalendar() - 1) {
					return 0.0;
				}
				d1 = this.getCalendar(i);
				d2 = correlation.getCalendar(j);
				compare = d1.compareTo(d2);
			}
			double delta = (this.getStock(d1).getLastPrice() - mean1) * (correlation.getStock(d2).getLastPrice() - mean2);
			sum += delta;
		}
		;
		double denom = (size - 1) * this.getStddev() * correlation.getStddev();
		double r = sum / denom;
		System.out.println("Correlation between " + correlationSymbol + " and " + this.code.toString() + " is: " + r);
		return r;
	}

	private double covar(double[] one, double[] two) {
		double ans = 0.0;
		double[] output = new double[one.length];
		MInteger outInt = new MInteger();
		MInteger outElem = new MInteger();
		com.tictactec.ta.lib.Core c = new com.tictactec.ta.lib.Core();
		c.beta(0, Math.min(one.length, two.length) - 1, one, two, Math.min(one.length, two.length) - 1, outInt, outElem, output);
		System.out.println("Covariance: (" + outInt.value + ", " + outElem.value + ")");
		for (int i = 0; i < output.length; i++) {
			System.out.print(output[i] + ",");
		}
		return output[0];
	}

	public double beta(String symbol) {
		double b = 0.0;
		if (symbol != null && !symbol.equals(correlationSymbol)) {
			correlationSymbol = symbol;
			try {
				correlation = new MyYahooStockHistoryServer(Country.UnitedState, Code.newInstance(symbol), this.duration);
			} catch (StockHistoryNotFoundException e) {
				e.printStackTrace();
				return 0.0;
			}
		}
		int thisSize = this.getNumOfCalendar();
		int otherSize = correlation.getNumOfCalendar();
		double[] lastChangeThis = new double[this.simpleDates.size()];
		double[] lastChangeRef = new double[correlation.getNumOfCalendar()];
		for (int i = 0; i < thisSize; i++) {
			lastChangeThis[i] = this.getStock(this.getCalendar(i)).getLastPrice();
		}
		for (int i = 0; i < otherSize; i++) {
			lastChangeRef[i] = correlation.getStock(correlation.getCalendar(i)).getLastPrice();
		}
		int maxLength = Math.min(lastChangeThis.length, lastChangeRef.length);
		double[] output = new double[maxLength];
		MInteger outInt = new MInteger();
		MInteger outElem = new MInteger();
		com.tictactec.ta.lib.Core c = new com.tictactec.ta.lib.Core();
		c.beta(0, maxLength - 1, lastChangeThis, lastChangeRef, maxLength - 1, outInt, outElem, output);
		b = output[0];
		System.out.println("Beta between " + correlationSymbol + " and " + this.code + " equals " + b);
		return b;
	}
}

