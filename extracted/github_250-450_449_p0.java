      /** The serialVersionUID */
      private static final long serialVersionUID = 1901016598354633256L;
      private boolean throwException;

      public ExceptionReducer(boolean throwException) {
         this.throwException = throwException;
      }

      @Override
      public Integer reduce(String key, Iterator<Integer> iter) {
         if(throwException) {
            //simulating exception
            int a = 4 / 0;
         }

         return 0;
      }
