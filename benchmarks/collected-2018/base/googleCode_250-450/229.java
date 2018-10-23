// https://searchcode.com/api/result/12375320/

package org.jeffkubina.utils;

import java.util.Random;

/**
 * The class FrequencyDistribution is used to randomly generate a frequency
 * distribution of integer values with a specified size and sum. It was designed
 * to generate distributions for which Size is much larger than can fit in
 * internal memory. It calculates each distribution value in lg(size) steps
 * using lg(size) memory.
 */
public class FrequencyDistribution
{
  /**
   * Instantiates a new FrequencyDistribution for integers 0 to Size - 1 whose
   * values sum to Sum.
   * 
   * @param Size
   *          Holds the size of the distribution, values will be generated for
   *          integers from 0 to Size - 1.
   * @param Sum
   *          Holds the non-negative integer that the values generated must sum
   *          to.
   */
  public FrequencyDistribution(long Size, long Sum)
  {
    initialize(Size, Sum, 0, 1, 1.0);
  }

  /**
   * Instantiates a new FrequencyDistribution for integers 0 to Size - 1 whose
   * values are all greater than or equal to MinValue and sum to Sum.
   * 
   * @param Size
   *          Holds the size of the distribution, values will be generated for
   *          integers from 0 to Size - 1.
   * @param Sum
   *          Holds the non-negative integer that the values generated must sum
   *          to.
   * @param MinValue
   *          Holds the minimum value of a distribution value; if MinValue *
   *          Size > Sum, an exception is thrown.
   */
  public FrequencyDistribution(long Size, long Sum, long MinValue)
  {
    initialize(Size, Sum, MinValue, 1, 1.0);
  }

  /**
   * Instantiates a new FrequencyDistribution for integers 0 to Size - 1 whose
   * values are all greater than or equal to MinValue, sum to Sum, and are
   * generated using the random seed RandomSeed.
   * 
   * @param Size
   *          Holds the size of the distribution, values will be generated for
   *          integers from 0 to Size - 1.
   * @param Sum
   *          Holds the non-negative integer that the values generated must sum
   *          to.
   * @param MinValue
   *          Holds the minimum value of a distribution value; if MinValue *
   *          Size > Sum, an exception is thrown.
   * @param RandomSeed
   *          Holds the random seed used to generate the values.
   */
  public FrequencyDistribution(long Size, long Sum, long MinValue,
      long RandomSeed)
  {
    initialize(Size, Sum, MinValue, RandomSeed, 1.0);
  }

  /**
   * Instantiates a new FrequencyDistribution for integers 0 to Size - 1 whose
   * values are all greater than or equal to MinValue, sum to Sum, and are
   * generated using the random seed RandomSeed with a bias factor of
   * RandomFactor.
   * 
   * @param Size
   *          Holds the size of the distribution, values will be generated for
   *          integers from 0 to Size - 1.
   * @param Sum
   *          Holds the non-negative integer that the values generated must sum
   *          to.
   * @param MinValue
   *          Holds the minimum value of a distribution value; if MinValue *
   *          Size > Sum, an exception is thrown.
   * @param RandomSeed
   *          Holds the random seed used to generate the values.
   * @param RandomFactor
   *          A value from 0 to 1 that bias's the random values generated.
   */
  public FrequencyDistribution(long Size, long Sum, long MinValue,
      long RandomSeed, double RandomFactor)
  {
    initialize(Size, Sum, MinValue, RandomSeed, RandomFactor);
  }

  /**
   * Initializes a new FrequencyDistribution.
   * 
   * @param Size
   *          Holds the size of the distribution, values will be generated for
   *          integers from 0 to Size - 1.
   * @param Sum
   *          Holds the non-negative integer that the values generated must sum
   *          to.
   * @param MinValue
   *          Holds the minimum value of a distribution value; if MinValue *
   *          Size > Sum, an exception is thrown.
   * @param RandomSeed
   *          Holds the random seed used to generate the values.
   * @param RandomFactor
   *          A value from 0 to 1 that bias's the random values generated.
   */
  private void initialize(long Size, long Sum, long MinValue, long RandomSeed,
      double RandomFactor)
  {
    /* force Size to be non-negative. */
    size = Math.abs(Size);

    /* force Sum to be non-negative. */
    sum = Math.abs(Sum);

    /* force minValue to be non-negative. */
    minValue = Math.abs(MinValue);

    /* ensure size and sum are in a valid range. */
    if (((size == 0) && (sum > 0)))
    {
      // programming error, throw an exception.
      throw new RuntimeException(
          "parameter size is zero but expected sum is positive.");
    }
    if (size * minValue > sum)
    {
      // programming error, throw an exception.
      throw new RuntimeException("parameter size (" + size
          + ") times the min value (" + minValue
          + ") is greater than the expected sum (" + sum + ").");
    }

    // reduce sum to add minValue to all values.
    sum -= size * minValue;

    /*
     * set the random seed and create the random number generator; seed for the
     * random generator is reset when computing values.
     */
    randomSeed = RandomSeed;
    randomGenerator = new Random();

    /* get the factor to scale the random numbers by. */
    randomFactor = Math.min(1.0, Math.abs(RandomFactor));

    return;
  }

