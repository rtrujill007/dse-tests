package com.esri.realtime.dse.util;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public abstract class DateUtil
{
// TODO:  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(DateUtil.class);

  public static String format(Date d)
  {
    return ThreadSafeDateFormatter.format(d, "yyyy-MM-dd'T'HH:mm:ss");
  }

  public static String format(Date d, String format)
  {
    return ThreadSafeDateFormatter.format(d, format);
  }

  public static Date convert(String s)
  {
    try
    {
      return checkedConvert(s);
    }
    catch (ParseException e)
    {
      // System.out.println("=========== Could not parse the date |" + s + "|");
      return null;
    }
  }

  public static Date convert(String s, DateFormatID id)
  {
    try
    {
      return checkedConvert(s, id);
    }
    catch (ParseException e)
    {
      // System.out.println("=========== Could not parse the date |" + s + "|");
      return null;
    }
  }

  public static Date convert(String s, String format)
  {
    if (format == null || format.isEmpty())
      return convert(s);

    try
    {
      SimpleDateFormat sdf = new SimpleDateFormat(format);
      return sdf.parse(s);
    }
    catch (ParseException e)
    {
      return null;
    }
  }

  public static Date convert(String s, String format, DateFormatID id)
  {
	  if (format != null && !format.isEmpty())
		  return convert(s, format);
	  else
	  	return convert(s, id);
  }


  public static Date checkedConvert(String s, DateFormatID id) throws ParseException
  {
	  int count = 0;
	  int go = id.value();
	  while (count < 7)
	  {
		  switch (go)
		  {
		  	case 0:
		  		try {
		  			id.set(go);
		  			return new Date(ISODateTimeFormat.dateTimeParser().withZoneUTC().parseMillis(s));
		  		} catch (IllegalArgumentException e) {
		  			count++;
		  			go = 1;
		  			continue;
		  		}
		  	case 1:
		  		try {
		  			id.set(go);
		  			return ThreadSafeDateFormatter.parse(s, null);
		  		} catch (Exception e) {
		  			count++;
		  			go = 2;
		  			continue;
		  		}
		  	case 2:
		  		try {
		  			id.set(go);
		  			return ThreadSafeDateFormatter.parse(s, "EEE MMM dd HH:mm:ss zzz yyyy");
		  		} catch (Exception e) {
		  			count++;
		  			go = 3;
		  			continue;
		  		}
		  	case 3:
		  		try {
		  			id.set(go);
		  			return new Date(DateTimeFormat.forPattern("MM/dd/yy hh:mm:ss aa").parseMillis(s));
		  		} catch (IllegalArgumentException e) {
		  			count++;
		  			go = 4;
		  			continue;
		  		}
		  	case 4:
		  		try {
		  			id.set(go);
		  			return new Date(DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss").parseMillis(s));
		  		} catch (IllegalArgumentException e) {
		  			count++;
		  			go = 5;
		  			continue;
		  		}
		  	case 5:
		  		try {
		  			id.set(go);
		  			return new Date(DateTimeFormat.forPattern("MM/dd/yy HH:mm").parseMillis(s));
		  		} catch (IllegalArgumentException e) {
		  			count++;
		  			go = 6;
		  			continue;
		  		}
		  	case 6:
		  		try {
		  			id.set(go);
		  			return new Date(Long.parseLong(s));
		  		} catch (Exception e) {
		  			count++;
		  			go = 0;
		  			continue;
		  		}
		  	default:
		  		go = 0;
		  		continue;
		  }
	  }
//  TODO: LOGGER.translate("DATE_UTILS_PARSE_ERROR", s)
	  throw new ParseException("", 0);
  }

  public static Date checkedConvert(String s) throws ParseException
  {
    try
    {
      DateTimeFormatter formatter = ISODateTimeFormat.dateTimeParser().withZoneUTC();
      long millis = formatter.parseMillis(s);
      return new Date(millis);

      // Try parsing the ISO standard format
      // return ThreadSafeDateFormatter.parse(s, "yyyy-MM-dd'T'HH:mm:ss");
    }
    catch (IllegalArgumentException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      // Try parsing the default system format
      return ThreadSafeDateFormatter.parse(s, null);
    }
    catch (ParseException e)
    {
      // System.out.println("=========== PARSE EXCEPTION |" + s +"|");
      // e.printStackTrace();
    }
    catch (NumberFormatException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      // Try parsing the default system format
      return ThreadSafeDateFormatter.parse(s, "EEE MMM dd HH:mm:ss zzz yyyy");
    }
    catch (ParseException e)
    {
      // System.out.println("=========== PARSE EXCEPTION |" + s +"|");
      // e.printStackTrace();
    }
    catch (NumberFormatException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yy hh:mm:ss aa");
      long millis = formatter.parseMillis(s);
      return new Date(millis);

      // Try parsing the default format from the old tracking server
      // return ThreadSafeDateFormatter.parse(s, "MM/dd/yyyy hh:mm:ss aa");
    }
    catch (IllegalArgumentException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm:ss");
      long millis = formatter.parseMillis(s);
      return new Date(millis);

      // Try parsing the default format from the old tracking server
      // return ThreadSafeDateFormatter.parse(s, "MM/dd/yyyy hh:mm:ss aa");
    }
    catch (IllegalArgumentException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm");
      long millis = formatter.parseMillis(s);
      return new Date(millis);
    }
    catch (IllegalArgumentException e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }

    try
    {
      long millis = Long.parseLong(s);
      return new Date(millis);

      // Try parsing the default format from the old tracking server
      // return ThreadSafeDateFormatter.parse(s, "MM/dd/yyyy hh:mm:ss aa");
    }
    catch (Exception e)
    {
      // System.out.println("=========== NUMBER FORMAT EXCEPTION: |" + s+"|");
      // e.printStackTrace();
    }
//  TODO:  LOGGER.translate("DATE_UTILS_PARSE_ERROR", s)
    throw new ParseException("", 0);
  }

  public static boolean beforeOrEqual(Date d1, Date d2)
  {
    return !(d1 == null || d2 == null) && (d1.before(d2) || d1.equals(d2));
  }

  public static Date addMins(Date d, long minutes)
  {
    return new Date(d.getTime() + minutes * 60 * 1000);
  }

  public static Date getEarliestTime(List<Date> dates)
  {
    Date earliest = null;
    for (Date d : dates)
    {
      if (earliest == null)
        earliest = d;
      else if (earliest.after(d))
        earliest = d;
    }
    return earliest;
  }

  public static Date getNowDateAtHour(int hour)
  {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR_OF_DAY, 8);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    return c.getTime();
  }

  public static long minutesBetween(Date d1, Date d2)
  {
    return secondsBetween(d1, d2) / 60;
  }

  public static long secondsBetween(Date d1, Date d2)
  {
    return millisecondsBetween(d1, d2) / 1000;
  }

  public static long millisecondsBetween(Date d1, Date d2)
  {
    return (d1 != null && d2 != null && d1.getTime() - d2.getTime() != 0) ? Math.abs((d1.getTime() - d2.getTime())) : 0;
  }

  public static void main(String[] args)
  {
    try
    {
      String string = "07/02/2012 08:00:00 AM";
      SimpleDateFormat localFormatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
      Date date = localFormatter.parse(string);
      long longVal = date.getTime();

      System.out.println("Default formatter tz is " + localFormatter.getTimeZone().getDisplayName());
      System.out.println("Original string = " + string);
      System.out.println("Parsed date = " + date);
      System.out.println("It's long value is " + longVal);

      System.out.println();

      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
      cal.setTimeInMillis(longVal);
      System.out.println("Now I'm processing the long value, interpreting it as UTC timestamp.");
      System.out.println("cal.getTime() <Date> is " + cal.getTime());

      System.out.println();

      string = "2012-07-02T08:00:00";
      SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      isoFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
      date = isoFormatter.parse(string);
      longVal = date.getTime();
      System.out.println("ISO formatter tz is " + isoFormatter.getTimeZone().getDisplayName());
      System.out.println("The iso string is " + string);
      System.out.println("Parsed iso date = " + date);
      System.out.println("It's long value is " + longVal);

    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}

class ThreadSafeDateFormatter
{
  private static final ThreadLocal<Map<String, DateFormat>> DATE_PARSERS = new ThreadLocal<Map<String, DateFormat>>()
                                                                         {
                                                                           @Override
                                                                           protected Map<String, DateFormat> initialValue()
                                                                           {
                                                                             return new HashMap<String, DateFormat>();
                                                                           }
                                                                         };

  static private final DateFormat getParser(final String pattern)
  {
    Map<String, DateFormat> parserMap = DATE_PARSERS.get();

    if (pattern == null)
      return DateFormat.getDateInstance();
    DateFormat df = parserMap.get(pattern);
    if (df == null)
    {
      // if parser for the same pattern does not exist yet, create one and save
      // it into map
      df = new SimpleDateFormat(pattern);
      if ("yyyy-MM-dd'T'HH:mm:ss".equals(pattern))
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
      parserMap.put(pattern, df);
    }

    return df;
  }

  /**
   * Static Public and Thread-Safe method to parse a date from the give String
   *
   * @param date
   *          : input string to parse
   * @param pattern
   *          : date format pattern of the input string
   * @return Date value of the input string
   * @throws ParseException
   *           If parse exception happened
   */
  static public Date parse(final String date, final String pattern) throws ParseException
  {
    return getParser(pattern).parse(date);
  }

  /**
   * Static Public and Thread-Safe method to parse a date from the give String
   * and return the long value of the result
   *
   * @param date
   *          : input string to parse
   * @param pattern
   *          : date format pattern of the input string
   * @return Long date value of the input string
   * @throws ParseException
   *           If parse exception happened
   * @throws ParseException
   */
  static public long parseLongDate(final String date, final String pattern) throws ParseException
  {
    return parse(date, pattern).getTime();
  }

  /**
   * A thread-safe method to format a given Date based-on the given pattern
   *
   * @param date
   *          Date to be formatted
   * @param pattern
   *          Pattern used to format the date
   * @return String of formatted date
   */
  static public String format(final Date date, final String pattern)
  {
    return getParser(pattern).format(date);
  }

  /**
   * A thread-safe method to format a given Date(in long) based-on the given
   * pattern
   *
   * @param date
   *          Date in long to be formatted
   * @param pattern
   *          Pattern used to format the date
   * @return String of formatted date
   */
  static public String format(final long date, final String pattern)
  {
    return getParser(pattern).format(new Date(date));
  }

}
