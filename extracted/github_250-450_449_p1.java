      /** The serialVersionUID */
      private static final long serialVersionUID = 1901016598354633256L;
      private static int counter;

      @Override
      public Integer reduce(String key, Iterator<Integer> iter) {

         if(counter > 0) {
            //simulating exception
            int a = 4 / 0;
         }
         counter++;

         return 0;
      }
