
      @Override
      public Integer reduce(String reducedKey, Iterator<Integer> iter) {
         int sum = 0;
         while (iter.hasNext()) {
            sum += iter.next();
         }
         return sum;
      }
