package com.esri.realtime.dse.util;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public abstract class Converter
{
  private static boolean isEmpty(String s)
  {
    return s == null || s.length() == 0;
  }

  /**
   * Converts an Object value to a String value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static String toString(Object value)
  {
    return (value != null) ? (value instanceof String) ? (String) value : value.toString() : null;
  }

  /**
   * Converts an Object value to a Date value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Date toDate(Object value)
  {
    if (value != null)
    {
      if (value instanceof Date)
        return (Date) value;
      else if (value instanceof Long)
      {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis((Long) value);
        return calendar.getTime();
      }
      else if (value instanceof String)
      {
        try
        {
          return DateUtil.checkedConvert((String) value);
        }
        catch (ParseException e)
        {
          ;
        }
      }
    }
    return null;
  }

  public static Long toEpochLong(Long dateAsLong)
  {
    if (dateAsLong <= 9999999999L) {
      return dateAsLong * 1000; // parse date as seconds since epoch
    } else {
      return dateAsLong;
    }
  }

  public static Long toEpochLong(String dateAsStr, String format)
  {
    if(StringUtils.isEmpty(format)) {
      Long dateAsLong = Converter.toLong(dateAsStr, Long.MIN_VALUE);
      if(dateAsLong != Long.MIN_VALUE) {
        return toEpochLong(dateAsLong);
      } else {
        // try dates as strings
        Date date = DateUtil.convert(dateAsStr, format);
        if(date == null)
          return null;
        else
          return date.getTime();
      }
    } else {
      Date date = DateUtil.convert(dateAsStr, format);
      if(date == null)
        return null;
      else
        return date.getTime();
    }
  }

  /**
   * Converts a string to a boolean value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Boolean toBoolean(String s, Boolean defaultValue)
  {
    if (!isEmpty(s))
    {
      String v = s.toLowerCase().trim();
      if (v.equals("true"))
        return true;
      else if (v.equals("false"))
        return false;
      else if (v.equals("y"))
        return true;
      else if (v.equals("n"))
        return false;
      else if (v.equals("yes"))
        return true;
      else if (v.equals("no"))
        return false;
      else if (v.equals("on"))
        return true;
      else if (v.equals("off"))
        return false;
      else if (v.equals("0"))
        return false;
      else if (v.equals("1"))
        return true;
      else if (v.equals("-1"))
        return false;
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Boolean value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Boolean toBoolean(Object value)
  {
    return (value != null) ? (value instanceof Boolean) ? (Boolean) value : toBoolean(value.toString(), null) : null;
  }

  /**
   * Converts a string to a float value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Float toFloat(String s, Float defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        String str = s;
        if (s.contains(","))
          str = s.replaceAll(",", ".");

        return Float.parseFloat(str);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Float value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Float toFloat(Object value)
  {
    return (value != null) ? (value instanceof Float) ? (Float) value : toFloat(value.toString(), null) : null;
  }

  /**
   * Converts a string to a double value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Double toDouble(String s, Double defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        String str = s;
        if (s.contains(","))
          str = s.replaceAll(",", ".");

        return Double.parseDouble(str);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Double value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Double toDouble(Object value)
  {
    return (value != null) ? (value instanceof Double) ? (Double) value : toDouble(value.toString(), null) : null;
  }

  /**
   * Converts a string to an int value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Integer toInteger(String s, Integer defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        return Integer.parseInt(s);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to an Integer value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Integer toInteger(Object value)
  {
    if (value != null)
    {
      if (value instanceof Integer)
        return (Integer) value;
      else
      {
        Double doubleValue = toDouble(value);
        if (doubleValue != null)
          return ((Long) Math.round(doubleValue)).intValue();
      }
    }
    return null;
  }

  /**
   * Converts a string to an short value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Short toShort(String s, Short defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        return Short.parseShort(s);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Short value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Short toShort(Object value)
  {
    if (value != null)
    {
      if (value instanceof Short)
        return (Short) value;
      else
      {
        Double doubleValue = toDouble(value);
        if (doubleValue != null)
          return ((Long) Math.round(doubleValue)).shortValue();
      }
    }
    return null;
  }

  /**
   * Converts a string to a byte value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Byte toByte(String s, Byte defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        return Byte.parseByte(s);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Byte value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Byte toByte(Object value)
  {
    if (value != null)
    {
      if (value instanceof Byte)
        return (Byte) value;
      else
      {
        Double doubleValue = toDouble(value);
        if (doubleValue != null)
          return ((Long) Math.round(doubleValue)).byteValue();
      }
    }
    return null;
  }

  /**
   * Converts a string to a long value.
   *
   * @param s
   *          the string to convert
   * @param defaultValue
   *          the default value to return if the string is invalid
   * @return the converted value
   */
  public static Long toLong(String s, Long defaultValue)
  {
    if (!isEmpty(s))
    {
      try
      {
        return Long.parseLong(s);
      }
      catch (Exception e)
      {
        ;
      }
    }
    return defaultValue;
  }

  /**
   * Converts an Object value to a Long value.
   *
   * @param value
   *          value to convert
   * @return the converted value
   */
  public static Long toLong(Object value)
  {
    if (value != null)
    {
      if (value instanceof Long)
        return (Long) value;
      else if (value instanceof Date)
        return ((Date)value).getTime();
      else
      {
        Double doubleValue = toDouble(value);
        if (doubleValue != null)
          return Math.round(doubleValue);
      }
    }
    return null;
  }

  /**
   * Converts a string to a UUID.
   *
   * @param s
   *          the string to convert
   * @return the converted value or null
   */
  public static UUID toUUID(String s)
  {
    UUID value = null;
    try
    {
      value = UUID.fromString(s);
    }
    catch (IllegalArgumentException e)
    {
      ;
    }
    return value;
  }

  /**
   * Escapes single and double quotes within a string. The escape character
   * inserted is a backslash.
   *
   * @param s
   *          the string to escape
   * @return the escaped string
   */
  public static String escapeQuotes(String s)
  {
    if (isEmpty(s))
      return "";
    else
    {
      int nIdx1 = s.indexOf("\"");
      int nIdx2 = s.indexOf("'");
      if ((nIdx1 != -1) || (nIdx2 != -1))
      {
        char c;
        StringBuffer sb = new StringBuffer(s.length() + 20);
        for (int i = 0; i < s.length(); i++)
        {
          c = s.charAt(i);
          if ((c == '\'') || (c == '"'))
            sb.append("\\");
          sb.append(c);
        }
        return sb.toString();
      }
      else
        return s;
    }
  }

  /**
   * Escapes special xml characters within a string.
   * <p>
   * < > & " are escaped. The single quote character is not escaped.
   *
   * @param s
   *          the string to escape
   * @return the escaped string
   */
  public static String escapeXmlForBrowser(String s)
  {
    if (isEmpty(s))
      return "";
    else
    {
      char c;
      StringBuffer sb = new StringBuffer(s.length() + 20);
      for (int i = 0; i < s.length(); i++)
      {
        c = s.charAt(i);
        if (c == '&')
          sb.append("&amp;");
        else if (c == '<')
          sb.append("&lt;");
        else if (c == '>')
          sb.append("&gt;");
        else if (c == '\'')
          sb.append("&apos;");
        else if (c == '"')
          sb.append("&quot;");
        else
          sb.append(c);
      }
      return sb.toString();
    }
  }

  static final String	HEXES	= "0123456789ABCDEF";

  public static String toHexString(byte[] bytes)
  {
    if (bytes == null)
      return null;
    final StringBuilder hex = new StringBuilder(3 * bytes.length);
    for (final byte b : bytes)
      hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F))).append(" ");
    return hex.toString().trim();
  }

  public static String removeUTF8BOM(String s)
  {
    return (s != null && s.startsWith("\uFEFF")) ? s.substring(1) : s;
  }
}
