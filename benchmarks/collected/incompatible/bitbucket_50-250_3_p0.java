
    public void reduce(Text gID, Iterable<IntWritable> positions, Context context)
        throws IOException, InterruptedException {

      // Initialize matrix, default all false
      HashMap<Integer, Boolean> matrix = new HashMap<Integer, Boolean>();
      // initialize unionfind
      UnionFind u = new UnionFind((COLUMN_GROUP_WIDTH + 1) * COLUMN_GROUP_HEIGHT);

      int gid = Integer.parseInt(gID.toString());
      // update matrix from Iterable<IntWritable> positions
      int position = 0, posLocal = 0;

      for (IntWritable p : positions) {
        position = p.get();
        int col = position / COLUMN_GROUP_HEIGHT;
        int row = position % COLUMN_GROUP_HEIGHT;
        posLocal = col * Pass1.COLUMN_GROUP_HEIGHT + row;
        matrix.put(posLocal, true);
      }

      // traverse the matrix, process union-find algorithm
      int nRow = Pass1.COLUMN_GROUP_HEIGHT; // height
      int nCol = Pass1.COLUMN_GROUP_WIDTH + 1; // width


      for (int col = 0; col < nCol; col++) {
        for (int row = 0; row < nRow; row++) {
          // if this entry has tree
          posLocal = col * Pass1.COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(posLocal)) {
            // if the vertex is connected with the one to its east
            if (col + 1 < nCol && matrix.containsKey(posLocal + Pass1.COLUMN_GROUP_HEIGHT)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col + 1, row, nRow));
            }
            // if the vertex is connected with the one to its north
            if (row + 1 < nRow && matrix.containsKey(posLocal + 1)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col, row + 1, nRow));
            }
            // if the vertex is connected with the one to its north-east
            if (col + 1 < nCol && row + 1 < nRow
                && matrix.containsKey(posLocal + Pass1.COLUMN_GROUP_HEIGHT + 1)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col + 1, row + 1, nRow));
            }
            // if the vertex is connected with the one to its north-west
            if (col - 1 >= 0 && row + 1 < nRow
                && matrix.containsKey(posLocal - Pass1.COLUMN_GROUP_HEIGHT + 1)) {
              u.union(Common.getPos(col, row, nRow), Common.getPos(col - 1, row + 1, nRow));
            }
          } else {
            // if this entry has no tree
            int index = Common.getPos(col, row, nRow);
            u.array[index] = 0;
          }
        }
      }

      int pos = 0, rootLocal = 0, absPos = 0, absRootLocal = 0;
      // traverse the matrix, for each 'vertex', find its root, and output as TupleWritable
      for (int col = 0; col < nCol; col++) {
        for (int row = 0; row < nRow; row++) {
          posLocal = col * COLUMN_GROUP_HEIGHT + row;
          if (matrix.containsKey(posLocal)) {
            pos = Common.getPos(col, row, nRow);
            // find the root of the CC
            rootLocal = u.find(pos);
            // change to absolute coordinates.
            absPos = COLUMN_GROUP_WIDTH * COLUMN_GROUP_HEIGHT * gid + pos;
            absRootLocal = COLUMN_GROUP_WIDTH * COLUMN_GROUP_HEIGHT * gid + rootLocal;

            String t = String.valueOf(absPos) + " " + String.valueOf(absRootLocal);

            try {
              context.write(gID, new Text(t));

            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
