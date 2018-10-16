
    static DateTimeFormatter dayFormat = ISODateTimeFormat.yearMonthDay();
    static DateTimeFormatter monthFormat = ISODateTimeFormat.yearMonth();

    public void reduce(IntWritable key, Iterable<LongWritable> values,
                       Context context) throws IOException, InterruptedException {

      HashMap<DateTime, Integer> days = new HashMap<DateTime, Integer>(); 
      HashMap<DateTime, Integer> months = new HashMap<DateTime, Integer>(); 

      for (LongWritable value: values) {
        DateTime timestamp = new DateTime(value.get());
        DateTime day = timestamp.withTimeAtStartOfDay(); 
        DateTime month = day.withDayOfMonth(1); 
        incrementCount(days, day);
        incrementCount(months, month);
      }
      for (Entry<DateTime, Integer> entry: days.entrySet()) 
        context.write(key, formatEntry(entry, dayFormat));
      for (Entry<DateTime, Integer> entry: months.entrySet())
        context.write(key, formatEntry(entry, monthFormat)); 
    }

    private void incrementCount(HashMap<DateTime, Integer> counts, DateTime key) {
      Integer currentCount = counts.get(key);
      if (currentCount == null)
        counts.put(key, 1);
      else
        counts.put(key, currentCount + 1);
    }

    private Text formatEntry(Entry<DateTime, Integer> entry, 
                             DateTimeFormatter formatter) {
      return new Text(formatter.print(entry.getKey()) + "\t" + entry.getValue())
    }