  /**
   * Returns the value in the distribution for Index; Index must be from 0 to
   * Size - 1 or an exception is thrown. Computes the value in lg(Size) steps
   * using lg(Size) memory.
   * 
   * @param Index
   *          Holds the index whose value is computed; Index must be from 0 to
   *          Size - 1.
   * @return Returns the value in the distribution for Index.
   */
  public final long getValue(long Index)
  {
    /* always reset the random generator for consistency. */
    randomGenerator.setSeed(randomSeed);

    /* ensure Index is in a valid range. */
    if (Index < 0)
    {
      /* programming error, throw an exception. */
      throw new RuntimeException("parameter Index (" + Index
          + ") must be non-negative.");
    }

    if (Index >= size)
    {
      /* programming error, throw an exception. */
      throw new RuntimeException("parameter Index (" + Index
          + ") must be non-negative.");
    }

    /* call recursive function to get value. */
    return minValue + _getValue(Index, size, sum);
  }

  /**
   * _getValue is a recursive function that computes the value of the
   * distribution for Index with no error bounds checking.
   * 
   * @param Index
   *          Holds the Index in the distribution whose value is to be returned.
   * @param Size
   *          Holds the total number of values in the distribution.
   * @param Sum
   *          Holds the sum of the values to generate..
   * 
   * @return Returns the value for Index in the distribution.
   */
  private final long _getValue(long Index, long Size, long Sum)
  {
    /* if the number of values left is zero, return zero. */
    if ((Sum < 1) || (Size < 2))
      return Sum;

    /* get the middle index for the total components. */
    long middle = Size / 2L;

    /* get the number of nodes for the left half of the divided nodes. */
    long leftNodes = (long) (Sum * randomGenerator.nextDouble() * randomFactor);

    if (Index < middle)
    {
      /* get the value for the left half. */
      return _getValue(Index, middle, leftNodes);
    } else
    {
      /* get the value for the right half. */
      return _getValue(Index - middle, Size - middle, Sum - leftNodes);
    }
  }

  /**
   * dump prints to stdout all pairs index,value of the distribution.
   */
  public final void dump()
  {
    for (long value = 0; value < size; value++)
    {
      System.out.println(value + "," + this.getValue(value));
    }
  }

  /**
   * runUnitTests executes NumberOfTests unit tests.
   * 
   * @param NumberOfTests
   *          Holds the number of unit tests to run.
   * 
   * @return Returns true if successful.
   */
  public final boolean runUnitTests(long NumberOfTests)
  {
    // get the number of tests to run.
    NumberOfTests = Math.abs(NumberOfTests);

    for (long testNumber = 0; testNumber < NumberOfTests; testNumber++)
    {
      // get a random size and sum for the distribution.
      long size = 1 + (long) (10000 * randomGenerator.nextDouble());
      long sum = 1 + (long) (size * randomGenerator.nextDouble());

      // create the distribution.
      FrequencyDistribution distribution = new FrequencyDistribution(size, sum,
          0, testNumber, 1);

      // check that the sum of the values is correct.
      long computeTotalValues = 0;
      for (long value = 0; value < size; value++)
      {
        computeTotalValues += distribution.getValue(value);
      }

      // return false if the sums are unequal.
      if (computeTotalValues != sum)
        return false;
    }

    return true;
  }

  /**
   * Holds the total number of unique values to generate; forced to be
   * non-negative.
   */
  private long size;

  /** Holds the total number of values to generate; forced to be non-negative. */
  private long sum;

  /** Holds the min value of values to generate; forced to be non-negative. */
  private long minValue;

  /**
   * Holds the random seed used by the random number generator; randomGenerator
   * must be re-seeded for each call to getValueSize.
   */
  private long randomSeed;

  /** Holds the random number generator. */
  private Random randomGenerator;

  /** Holds the factor used to scale the random numbers generated. */
  private double randomFactor;
}

