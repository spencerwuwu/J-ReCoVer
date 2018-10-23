// https://searchcode.com/api/result/111496283/

///////////////////////////////////////////////////////////////////////////////
//  Filename: $RCSfile: Symmetry.java,v $
//  Purpose:  Brute force symmetry analyzer.
//  Language: Java
//  Compiler: JDK 1.4
//  Authors:  Joerg Kurt Wegner
//  Original author: (C) 1996, 2003 S. Patchkovskii, Serguei.Patchkovskii@sympatico.ca
//  Version:  $Revision: 1.10 $
//            $Date: 2005/02/17 16:48:35 $
//            $Author: wegner $
//
// Copyright Symmetry:       S. Patchkovskii, 1996,2000,2003
// Copyright OELIB:          OpenEye Scientific Software, Santa Fe,
//                           U.S.A., 1999,2000,2001
// Copyright JOELIB/JOELib2: Dept. Computer Architecture, University of
//                           Tuebingen, Germany, 2001,2002,2003,2004,2005
// Copyright JOELIB/JOELib2: ALTANA PHARMA AG, Konstanz, Germany,
//                           2003,2004,2005
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
///////////////////////////////////////////////////////////////////////////////
package joelib2.math.symmetry;

import joelib2.io.BasicIOType;
import joelib2.io.BasicIOTypeHolder;
import joelib2.io.MoleculeFileHelper;
import joelib2.io.MoleculeFileIO;
import joelib2.io.MoleculeIOException;

import joelib2.molecule.Atom;
import joelib2.molecule.BasicConformerMolecule;
import joelib2.molecule.Molecule;

import joelib2.sort.QuickInsertSort;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Category;


/**
 * Brute force symmetry analyzer. All specific structures should have
 * corresponding elements in the same position generic structure does. <br>
 * <br>
 * Planes are characterized by the surface normal direction (taken in the
 * direction <b>from </b> the coordinate origin) and distance from the
 * coordinate origin to the plane in the direction of the surface normal. <br>
 * <br>
 * Inversion is characterized by location of the inversion center. <br>
 * <br>
 * Rotation is characterized by a vector (distance+direction) from the origin to
 * the rotation axis, axis direction and rotation order. Rotations are in the
 * clockwise direction looking opposite to the direction of the axis. Note that
 * this definition of the rotation axis is <b>not </b> unique, since an
 * arbitrary multiple of the axis direction can be added to the position vector
 * without changing actual operation. <br>
 * <br>
 * Mirror rotation is defined by the same parameters as normal rotation, but the
 * origin is now unambiguous since it defines the position of the plane
 * associated with the axis. <br>
 * <br>
 * <p>
 * <blockquote>
 *
 * <pre>
 *
 *
 *
 *
 *     Usage:
 *     java -cp . joelib2.math.symmetry.Symmetry [options] &lt;SDF input file&gt;
 *
 *     Options:
 *     -maxaxisorder (20)      Maximum order of rotation axis to look for
 *     -maxoptcycles (200)     Maximum allowed number of cycles in symmetry element optimization
 *
 *     Defaults values should be ok for the following parameters:
 *     -same         (0.0010)  Atoms are colliding if distance falls below this value
 *     -primary      (0.05)    Initial loose criterion for atom equivalence
 *     -final        (0.05)    Final criterion for atom equivalence
 *     -maxoptstep   (0.5)     Largest step allowed in symmetry element optimization
 *     -minoptstep   (1.0E-7)  Termination criterion in symmetry element optimization
 *     -gradstep     (1.0E-7)  Finite step used in numeric gradient evaluation
 *     -minchange    (1.0E-10) Minimum allowed change in target methodName
 *     -minchgcycles (5)       Number of minchange cycles before optimization stops
 *
 *
 *
 *
 * </pre>
 *
 * </blockquote>
 *
 * @.author Serguei Patchkovskii
 * @.author wegnerj
 * @.license GPL
 * @.cvsversion $Revision: 1.10 $, $Date: 2005/02/17 16:48:35 $
 */
public class Symmetry
{
    //~ Static fields/initializers /////////////////////////////////////////////

    // /////////////////////////////////////////////

    // Obtain a suitable logger.
    private static Category logger = Category.getInstance(Symmetry.class
            .getName());

    public static final double TOLERANCE_SAME_DEFAULT = 1e-3;

    public static final double TOLERANCE_PRIMARY_DEFAULT = 5e-2;

    public static final double TOLERANCE_FINAL_DEFAULT = 5e-2;

    public static final double MAXOPT_STEP_DEFAULT = 5e-1;

    public static final double MINOPT_STEP_DEFAULT = 1e-7;

    public static final double GRADIENT_STEP_DEFAULT = 1e-7;

    public static final double OPTCHANGE_THRESHOLD_DEFAULT = 1e-10;

    public static final int MAX_OPT_CYCLES_DEFAULT = 200;

    public static final int OPT_CHANGE_HITS_DEFAULT = 5;

    public static final int MAX_AXIS_ORDER_DEFAULT = 20;

    private static final double M_PI =
        3.1415926535897932384626433832795028841971694;

    private static final int MAXPARAM = 7;

    //~ Instance fields ////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////

    private SymAtom[] atoms = null;

    private int atomsCount = 0;

    private boolean badOptimization = false;

    private double[] centerOfSomething = new double[SymAtom.DIMENSION];

    private double[] distanceFromCenter = null;

    /**
     * Finite step used in numeric gradient evaluation.
     */
    private double gradientStep = GRADIENT_STEP_DEFAULT;

    //private double toleranceFinal = 1e-3;
    private SymmetryElement[] improperAxes = null;

    private int improperAxesCount = 0;

    private int[] improperAxesCounts = null;

    private SymmetryElement[] inversionCenters = null;

    private int inversionCentersCount = 0;

    /**
     * Maximum order of rotation axis to look for.
     */
    private int maxAxisOrder = MAX_AXIS_ORDER_DEFAULT;

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    private int maxOptCycles = MAX_OPT_CYCLES_DEFAULT;

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    private double maxOptStep = MAXOPT_STEP_DEFAULT;

    /**
     * Termination criterion in symmetry element optimization.
     */
    private double minOptStep = MINOPT_STEP_DEFAULT;

    private SymmetryElement molecularPlane = null;

    private SymmetryElement[] normalAxes = null;

    private int normalAxesCount = 0;

    private int[] normalAxesCounts = null;

    /**
     * Number of minchange cycles before optimization stops.
     */
    private int optChangeHits = OPT_CHANGE_HITS_DEFAULT;

    /**
     * Minimum allowed change in target methodName.
     */
    private double optChangeThreshold = OPTCHANGE_THRESHOLD_DEFAULT;

    private SymmetryElement[] planes = null;

    private int planesCount = 0;

    private char[] pointGroupRejectionReason = null;

    private SymStatistic statistic = new SymStatistic();

    private String symmetryCode = null;

    /**
     * Final criterion for atom equivalence.
     */
    private double toleranceFinal = TOLERANCE_FINAL_DEFAULT;

    /**
     * Initial loose criterion for atom equivalence.
     */
    private double tolerancePrimary = TOLERANCE_PRIMARY_DEFAULT;

    /**
     * Atoms are colliding if distance falls below this value.
     */
    private double toleranceSame = TOLERANCE_SAME_DEFAULT;

    //~ Constructors ///////////////////////////////////////////////////////////

    // ///////////////////////////////////////////////////////////

    /**
     * Brute force symmetry analyzer.
     */
    public Symmetry()
    {
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////////////

    /**
     * The main program for the symmetry class.
     *
     * @param args
     *            The command line arguments
     */
    public static void main(String[] args)
    {
        Symmetry symmetry = new Symmetry();

        if (args.length > 1)
        {
            parseMultipleArguments(symmetry, args);
        }
        else if (args.length == 1)
        {
            if (args[0].equalsIgnoreCase("--help") ||
                    args[0].equalsIgnoreCase("-help") ||
                    args[0].equalsIgnoreCase("-h") ||
                    args[0].equalsIgnoreCase("-?"))
            {
                symmetry.usage();
            }
        }

        if (args.length >= 1)
        {
            String filename = args[args.length - 1];
            calculateSymmetry(symmetry, filename);

            System.exit(0);
        }
        else
        {
            System.err.println("No SDF input file defined. Use -h for help.");
        }
    }

    public void findSymmetryElements() throws SymmetryException
    {
        findSymmetryElements(false);
    }

    public void findSymmetryElements(boolean showInfo) throws SymmetryException
    {
        findCenterOfSomething();

        if (showInfo)
        {
            System.out.println("Looking for the inversion center");
        }

        findInversionCenters();

        if (showInfo)
        {
            reportAndResetCounters();
            System.out.println("Looking for the planes of symmetry");
        }

        findPlanes();

        if (showInfo)
        {
            reportAndResetCounters();
            System.out.println("Looking for infinity axis");
        }

        findInfinityAxis();

        if (showInfo)
        {
            reportAndResetCounters();
            System.out.println("Looking for C2 axes");
        }

        findC2Axes();

        if (showInfo)
        {
            reportAndResetCounters();
            System.out.println("Looking for higher axes");
        }

        findHigherAxes();

        if (showInfo)
        {
            reportAndResetCounters();
            System.out.println("Looking for the improper axes");
        }

        findImproperAxes();

        if (showInfo)
        {
            reportAndResetCounters();
        }

        sortSymmetryElements();
        summarizeSymmetryElements();
        buildSymmetryCode();
    }

    /**
     * Gets the normal axis of the molecule. Relevant elements are
     * {@link joelib2.math.symmetry.SymmetryElement#maxdev},
     * {@link joelib2.math.symmetry.SymmetryElement#order},
     * {@link joelib2.math.symmetry.SymmetryElement#normal}and
     * {@link joelib2.math.symmetry.SymmetryElement#distance}.<br>
     * A order of <tt>0</tt> represents an infinity normal axis. The position
     * of the normal axis can be calculated with:
     * <tt>x=normalAxes.distance*normalAxes.normal[0]</tt><br>
     * <tt>y=normalAxes.distance*normalAxes.normal[1]</tt><br>
     * <tt>z=normalAxes.distance*normalAxes.normal[2]</tt>
     *
     * @return SymmetryElement[] normal axis
     */
    public SymmetryElement[] getAxes()
    {
        SymmetryElement[] axes = null;

        if (normalAxesCount != 0)
        {
            axes = normalAxes;
        }

        return axes;
    }

    public boolean getBadOptimization()
    {
        return badOptimization;
    }

    /**
     * Finite step used in numeric gradient evaluation.
     */
    public double getGradientStep()
    {
        return gradientStep;
    }

    /**
     * Gets the improper axis of the molecule. Relevant elements are
     * {@link joelib2.math.symmetry.SymmetryElement#maxdev},
     * {@link joelib2.math.symmetry.SymmetryElement#order},
     * {@link joelib2.math.symmetry.SymmetryElement#normal}and
     * {@link joelib2.math.symmetry.SymmetryElement#distance}.<br>
     * A order of <tt>0</tt> represents an infinity normal axis. The position
     * of the improper axis can be calculated with:
     * <tt>x=normalAxes.distance*normalAxes.normal[0]</tt><br>
     * <tt>y=normalAxes.distance*normalAxes.normal[1]</tt><br>
     * <tt>z=normalAxes.distance*normalAxes.normal[2]</tt>
     *
     * @return SymmetryElement[] improper axis
     */
    public SymmetryElement[] getImproperAxes()
    {
        SymmetryElement[] axes = null;

        if (improperAxesCount != 0)
        {
            axes = improperAxes;
        }

        return axes;
    }

    /**
     * Gets the inversion centre of the molecule. Relevant elements are
     * {@link joelib2.math.symmetry.SymmetryElement#maxdev},
     * {@link joelib2.math.symmetry.SymmetryElement#normal}and
     * {@link joelib2.math.symmetry.SymmetryElement#distance}.<br>
     * The position of the inversion center can be calculated with:
     * <tt>x=inversionCenters.distance*inversionCenters.normal[0]</tt><br>
     * <tt>y=inversionCenters.distance*inversionCenters.normal[1]</tt><br>
     * <tt>z=inversionCenters.distance*inversionCenters.normal[2]</tt>
     *
     * @return SymmetryElement[] inversion center
     */
    public SymmetryElement getInversionCenter()
    {
        if (inversionCentersCount == 0)
        {
            return null;
        }

        return inversionCenters[0];
    }

    /**
     * Maximum order of rotation axis to look for.
     */
    public int getMaxAxisOrder()
    {
        return maxAxisOrder;
    }

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    public int getMaxOptCycles()
    {
        return maxOptCycles;
    }

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    public double getMaxOptStep()
    {
        return maxOptStep;
    }

    /**
     * Termination criterion in symmetry element optimization.
     */
    public double getMinOptStep()
    {
        return minOptStep;
    }

    /**
     * Number of minchange cycles before optimization stops
     */
    public int getOptChangeHits()
    {
        return optChangeHits;
    }

    /**
     * Minimum allowed change in target methodName.
     */
    public double getOptChangeThreshold()
    {
        return optChangeThreshold;
    }

    /**
     * Gets the symmetry planes of the molecule. Relevant elements are
     * {@link joelib2.math.symmetry.SymmetryElement#maxdev},
     * {@link joelib2.math.symmetry.SymmetryElement#normal}and
     * {@link joelib2.math.symmetry.SymmetryElement#distance}.
     *
     * @return SymmetryElement[] symmetry planes
     */
    public SymmetryElement[] getPlanes()
    {
        if (planesCount == 0)
        {
            return null;
        }

        return planes;
    }

    public String getSymmetryCode()
    {
        return symmetryCode;
    }

    /**
     * Final criterion for atom equivalence.
     */
    public double getToleranceFinal()
    {
        return toleranceFinal;
    }

    /**
     * Initial loose criterion for atom equivalence.
     */
    public double getTolerancePrimary()
    {
        return tolerancePrimary;
    }

    /**
     * Atoms are colliding if distance falls below this value.
     */
    public double getToleranceSame()
    {
        return toleranceSame;
    }

    public PointGroup identifyPointGroup()
    {
        return identifyPointGroup(false);
    }

    public PointGroup identifyPointGroup(boolean showInfo)
    {
        if (symmetryCode == null)
        {
            return null;
        }

        int i;
        int last_matching = -1;
        int matching_count = 0;
        String symmetryCodeS;

        for (i = 0; i < PointGroups.defaultPointGroups.length; i++)
        {
            //System.out.println("symmetryCode:'"+new
            // String(symmetryCode)+"'=='"+PointGroups.defaultPointGroups[i].getSymmetryCode()+"'");
            symmetryCodeS = symmetryCode;

            if (symmetryCodeS.equals(
                        PointGroups.defaultPointGroups[i].getSymmetryCode()) ==
                    true)
            {
                if (PointGroups.defaultPointGroups[i].getCheck() == true)
                {
                    last_matching = i;
                    matching_count++;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        System.out.println("It looks very much like " +
                            PointGroups.defaultPointGroups[i].getGroupName() +
                            ", but it is not since " +
                            String.valueOf(pointGroupRejectionReason));
                    }
                }
            }
        }

        if (matching_count == 0)
        {
            if (showInfo)
            {
                System.out.println(
                    "These symmetry elements match no point group I know of. Sorry.");
            }
        }

        if (matching_count > 1)
        {
            if (showInfo)
            {
                System.out.println(
                    "These symmetry elements match more than one group I know of.");
                System.out.println("Matching groups are:");

                for (i = 0; i < PointGroups.defaultPointGroups.length; i++)
                {
                    if ((symmetryCode.equals(
                                    PointGroups.defaultPointGroups[i]
                                    .getSymmetryCode()) == false) &&
                            (PointGroups.defaultPointGroups[i].getCheck() ==
                                true))
                    {
                        System.out.println("" +
                            PointGroups.defaultPointGroups[i].getGroupName());
                    }
                }
            }
        }

        if (matching_count == 1)
        {
            if (showInfo)
            {
                System.out.println("It seems to be the " +
                    PointGroups.defaultPointGroups[last_matching].getGroupName() +
                    " point group");
            }

            return PointGroups.defaultPointGroups[last_matching];
        }

        return null;
    }

    /**
     * Read coordinates from a single molecule.
     */
    public boolean readCoordinates(Molecule mol)
    {
        atomsCount = mol.getAtomsSize();

        if (logger.isDebugEnabled())
        {
            logger.debug("Atoms count = " + atomsCount);
        }

        atoms = new SymAtom[atomsCount];

        boolean coordsReaded = true;

        if (atoms == null)
        {
            logger.error("Out of memory for atoms coordinates");

            return false;
        }
        else
        {
            Atom atom;

            for (int atomIdx = 0; atomIdx < atomsCount; atomIdx++)
            {
                atom = mol.getAtom(atomIdx + 1);
                atoms[atomIdx] = new SymAtom();
                atoms[atomIdx].type = atom.getAtomicNumber();
                atoms[atomIdx].coord[0] = atom.get3Dx();
                atoms[atomIdx].coord[1] = atom.get3Dy();
                atoms[atomIdx].coord[2] = atom.get3Dz();
            }
        }

        return coordsReaded;
    }

    /**
     * Report symmetry elements.
     */
    public void reportSymmetryElementsVerbose()
    {
        reportInversionCenter();
        reportAxes();
        reportImproperAxes();
        reportPlanes();
    }

    public void setBadOptimization(boolean badOptimization)
    {
        this.badOptimization = badOptimization;
    }

    /**
     * Finite step used in numeric gradient evaluation.
     */
    public void setGradientStep(double _gradientStep)
    {
        gradientStep = _gradientStep;
    }

    /**
     * Maximum order of rotation axis to look for.
     */
    public void setMaxAxisOrder(int _maxAxisOrder)
    {
        maxAxisOrder = _maxAxisOrder;
    }

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    public void setMaxOptCycles(int _maxOptCycles)
    {
        maxOptCycles = _maxOptCycles;
    }

    /**
     * Maximum allowed number of cycles in symmetry element optimization.
     */
    public void setMaxOptStep(double _maxOptStep)
    {
        maxOptStep = _maxOptStep;
    }

    /**
     * Termination criterion in symmetry element optimization.
     */
    public void setMinOptStep(double _minOptStep)
    {
        minOptStep = _minOptStep;
    }

    /**
     * Number of minchange cycles before optimization stops
     */
    public void setOptChangeHits(int _optChangeHits)
    {
        optChangeHits = _optChangeHits;
    }

    /**
     * Minimum allowed change in target methodName.
     */
    public void setOptChangeThreshold(double _optChangeThreshold)
    {
        optChangeThreshold = _optChangeThreshold;
    }

    /**
     * Final criterion for atom equivalence.
     */
    public void setToleranceFinal(double _toleranceFinal)
    {
        toleranceFinal = _toleranceFinal;
    }

    /**
     * Initial loose criterion for atom equivalence.
     */
    public void setTolerancePrimary(double _tolerancePrimary)
    {
        tolerancePrimary = _tolerancePrimary;
    }

    /**
     * Atoms are colliding if distance falls below this value.
     */
    public void setToleranceSame(double _toleranceSame)
    {
        toleranceSame = _toleranceSame;
    }

    public void usage()
    {
        StringBuffer sb = new StringBuffer();
        String programName = this.getClass().getName();

        sb.append("Usage is :\n");
        sb.append("java -cp . ");
        sb.append(programName);
        sb.append(" [options]");
        sb.append(" <SDF input file>");
        sb.append("\n\n");
        sb.append("Options:\n");

        sb.append("  -maxaxisorder (" + this.getMaxAxisOrder() +
            ") Maximum order of rotation axis to look for\n");

        sb.append("  -maxoptcycles (" + this.getMaxOptCycles() +
            ") Maximum allowed number of cycles in symmetry element optimization\n" +
            "\nDefaults values should be ok for the following parameters:\n");

        sb.append("  -same         (" + this.getToleranceSame() +
            ") Atoms are colliding if distance falls below this value\n");

        sb.append("  -primary      (" + this.getTolerancePrimary() +
            ") Initial loose criterion for atom equivalence\n");

        sb.append("  -final        (" + this.getToleranceFinal() +
            ") Final criterion for atom equivalence\n");

        sb.append("  -maxoptstep   (" + this.getMaxOptStep() +
            ") Largest step allowed in symmetry element optimization\n");

        sb.append("  -minoptstep   (" + this.getMinOptStep() +
            ") Termination criterion in symmetry element optimization\n");

        sb.append("  -gradstep     (" + this.getGradientStep() +
            ") Finite step used in numeric gradient evaluation\n");

        sb.append("  -minchange    (" + this.getOptChangeThreshold() +
            ") Minimum allowed change in target methodName\n");

        sb.append("  -minchgcycles (" + this.getOptChangeHits() +
            ") Number of minchange cycles before optimization stops\n");

        sb.append("\n Note that only primitive rotations will be reported\n");
        sb.append(
            " \nThis is version $Revision: 1.10 $ ($Date: 2005/02/17 16:48:35 $)\n");

        System.out.println(sb.toString());

        System.exit(0);
    }

    protected String buildSymmetryCode()
    {
        int i;
        StringBuffer symmetryCode = new StringBuffer(10 *
                (planesCount + normalAxesCount + improperAxesCount +
                    inversionCentersCount + 2));

        if ((planesCount + normalAxesCount + improperAxesCount +
                    inversionCentersCount) == 0)
        {
            symmetryCode.append("<no symmetry elements>");

            //System.out.println("Molecule has no symmetry elements\n");
        }
        else
        {
            //System.out.println("Molecule has the following symmetry elements:
            // ");
            if (inversionCentersCount > 0)
            {
                symmetryCode.append("(i) ");
            }

            if (normalAxesCounts[0] == 1)
            {
                symmetryCode.append("(Cinf) ");
            }

            if (normalAxesCounts[0] > 1)
            {
                symmetryCode.append(normalAxesCounts[0]);
                symmetryCode.append("*(Cinf) ");
            }

            for (i = getMaxAxisOrder(); i >= 2; i--)
            {
                if (normalAxesCounts[i] == 1)
                {
                    symmetryCode.append("(C");
                    symmetryCode.append(i);
                    symmetryCode.append(") ");
                }

                if (normalAxesCounts[i] > 1)
                {
                    symmetryCode.append(normalAxesCounts[i]);
                    symmetryCode.append("*(C");
                    symmetryCode.append(i);
                    symmetryCode.append(") ");
                }
            }

            for (i = getMaxAxisOrder(); i >= 2; i--)
            {
                if (improperAxesCounts[i] == 1)
                {
                    symmetryCode.append("(S");
                    symmetryCode.append(i);
                    symmetryCode.append(") ");
                }

                if (improperAxesCounts[i] > 1)
                {
                    symmetryCode.append(improperAxesCounts[i]);
                    symmetryCode.append("*(S");
                    symmetryCode.append(i);
                    symmetryCode.append(") ");
                }
            }

            if (planesCount == 1)
            {
                symmetryCode.append("(sigma) ");
            }

            if (planesCount > 1)
            {
                symmetryCode.append(planesCount);
                symmetryCode.append("*(sigma) ");
            }
        }

        return symmetryCode.toString();
    }

    //
    //   Inversion-center specific functions
    //
    protected void invertAtom(SymmetryElement center, SymAtom from, SymAtom to)
    {
        int i;

        to.type = from.type;

        for (i = 0; i < SymAtom.DIMENSION; i++)
        {
            to.coord[i] = (2 * center.distance * center.normal[i]) -
                from.coord[i];
        }
    }

    //
    //   Plane-specific functions
    //
    protected void mirrorAtom(SymmetryElement plane, SymAtom from, SymAtom to)
        throws SymmetryException
    {
        int i;
        double r;

        for (i = 0, r = plane.distance; i < SymAtom.DIMENSION; i++)
        {
            r -= (from.coord[i] * plane.normal[i]);
        }

        to.type = from.type;

        for (i = 0; i < SymAtom.DIMENSION; i++)
        {
            to.coord[i] = from.coord[i] + (2 * r * plane.normal[i]);
        }
    }

    //
    //   Normal rotation axis-specific routines.
    //
    protected void rotateAtom(SymmetryElement axis, SymAtom from, SymAtom to)
        throws SymmetryException
    {
        double[] x = new double[3];
        double[] y = new double[3];
        double[] a = new double[3];
        double[] b = new double[3];
        double[] c = new double[3];
        double angle = (axis.order != 0) ? ((2 * M_PI) / axis.order) : 1.0;
        double a_sin = Math.sin(angle);
        double a_cos = Math.cos(angle);
        double dot;
        int i;

        if (SymAtom.DIMENSION != 3)
        {
            throw new SymmetryException("Catastrophe in rotate_atom!");
        }

        for (i = 0; i < 3; i++)
        {
            x[i] = from.coord[i] - (axis.distance * axis.normal[i]);
        }

        for (i = 0, dot = 0; i < 3; i++)
        {
            dot += (x[i] * axis.direction[i]);
        }

        for (i = 0; i < 3; i++)
        {
            a[i] = axis.direction[i] * dot;
        }

        for (i = 0; i < 3; i++)
        {
            b[i] = x[i] - a[i];
        }

        c[0] = (b[1] * axis.direction[2]) - (b[2] * axis.direction[1]);
        c[1] = (b[2] * axis.direction[0]) - (b[0] * axis.direction[2]);
        c[2] = (b[0] * axis.direction[1]) - (b[1] * axis.direction[0]);

        for (i = 0; i < 3; i++)
        {
            y[i] = a[i] + (b[i] * a_cos) + (c[i] * a_sin);
        }

        for (i = 0; i < 3; i++)
        {
            to.coord[i] = y[i] + (axis.distance * axis.normal[i]);
        }

        to.type = from.type;
    }

    //
    //   Improper axes-specific routines.
    //   These are obtained by slight modifications of normal rotation
    //       routines.
    //
    protected void rotateReflectAtom(SymmetryElement axis, SymAtom from,
        SymAtom to) throws SymmetryException
    {
        double[] x = new double[3];
        double[] y = new double[3];
        double[] a = new double[3];
        double[] b = new double[3];
        double[] c = new double[3];
        double angle = (2 * M_PI) / axis.order;
        double a_sin = Math.sin(angle);
        double a_cos = Math.cos(angle);
        double dot;
        int i;

        if (SymAtom.DIMENSION != 3)
        {
            throw new SymmetryException("Catastrophe in rotate_reflect_atom!");
        }

        for (i = 0; i < 3; i++)
        {
            x[i] = from.coord[i] - (axis.distance * axis.normal[i]);
        }

        for (i = 0, dot = 0; i < 3; i++)
        {
            dot += (x[i] * axis.direction[i]);
        }

        for (i = 0; i < 3; i++)
        {
            a[i] = axis.direction[i] * dot;
        }

        for (i = 0; i < 3; i++)
        {
            b[i] = x[i] - a[i];
        }

        c[0] = (b[1] * axis.direction[2]) - (b[2] * axis.direction[1]);
        c[1] = (b[2] * axis.direction[0]) - (b[0] * axis.direction[2]);
        c[2] = (b[0] * axis.direction[1]) - (b[1] * axis.direction[0]);

        for (i = 0; i < 3; i++)
        {
            y[i] = -a[i] + (b[i] * a_cos) + (c[i] * a_sin);
        }

        for (i = 0; i < 3; i++)
        {
            to.coord[i] = y[i] + (axis.distance * axis.normal[i]);
        }

        to.type = from.type;
    }

    // OUTSOURCED to SymAxesComparator !!!
    //  private int compareAxes(
    //          final SymmetryElement a[],
    //          final SymmetryElement b[])
    //  {
    //          SymmetryElement axis_a = a[0];
    //          SymmetryElement axis_b = b[0];
    //          int i, order_a, order_b;
    //
    //          order_a = axis_a.order;
    //          if (order_a == 0)
    //                  order_a = 10000;
    //          order_b = axis_b.order;
    //          if (order_b == 0)
    //                  order_b = 10000;
    //          if ((i = order_b - order_a) != 0)
    //                  return i;
    //          if (axis_a.maxdev > axis_b.maxdev)
    //                  return -1;
    //          if (axis_a.maxdev < axis_b.maxdev)
    //                  return 1;
    //          return 0;
    //  }
    protected void sortSymmetryElements()
    {
        QuickInsertSort sorting = new QuickInsertSort();
        SymAxesComparator axesComparator = new SymAxesComparator();

        if (planesCount > 1)
        {
            sorting.sort(planes, axesComparator);
        }

        if (normalAxesCount > 1)
        {
            sorting.sort(normalAxes, axesComparator);
        }

        if (improperAxesCount > 1)
        {
            sorting.sort(improperAxes, axesComparator);
        }
    }

    protected void summarizeSymmetryElements()
    {
        int i;

        normalAxesCounts = new int[getMaxAxisOrder() + 1];
        improperAxesCounts = new int[getMaxAxisOrder() + 1];

        for (i = 0; i < normalAxesCount; i++)
        {
            if (normalAxes[i] != null)
            {
                normalAxesCounts[normalAxes[i].order]++;
            }
        }

        for (i = 0; i < improperAxesCount; i++)
        {
            if (improperAxes[i] != null)
            {
                improperAxesCounts[improperAxes[i].order]++;
            }
        }
    }

    void findC2Axes() throws SymmetryException
    {
        int i;
        int j;
        int k;
        int l;
        int m;
        double[] center = new double[SymAtom.DIMENSION];
        double[] distances = new double[atomsCount];
        double r;
        SymmetryElement axis;

        if (distances == null)
        {
            throw new SymmetryException("Out of memory in find_c2_axes()");
        }

        if (normalAxes == null)
        {
            normalAxes = new SymmetryElement[1];
        }

        for (i = 1; i < atomsCount; i++)
        {
            for (j = 0; j < i; j++)
            {
                if (atoms[i].type != atoms[j].type)
                {
                    continue;
                }

                //A very cheap, but quite effective check
                if (Math.abs(distanceFromCenter[i] - distanceFromCenter[j]) >
                        getTolerancePrimary())
                {
                    continue;
                }

                //
                //   First, let's try to get it cheap and use CenterOfSomething
                //
                for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
                {
                    center[k] = (atoms[i].coord[k] + atoms[j].coord[k]) / 2;
                    r += pow2(center[k] - centerOfSomething[k]);
                }

                r = Math.sqrt(r);

                // It's Ok to use CenterOfSomething
                if (r > (5 * getTolerancePrimary()))
                {
                    if ((axis = initC2Axis(i, j, centerOfSomething)) != null)
                    {
                        normalAxesCount++;

                        SymmetryElement[] tmpAxes =
                            new SymmetryElement[normalAxesCount];
                        System.arraycopy(normalAxes, 0, tmpAxes, 0,
                            normalAxesCount - 1);
                        normalAxes = tmpAxes;

                        if (normalAxes == null)
                        {
                            throw new SymmetryException(
                                "Out of memory in find_c2_axes");
                        }

                        normalAxes[normalAxesCount - 1] = axis;
                    }

                    continue;
                }

                //
                //  Now, C2 axis can either pass through an atom, or through the
                //  middle of the other pair.
                //
                for (k = 0; k < atomsCount; k++)
                {
                    if ((axis = initC2Axis(i, j, atoms[k].coord)) != null)
                    {
                        normalAxesCount++;

                        SymmetryElement[] tmpAxes =
                            new SymmetryElement[normalAxesCount];
                        System.arraycopy(normalAxes, 0, tmpAxes, 0,
                            normalAxesCount - 1);
                        normalAxes = tmpAxes;

                        if (normalAxes == null)
                        {
                            throw new SymmetryException(
                                "Out of memory in find_c2_axes");
                        }

                        normalAxes[normalAxesCount - 1] = axis;
                    }
                }

                //
                //  Prepare data for an additional pre-screening check
                //
                for (k = 0; k < atomsCount; k++)
                {
                    for (l = 0, r = 0; l < SymAtom.DIMENSION; l++)
                    {
                        r += pow2(atoms[k].coord[l] - center[l]);
                    }

                    distances[k] = Math.sqrt(r);
                }

                for (k = 0; k < atomsCount; k++)
                {
                    for (l = 0; l < atomsCount; l++)
                    {
                        if (atoms[k].type != atoms[l].type)
                        {
                            continue;
                        }

                        // We really need this one to run reasonably fast!
                        if ((Math.abs(
                                        distanceFromCenter[k] -
                                        distanceFromCenter[l]) >
                                    getTolerancePrimary()) ||
                                (Math.abs(distances[k] - distances[l]) >
                                    getTolerancePrimary()))
                        {
                            continue;
                        }

                        for (m = 0; m < SymAtom.DIMENSION; m++)
                        {
                            center[m] = (atoms[k].coord[m] +
                                    atoms[l].coord[m]) / 2;
                        }

                        if ((axis = initC2Axis(i, j, center)) != null)
                        {
                            normalAxesCount++;

                            SymmetryElement[] tmpAxes =
                                new SymmetryElement[normalAxesCount];
                            System.arraycopy(normalAxes, 0, tmpAxes, 0,
                                normalAxesCount - 1);
                            normalAxes = tmpAxes;

                            if (normalAxes == null)
                            {
                                throw new SymmetryException(
                                    "Out of memory in find_c2_axes");
                            }

                            normalAxes[normalAxesCount - 1] = axis;
                        }
                    }
                }
            }
        }

        distances = null;
    }

    /**
     * @param symmetry
     * @param filename
     */
    private static void calculateSymmetry(Symmetry symmetry, String filename)
    {
        BasicIOType inType = BasicIOTypeHolder.instance().getIOType("SDF");
        FileInputStream in = null;
        MoleculeFileIO loader = null;
        Molecule mol = null;

        try
        {
            in = new FileInputStream(filename);
            loader = MoleculeFileHelper.getMolReader(in, inType);
        }
        catch (FileNotFoundException ex)
        {
            logger.error("Can not find input file: " + filename);
            System.exit(1);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if (!loader.readable())
        {
            logger.error(inType.getRepresentation() + " is not readable.");
            logger.error("You're invited to write one !;-)");
            System.exit(1);
        }

        boolean success = true;

        for (;;)
        {
            mol = new BasicConformerMolecule(inType, inType);
            mol.clear();

            try
            {
                success = loader.read(mol);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
                System.exit(1);
            }
            catch (MoleculeIOException ex)
            {
                ex.printStackTrace();
                logger.info("Molecule was skipped: " + mol.getTitle());

                continue;
            }

            if (!success)
            {
                break;
            }
            else
            {
                if (!symmetry.readCoordinates(mol))
                {
                    System.err.println("Error reading in atomic coordinates\n");
                    System.exit(1);
                }
            }
        }

        // start symmetry estimation
        try
        {
            symmetry.findSymmetryElements(true);
        }
        catch (SymmetryException e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        if (symmetry.getBadOptimization())
        {
            System.out.println(
                "Refinement of some symmetry elements was terminated before convergence was reached.\n" +
                "Some symmetry elements may remain unidentified.\n");
        }

        symmetry.reportSymmetryElementsVerbose();
        System.out.println("Molecule has the following symmetry elements:\n" +
            symmetry.getSymmetryCode());

        PointGroup pointGroup = symmetry.identifyPointGroup();

        if (pointGroup != null)
        {
            System.out.println("It seems to be the " +
                pointGroup.getGroupName() + " point group");
        }
        else
        {
            System.out.println(
                "These symmetry elements match more than one group I know of.");
        }
    }

    /**
     * @param args
     */
    private static void parseMultipleArguments(Symmetry symmetry, String[] args)
    {
        for (int i = 0; i < (args.length - 1); i++)
        {
            if (args[i].equalsIgnoreCase("--help") ||
                    args[0].equalsIgnoreCase("-help") ||
                    args[i].equalsIgnoreCase("-h") ||
                    args[i].equalsIgnoreCase("-?"))
            {
                symmetry.usage();
            }

            if (args[i].equalsIgnoreCase("-minchgcycles"))
            {
                i++;
                symmetry.setOptChangeHits(Integer.parseInt(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-minchange"))
            {
                i++;
                symmetry.setOptChangeThreshold(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-same"))
            {
                i++;
                symmetry.setToleranceSame(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-primary"))
            {
                i++;
                symmetry.setTolerancePrimary(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-final"))
            {
                i++;
                symmetry.setToleranceFinal(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-maxoptstep"))
            {
                i++;
                symmetry.setMaxOptStep(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-minoptstep"))
            {
                i++;
                symmetry.setMinOptStep(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-gradstep"))
            {
                i++;
                symmetry.setGradientStep(Double.parseDouble(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-maxoptcycles"))
            {
                i++;
                symmetry.setMaxOptCycles(Integer.parseInt(args[i]));
            }
            else if (args[i].equalsIgnoreCase("-maxaxisorder"))
            {
                i++;
                symmetry.setMaxAxisOrder(Integer.parseInt(args[i]));
            }
            else
            {
                System.err.println("Unrecognized option \"%s\"\n" + args[i]);
                System.exit(1);
            }
        }
    }

    private SymmetryElement allocSymmetryElement()
    {
        return new SymmetryElement(atomsCount);
    }

    private int checkTransformOrder(SymmetryElement elem)
        throws SymmetryException
    {
        int i;
        int j;
        int k;
        TransformationAtom rotate_reflect_atom = new TransformationAtom(
                "rotateReflectAtom");

        for (i = 0; i < atomsCount; i++)
        {
            //Identity transform is Ok for any order
            if (elem.transform[i] == i)
            {
                continue;
            }

            if (elem.transformAtomMethod.equals(rotate_reflect_atom))
            {
                j = elem.transform[i];

                if (elem.transform[j] == i)
                {
                    //Second-order transform is Ok for improper axis
                    continue;
                }
            }

            for (j = elem.order - 1, k = elem.transform[i]; j > 0;
                    j--, k = elem.transform[k])
            {
                if (k == i)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("transform looped " + j +
                            " steps too early from atom " + i);
                    }

                    return -1;
                }
            }

            if ((k != i) &&
                    elem.transformAtomMethod.equals(rotate_reflect_atom))
            {
                // For improper axes, the complete loop may also take twice the
                // order
                for (j = elem.order; j > 0; j--, k = elem.transform[k])
                {
                    if (k == i)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("(improper) transform looped " + j +
                                " steps too early from atom " + i);
                        }

                        return -1;
                    }
                }
            }

            if (k != i)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("transform failed to loop after " +
                        elem.order + " steps from atom " + i);
                }

                return -1;
            }
        }

        return 0;
    }

    private int checkTransformQuality(SymmetryElement elem)
        throws SymmetryException
    {
        int i;
        int j;
        int k;
        SymAtom symmetric = new SymAtom();
        double r = 0.0;
        double max_r;

        for (i = 0, max_r = 0; i < atomsCount; i++)
        {
            j = elem.transform[i];
            elem.transformAtom(this, elem, atoms[i], symmetric);

            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                r += pow2(symmetric.coord[k] - atoms[j].coord[k]);
            }

            r = Math.sqrt(r);

            if (r > getToleranceFinal())
            {
                //System.out.println("distance to symmetric atom (" + r + ") is
                // too big (>"+getToleranceFinal()+") for " + i);
                if (logger.isDebugEnabled())
                {
                    logger.debug("distance to symmetric atom (" + r +
                        ") is too big for " + i);
                }

                return -1;
            }

            if (r > max_r)
            {
                max_r = r;
            }
        }

        elem.maxdev = max_r;

        return 0;
    }

    private void clear()
    {
        toleranceSame = TOLERANCE_SAME_DEFAULT;
        tolerancePrimary = TOLERANCE_PRIMARY_DEFAULT;
        toleranceFinal = TOLERANCE_FINAL_DEFAULT;
        maxOptStep = MAXOPT_STEP_DEFAULT;
        minOptStep = MINOPT_STEP_DEFAULT;
        gradientStep = GRADIENT_STEP_DEFAULT;
        optChangeThreshold = OPTCHANGE_THRESHOLD_DEFAULT;
        maxOptCycles = MAX_OPT_CYCLES_DEFAULT;
        optChangeHits = OPT_CHANGE_HITS_DEFAULT;
        maxAxisOrder = MAX_AXIS_ORDER_DEFAULT;

        planesCount = 0;
        normalAxesCount = 0;
        inversionCentersCount = 0;
        improperAxesCount = 0;

        atoms = null;
        atomsCount = 0;

        centerOfSomething = new double[SymAtom.DIMENSION];
        distanceFromCenter = null;

        planes = null;
        molecularPlane = null;
        inversionCenters = null;
        normalAxes = null;
        improperAxes = null;
        normalAxesCounts = null;
        improperAxesCounts = null;
        badOptimization = false;
        symmetryCode = null;
        pointGroupRejectionReason = null;

        statistic.clear();
    }

    private void destroySymmetryElement(SymmetryElement elem)
    {
        elem.transform = null;
    }

    private int establishPairs(SymmetryElement elem) throws SymmetryException
    {
        int i;
        int j;
        int k;
        int best_j;
        boolean[] atomUsed = new boolean[atomsCount];
        double distance = 0.0;
        double bestDistance;
        SymAtom symmetric = new SymAtom();

        for (i = 0; i < atomsCount; i++)
        {
            //  No symmetric atom yet
            if (elem.transform[i] >= atomsCount)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("looking for a pair for " + i);
                }

                elem.transformAtom(this, elem, atoms[i], symmetric);

                if (logger.isDebugEnabled())
                {
                    logger.debug("new coordinates are: (" + symmetric.coord[0] +
                        "," + symmetric.coord[1] + "," + symmetric.coord[2] +
                        ")");
                }

                best_j = i;

                // Performance value we'll reject
                bestDistance = 2 * getTolerancePrimary();

                for (j = 0; j < atomsCount; j++)
                {
                    if ((atoms[j].type != symmetric.type) || atomUsed[j])
                    {
                        continue;
                    }

                    for (k = 0, distance = 0; k < SymAtom.DIMENSION; k++)
                    {
                        distance += pow2(symmetric.coord[k] - atoms[j].coord[k]);
                    }

                    distance = Math.sqrt(distance);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("distance to " + j + " is " + distance);
                    }

                    if (distance < bestDistance)
                    {
                        best_j = j;
                        bestDistance = distance;
                    }
                }

                // Too bad, there is no symmetric atom
                if (bestDistance > getTolerancePrimary())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("no pair for atom " + i + " - best was " +
                            best_j + " with err = " + bestDistance);
                    }

                    atomUsed = null;

                    return -1;
                }

                elem.transform[i] = best_j;
                atomUsed[best_j] = true;

                if (logger.isDebugEnabled())
                {
                    logger.debug("atom " + i + " transforms to the atom " +
                        best_j + ", err = " + bestDistance);
                }
            }
        }

        atomUsed = null;

        return 0;
    }

    private double evalOptimizationTargetFunction(SymmetryElement elem,
        int[] finish) throws SymmetryException
    {
        int i;
        int j;
        int k;
        SymAtom symmetric = new SymAtom();
        double target;
        double r = 0.0;
        double maxr;

        if (elem.nparam >= 4)
        {
            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                r += (elem.normal[k] * elem.normal[k]);
            }

            r = Math.sqrt(r);

            if (r < getToleranceSame())
            {
                throw new SymmetryException("Normal collapced!");
            }

            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                elem.normal[k] /= r;
            }

            if (elem.distance < 0)
            {
                elem.distance = -elem.distance;

                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    elem.normal[k] = -elem.normal[k];
                }
            }
        }

        if (elem.nparam >= 7)
        {
            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                r += (elem.direction[k] * elem.direction[k]);
            }

            r = Math.sqrt(r);

            if (r < getToleranceSame())
            {
                throw new SymmetryException("Direction collapced!");
            }

            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                elem.direction[k] /= r;
            }
        }

        for (i = 0, target = maxr = 0; i < atomsCount; i++)
        {
            elem.transformAtom(this, elem, atoms[i], symmetric);
            j = elem.transform[i];

            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                r += pow2(atoms[j].coord[k] - symmetric.coord[k]);
            }

            if (r > maxr)
            {
                maxr = r;
            }

            target += r;
        }

        if (finish != null)
        {
            finish[0] = 0;

            if (Math.sqrt(maxr) < getToleranceFinal())
            {
                finish[0] = 1;
            }
        }

        return target;
    }

    //
    //   Control routines
    //
    private void findCenterOfSomething() throws SymmetryException
    {
        int atomIdx;
        int syAtomIdx;
        double[] coordSum = new double[SymAtom.DIMENSION];
        double center;

        for (syAtomIdx = 0; syAtomIdx < SymAtom.DIMENSION; syAtomIdx++)
        {
            coordSum[syAtomIdx] = 0;
        }

        for (atomIdx = 0; atomIdx < atomsCount; atomIdx++)
        {
            for (syAtomIdx = 0; syAtomIdx < SymAtom.DIMENSION; syAtomIdx++)
            {
                coordSum[syAtomIdx] += atoms[atomIdx].coord[syAtomIdx];
            }
        }

        for (syAtomIdx = 0; syAtomIdx < SymAtom.DIMENSION; syAtomIdx++)
        {
            centerOfSomething[syAtomIdx] = coordSum[syAtomIdx] / atomsCount;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Center of something is at " + centerOfSomething[0] +
                ", " + centerOfSomething[1] + ", " + centerOfSomething[2]);
        }

        distanceFromCenter = new double[atomsCount];

        if (distanceFromCenter == null)
        {
            throw new SymmetryException(
                "Unable to allocate array for the distances");
        }

        for (atomIdx = 0; atomIdx < atomsCount; atomIdx++)
        {
            for (syAtomIdx = 0, center = 0; syAtomIdx < SymAtom.DIMENSION;
                    syAtomIdx++)
            {
                center += pow2(atoms[atomIdx].coord[syAtomIdx] -
                        centerOfSomething[syAtomIdx]);
            }

            distanceFromCenter[atomIdx] = center;
        }
    }

    private void findHigherAxes() throws SymmetryException
    {
        int i;
        int j;
        int k;
        SymmetryElement axis;

        for (i = 0; i < atomsCount; i++)
        {
            for (j = i + 1; j < atomsCount; j++)
            {
                if (atoms[i].type != atoms[j].type)
                {
                    continue;
                }

                // A very cheap, but quite effective check
                if (Math.abs(distanceFromCenter[i] - distanceFromCenter[j]) >
                        getTolerancePrimary())
                {
                    continue;
                }

                for (k = 0; k < atomsCount; k++)
                {
                    if (atoms[i].type != atoms[k].type)
                    {
                        continue;
                    }

                    if ((Math.abs(
                                    distanceFromCenter[i] -
                                    distanceFromCenter[k]) >
                                getTolerancePrimary()) ||
                            (Math.abs(
                                    distanceFromCenter[j] -
                                    distanceFromCenter[k]) >
                                getTolerancePrimary()))
                    {
                        continue;
                    }

                    if ((axis = initHigherAxis(i, j, k)) != null)
                    {
                        normalAxesCount++;

                        if (normalAxes == null)
                        {
                            normalAxes = new SymmetryElement[normalAxesCount];
                        }

                        SymmetryElement[] tmpAxes =
                            new SymmetryElement[normalAxesCount];
                        System.arraycopy(normalAxes, 0, tmpAxes, 0,
                            normalAxesCount - 1);
                        normalAxes = tmpAxes;

                        if (normalAxes == null)
                        {
                            throw new SymmetryException(
                                "Out of memory in findHigherAxes");
                        }

                        normalAxes[normalAxesCount - 1] = axis;
                    }
                }
            }
        }
    }

    private void findImproperAxes() throws SymmetryException
    {
        int i;
        int j;
        int k;
        SymmetryElement axis;

        for (i = 0; i < atomsCount; i++)
        {
            for (j = i + 1; j < atomsCount; j++)
            {
                for (k = 0; k < atomsCount; k++)
                {
                    if ((axis = initImproperAxis(i, j, k)) != null)
                    {
                        improperAxesCount++;

                        if (improperAxes == null)
                        {
                            improperAxes =
                                new SymmetryElement[improperAxesCount];
                        }

                        SymmetryElement[] tmpAxes =
                            new SymmetryElement[improperAxesCount];
                        System.arraycopy(improperAxes, 0, tmpAxes, 0,
                            improperAxesCount - 1);
                        improperAxes = tmpAxes;

                        if (improperAxes == null)
                        {
                            throw new SymmetryException(
                                "Out of memory in findImproperAxes");
                        }

                        improperAxes[improperAxesCount - 1] = axis;
                    }
                }
            }
        }
    }

    private void findInfinityAxis() throws SymmetryException
    {
        SymmetryElement axis;

        if ((axis = initUltimateAxis()) != null)
        {
            normalAxesCount++;

            if (normalAxes == null)
            {
                normalAxes = new SymmetryElement[normalAxesCount];
            }

            SymmetryElement[] tmpAxes = new SymmetryElement[normalAxesCount];
            System.arraycopy(normalAxes, 0, tmpAxes, 0, normalAxesCount - 1);
            normalAxes = tmpAxes;

            if (normalAxes == null)
            {
                throw new SymmetryException(
                    "Out of memory in find_infinity_axes()");
            }

            normalAxes[normalAxesCount - 1] = axis;
        }
    }

    private void findInversionCenters() throws SymmetryException
    {
        SymmetryElement center;

        if ((center = initInversionCenter()) != null)
        {
            inversionCenters = new SymmetryElement[1];
            inversionCenters[0] = center;
            inversionCentersCount = 1;
        }
    }

    private void findPlanes() throws SymmetryException
    {
        int i;
        int j;
        SymmetryElement plane;

        plane = initUltimatePlane();

        if (plane != null)
        {
            molecularPlane = plane;
            planesCount++;

            if (planes == null)
            {
                planes = new SymmetryElement[planesCount];
            }

            SymmetryElement[] tmpPlanes = new SymmetryElement[planesCount];
            System.arraycopy(planes, 0, tmpPlanes, 0, planesCount - 1);
            planes = tmpPlanes;

            if (planes == null)
            {
                throw new SymmetryException("Out of memory in find_planes");
            }

            planes[planesCount - 1] = plane;
        }

        //              else
        //              {
        //                      //System.out.println("planes ["+(planesCount - 1)+"]="+plane);
        //                      throw new SymmetryException("Ultimate plane was not initialized.");
        //              }
        for (i = 1; i < atomsCount; i++)
        {
            for (j = 0; j < i; j++)
            {
                if (atoms[i].type != atoms[j].type)
                {
                    continue;
                }

                if ((plane = initMirrorPlane(i, j)) != null)
                {
                    planesCount++;

                    if (planes == null)
                    {
                        planes = new SymmetryElement[planesCount];
                    }

                    SymmetryElement[] tmpPlanes =
                        new SymmetryElement[planesCount];
                    System.arraycopy(planes, 0, tmpPlanes, 0, planesCount - 1);
                    planes = tmpPlanes;

                    if (planes == null)
                    {
                        throw new SymmetryException(
                            "Out of memory in find_planes");
                    }

                    planes[planesCount - 1] = plane;
                }
            }
        }
    }

    private void getParams(SymmetryElement elem, double[] values)
    {
        //memcpy( values, &elem->distance, elem->nparam * sizeof( double ) ) ;
        double[] tmp = new double[1];
        tmp[0] = elem.distance;
        System.arraycopy(tmp, 0, values, elem.nparam - 1, 1);
    }

    private SymmetryElement initAxisParameters(double[] a, double[] b,
        double[] c)
    {
        SymmetryElement axis = null;
        int i;
        int order;
        int sign;
        double ra;
        double rb;
        double rc;
        double rab;
        double rbc;
        double rac;
        double r;
        double angle;

        ra = rb = rc = rab = rbc = rac = 0;

        for (i = 0; i < SymAtom.DIMENSION; i++)
        {
            ra += (a[i] * a[i]);
            rb += (b[i] * b[i]);
            rc += (c[i] * c[i]);
        }

        ra = Math.sqrt(ra);
        rb = Math.sqrt(rb);
        rc = Math.sqrt(rc);

        if ((Math.abs(ra - rb) > getTolerancePrimary()) ||
                (Math.abs(ra - rc) > getTolerancePrimary()) ||
                (Math.abs(rb - rc) > getTolerancePrimary()))
        {
            statistic.earlyRemovedCandidates++;

            if (logger.isDebugEnabled())
            {
                logger.debug("points are not on a sphere");
            }
        }
        else
        {
            for (i = 0; i < SymAtom.DIMENSION; i++)
            {
                rab += ((a[i] - b[i]) * (a[i] - b[i]));
                rac += ((a[i] - c[i]) * (a[i] - c[i]));
                rbc += ((c[i] - b[i]) * (c[i] - b[i]));
            }

            rab = Math.sqrt(rab);
            rac = Math.sqrt(rac);
            rbc = Math.sqrt(rbc);

            if (Math.abs(rab - rbc) > getTolerancePrimary())
            {
                statistic.earlyRemovedCandidates++;

                if (logger.isDebugEnabled())
                {
                    logger.debug("points can't be rotation-equivalent");
                }
            }
            else
            {
                if ((rab <= getToleranceSame()) ||
                        (rbc <= getToleranceSame()) ||
                        (rac <= getToleranceSame()))
                {
                    statistic.earlyRemovedCandidates++;

                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                            "rotation is underdefined by these points");
                    }
                }
                else
                {
                    rab = (rab + rbc) / 2;
                    angle = M_PI - (2 * Math.asin(rac / (2 * rab)));

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("rotation angle is " + angle);
                    }

                    if (Math.abs(angle) <= (M_PI / (getMaxAxisOrder() + 1)))
                    {
                        statistic.earlyRemovedCandidates++;

                        if (logger.isDebugEnabled())
                        {
                            logger.debug(
                                "atoms are too close to a straight line");
                        }
                    }
                    else
                    {
                        order = (int) Math.floor(((2 * M_PI) / angle) + 0.5);

                        //System.out.println("rotation axis order (" + order +
                        // ")");
                        if ((order <= 2) || (order > getMaxAxisOrder()))
                        {
                            statistic.earlyRemovedCandidates++;

                            if (logger.isDebugEnabled())
                            {
                                logger.debug("rotation axis order (" + order +
                                    ") is not from 3 to " + getMaxAxisOrder());
                            }
                        }
                        else
                        {
                            axis = allocSymmetryElement();
                            axis.order = order;
                            axis.nparam = 7;

                            for (i = 0, r = 0; i < SymAtom.DIMENSION; i++)
                            {
                                r += (centerOfSomething[i] *
                                        centerOfSomething[i]);
                            }

                            r = Math.sqrt(r);

                            if (r > 0)
                            {
                                for (i = 0; i < SymAtom.DIMENSION; i++)
                                {
                                    axis.normal[i] = centerOfSomething[i] / r;
                                }
                            }
                            else
                            {
                                axis.normal[0] = 1;

                                for (i = 1; i < SymAtom.DIMENSION; i++)
                                {
                                    axis.normal[i] = 0;
                                }
                            }

                            axis.distance = r;
                            axis.direction[0] = ((b[1] - a[1]) *
                                    (c[2] - b[2])) -
                                ((b[2] - a[2]) * (c[1] - b[1]));
                            axis.direction[1] = ((b[2] - a[2]) *
                                    (c[0] - b[0])) -
                                ((b[0] - a[0]) * (c[2] - b[2]));
                            axis.direction[2] = ((b[0] - a[0]) *
                                    (c[1] - b[1])) -
                                ((b[1] - a[1]) * (c[0] - b[0]));

                            //
                            //  Arbitrarily select axis direction so that first
                            // non-zero
                            // component
                            //  or the direction is positive.
                            //
                            sign = 0;

                            if (axis.direction[0] <= 0)
                            {
                                if (axis.direction[0] < 0)
                                {
                                    sign = 1;
                                }
                                else if (axis.direction[1] <= 0)
                                {
                                    if (axis.direction[1] < 0)
                                    {
                                        sign = 1;
                                    }
                                    else if (axis.direction[2] < 0)
                                    {
                                        sign = 1;
                                    }
                                }
                            }

                            if (sign != 0)
                            {
                                for (i = 0; i < SymAtom.DIMENSION; i++)
                                {
                                    axis.direction[i] = -axis.direction[i];
                                }
                            }

                            for (i = 0, r = 0; i < SymAtom.DIMENSION; i++)
                            {
                                r += (axis.direction[i] * axis.direction[i]);
                            }

                            r = Math.sqrt(r);

                            for (i = 0; i < SymAtom.DIMENSION; i++)
                            {
                                axis.direction[i] /= r;
                            }

                            if (logger.isDebugEnabled())
                            {
                                logger.debug("axis origin is at (" +
                                    (axis.normal[0] * axis.distance) + "," +
                                    (axis.normal[1] * axis.distance) + "," +
                                    (axis.normal[2] * axis.distance) + ")");
                                logger.debug("    axis is in the direction (" +
                                    axis.direction[0] + "," +
                                    axis.direction[1] + "," +
                                    axis.direction[2] + ")");
                            }
                        }
                    }
                }
            }
        }

        return axis;
    }

    private SymmetryElement initC2Axis(int i, int j, double[] support)
        throws SymmetryException
    {
        SymmetryElement axis;
        int k;
        double ris;
        double rjs;
        double r;
        double[] center = new double[SymAtom.DIMENSION];

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying c2 axis for the pair (" + i + "," + j +
                ") with the support (" + support[0] + "," + support[1] + "," +
                support[2] + ")");
        }

        statistic.totalExaminedCandidates++;

        // First, do a quick sanity check
        for (k = 0, ris = rjs = 0; k < SymAtom.DIMENSION; k++)
        {
            ris += pow2(atoms[i].coord[k] - support[k]);
            rjs += pow2(atoms[j].coord[k] - support[k]);
        }

        ris = Math.sqrt(ris);
        rjs = Math.sqrt(rjs);

        if (Math.abs(ris - rjs) > getTolerancePrimary())
        {
            statistic.earlyRemovedCandidates++;

            if (logger.isDebugEnabled())
            {
                logger.debug("Support can't actually define a rotation axis");
            }

            return null;
        }
        else
        {
            axis = allocSymmetryElement();
            axis.transformAtomMethod = new TransformationAtom("rotateAtom");
            axis.order = 2;
            axis.nparam = 7;

            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                r += (centerOfSomething[k] * centerOfSomething[k]);
            }

            r = Math.sqrt(r);

            if (r > 0)
            {
                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    axis.normal[k] = centerOfSomething[k] / r;
                }
            }
            else
            {
                axis.normal[0] = 1;

                for (k = 1; k < SymAtom.DIMENSION; k++)
                {
                    axis.normal[k] = 0;
                }
            }

            axis.distance = r;

            for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
            {
                center[k] = ((atoms[i].coord[k] + atoms[j].coord[k]) / 2) -
                    support[k];
                r += (center[k] * center[k]);
            }

            r = Math.sqrt(r);

            // c2 is underdefined, let's do something special
            if (r <= getTolerancePrimary())
            {
                if (molecularPlane != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                            "c2 is underdefined, but there is a molecular plane");
                    }

                    for (k = 0; k < SymAtom.DIMENSION; k++)
                    {
                        axis.direction[k] = molecularPlane.normal[k];
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug(
                            "c2 is underdefined, trying random direction");
                    }

                    for (k = 0; k < SymAtom.DIMENSION; k++)
                    {
                        center[k] = atoms[i].coord[k] - atoms[j].coord[k];
                    }

                    if ((Math.abs(center[2]) + Math.abs(center[1])) >
                            getToleranceSame())
                    {
                        axis.direction[0] = 0;
                        axis.direction[1] = center[2];
                        axis.direction[2] = -center[1];
                    }
                    else
                    {
                        axis.direction[0] = -center[2];
                        axis.direction[1] = 0;
                        axis.direction[2] = center[0];
                    }

                    for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
                    {
                        r += (axis.direction[k] * axis.direction[k]);
                    }

                    r = Math.sqrt(r);

                    for (k = 0; k < SymAtom.DIMENSION; k++)
                    {
                        axis.direction[k] /= r;
                    }
                }
            }
            else
            {
                //direction is Ok, renormalize it
                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    axis.direction[k] = center[k] / r;
                }
            }

            if (refineSymmetryElement(axis, 1) < 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.warn("refinement failed for the c2 axis");
                }

                destroySymmetryElement(axis);

                return null;
            }
        }

        return axis;
    }

    private SymmetryElement initHigherAxis(int axis1Idx, int axis2Idx,
        int axis3Idx) throws SymmetryException
    {
        SymmetryElement axis;
        double[] axis1 = new double[SymAtom.DIMENSION];
        double[] axis2 = new double[SymAtom.DIMENSION];
        double[] axis3 = new double[SymAtom.DIMENSION];
        int symAtomIdx;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying cn axis for the triplet (" + axis1Idx + "," +
                axis2Idx + "," + axis3Idx + ")");
        }

        statistic.totalExaminedCandidates++;

        // Do a quick check of geometry validity
        for (symAtomIdx = 0; symAtomIdx < SymAtom.DIMENSION; symAtomIdx++)
        {
            axis1[symAtomIdx] = atoms[axis1Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
            axis2[symAtomIdx] = atoms[axis2Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
            axis3[symAtomIdx] = atoms[axis3Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
        }

        if ((axis = initAxisParameters(axis1, axis2, axis3)) == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("no coherrent axis is defined by the points");
            }

            return null;
        }
        else
        {
            axis.transformAtomMethod = new TransformationAtom("rotateAtom");

            if (refineSymmetryElement(axis, 1) < 0)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("refinement failed for the c" + axis.order +
                        " axis");
                }

                destroySymmetryElement(axis);

                return null;
            }
        }

        return axis;
    }

    private SymmetryElement initImproperAxis(int axis1Idx, int axis2Idx,
        int axis3Idx) throws SymmetryException
    {
        SymmetryElement axis;
        double[] axis1 = new double[SymAtom.DIMENSION];
        double[] axis2 = new double[SymAtom.DIMENSION];
        double[] axis3 = new double[SymAtom.DIMENSION];
        double[] centerpoint = new double[SymAtom.DIMENSION];
        double center;
        int symAtomIdx;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying sn axis for the triplet (" + axis1Idx + "," +
                axis2Idx + "," + axis3Idx + ")");
        }

        statistic.totalExaminedCandidates++;

        // First, reduce the problem to Cn case
        for (symAtomIdx = 0; symAtomIdx < SymAtom.DIMENSION; symAtomIdx++)
        {
            axis1[symAtomIdx] = atoms[axis1Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
            axis2[symAtomIdx] = atoms[axis2Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
            axis3[symAtomIdx] = atoms[axis3Idx].coord[symAtomIdx] -
                centerOfSomething[symAtomIdx];
        }

        for (symAtomIdx = 0, center = 0; symAtomIdx < SymAtom.DIMENSION;
                symAtomIdx++)
        {
            centerpoint[symAtomIdx] = axis1[symAtomIdx] + axis3[symAtomIdx] +
                (2 * axis2[symAtomIdx]);
            center += (centerpoint[symAtomIdx] * centerpoint[symAtomIdx]);
        }

        center = Math.sqrt(center);

        if (center <= getToleranceSame())
        {
            statistic.earlyRemovedCandidates++;

            if (logger.isDebugEnabled())
            {
                logger.debug(
                    "atoms can not define improper axis of the order more than 2");
            }

            return null;
        }
        else
        {
            for (symAtomIdx = 0; symAtomIdx < SymAtom.DIMENSION; symAtomIdx++)
            {
                centerpoint[symAtomIdx] /= center;
            }

            for (symAtomIdx = 0, center = 0; symAtomIdx < SymAtom.DIMENSION;
                    symAtomIdx++)
            {
                center += (centerpoint[symAtomIdx] * axis2[symAtomIdx]);
            }

            for (symAtomIdx = 0; symAtomIdx < SymAtom.DIMENSION; symAtomIdx++)
            {
                axis2[symAtomIdx] = (2 * center * centerpoint[symAtomIdx]) -
                    axis2[symAtomIdx];
            }

            // Do a quick check of geometry validity
            if ((axis = initAxisParameters(axis1, axis2, axis3)) == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(
                        "no coherrent improper axis is defined by the points");
                }

                return null;
            }
            else
            {
                axis.transformAtomMethod = new TransformationAtom(
                        "rotateReflectAtom");

                if (refineSymmetryElement(axis, 1) < 0)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("refinement failed for the s" +
                            axis.order + " axis");
                    }

                    destroySymmetryElement(axis);

                    return null;
                }
            }
        }

        return axis;
    }

    private SymmetryElement initInversionCenter() throws SymmetryException
    {
        SymmetryElement center = allocSymmetryElement();
        int k;
        double r;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying inversion center at the center of something");
        }

        statistic.totalExaminedCandidates++;
        center.transformAtomMethod = new TransformationAtom("invertAtom");
        center.order = 2;
        center.nparam = 4;

        for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
        {
            r += (centerOfSomething[k] * centerOfSomething[k]);
        }

        r = Math.sqrt(r);

        if (r > 0)
        {
            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                center.normal[k] = centerOfSomething[k] / r;
            }
        }
        else
        {
            center.normal[0] = 1;

            for (k = 1; k < SymAtom.DIMENSION; k++)
            {
                center.normal[k] = 0;
            }
        }

        center.distance = r;

        if (logger.isDebugEnabled())
        {
            logger.debug("initial inversion center is at " + r +
                " from the origin");
        }

        if (refineSymmetryElement(center, 1) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("refinement failed for the inversion center");
            }

            destroySymmetryElement(center);

            return null;
        }

        return center;
    }

    private SymmetryElement initMirrorPlane(int i, int j)
        throws SymmetryException
    {
        SymmetryElement plane = allocSymmetryElement();
        double[] dx = new double[SymAtom.DIMENSION];
        double[] midpoint = new double[SymAtom.DIMENSION];
        double rab;
        double r;
        int k;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying mirror plane for atoms " + i + " " + j);
        }

        statistic.totalExaminedCandidates++;
        plane.transformAtomMethod = new TransformationAtom("mirrorAtom");
        plane.order = 2;
        plane.nparam = 4;

        for (k = 0, rab = 0; k < SymAtom.DIMENSION; k++)
        {
            dx[k] = atoms[i].coord[k] - atoms[j].coord[k];
            midpoint[k] = (atoms[i].coord[k] + atoms[j].coord[k]) / 2.0;
            rab += (dx[k] * dx[k]);
        }

        rab = Math.sqrt(rab);

        if (rab < getToleranceSame())
        {
            throw new SymmetryException("Atoms " + i + " and " + j +
                " coincide (r = " + rab + ")");
        }

        for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
        {
            plane.normal[k] = dx[k] / rab;
            r += (midpoint[k] * plane.normal[k]);
        }

        //Reverce normal direction, distance is always positive!
        if (r < 0)
        {
            r = -r;

            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                plane.normal[k] = -plane.normal[k];
            }
        }

        plane.distance = r;

        if (logger.isDebugEnabled())
        {
            logger.debug("initial plane is at " + r + " from the origin");
        }

        if (refineSymmetryElement(plane, 1) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("refinement failed for the plane");
            }

            destroySymmetryElement(plane);

            return null;
        }

        return plane;
    }

    private SymmetryElement initUltimateAxis() throws SymmetryException
    {
        SymmetryElement axis = allocSymmetryElement();
        double[] dir = new double[SymAtom.DIMENSION];
        double[] rel = new double[SymAtom.DIMENSION];
        double s;
        int i;
        int k;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying infinity axis");
        }

        statistic.totalExaminedCandidates++;
        axis.transformAtomMethod = new TransformationAtom("rotateAtom");
        axis.order = 0;
        axis.nparam = 7;

        for (k = 0; k < SymAtom.DIMENSION; k++)
        {
            dir[k] = 0;
        }

        for (i = 0; i < atomsCount; i++)
        {
            for (k = 0, s = 0; k < SymAtom.DIMENSION; k++)
            {
                rel[k] = atoms[i].coord[k] - centerOfSomething[k];
                s += (rel[k] * dir[k]);
            }

            if (s >= 0)
            {
                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    dir[k] += rel[k];
                }
            }
            else
            {
                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    dir[k] -= rel[k];
                }
            }
        }

        for (k = 0, s = 0; k < SymAtom.DIMENSION; k++)
        {
            s += pow2(dir[k]);
        }

        s = Math.sqrt(s);

        if (s > 0)
        {
            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                dir[k] /= s;
            }
        }
        else
        {
            dir[0] = 1;
        }

        for (k = 0; k < SymAtom.DIMENSION; k++)
        {
            axis.direction[k] = dir[k];
        }

        for (k = 0, s = 0; k < SymAtom.DIMENSION; k++)
        {
            s += pow2(centerOfSomething[k]);
        }

        s = Math.sqrt(s);

        if (s > 0)
        {
            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                axis.normal[k] = centerOfSomething[k] / s;
            }
        }
        else
        {
            for (k = 1; k < SymAtom.DIMENSION; k++)
            {
                axis.normal[k] = 0;
            }

            axis.normal[0] = 1;
        }

        axis.distance = s;

        for (k = 0; k < atomsCount; k++)
        {
            axis.transform[k] = k;
        }

        if (refineSymmetryElement(axis, 0) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("refinement failed for the infinity axis");
            }

            destroySymmetryElement(axis);

            return null;
        }

        return axis;
    }

    private SymmetryElement initUltimatePlane() throws SymmetryException
    {
        SymmetryElement plane = allocSymmetryElement();
        double[] d0 = new double[SymAtom.DIMENSION];
        double[] d1 = new double[SymAtom.DIMENSION];
        double[] d2 = new double[SymAtom.DIMENSION];
        double[] p = new double[SymAtom.DIMENSION];
        double r;
        double s0;
        double s1;
        double s2;
        double[] d;
        int i;
        int j;
        int k;

        if (logger.isDebugEnabled())
        {
            logger.debug("Trying whole-molecule mirror plane");
        }

        statistic.totalExaminedCandidates++;
        plane.transformAtomMethod = new TransformationAtom("mirrorAtom");
        plane.order = 1;
        plane.nparam = 4;

        for (k = 0; k < SymAtom.DIMENSION; k++)
        {
            d0[k] = d1[k] = d2[k] = 0;
        }

        d0[0] = 1;
        d1[1] = 1;
        d2[2] = 1;

        for (i = 1; i < atomsCount; i++)
        {
            for (j = 0; j < i; j++)
            {
                for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
                {
                    p[k] = atoms[i].coord[k] - atoms[j].coord[k];
                    r += (p[k] * p[k]);
                }

                r = Math.sqrt(r);

                for (k = 0, s0 = s1 = s2 = 0; k < SymAtom.DIMENSION; k++)
                {
                    p[k] /= r;
                    s0 += (p[k] * d0[k]);
                    s1 += (p[k] * d1[k]);
                    s2 += (p[k] * d2[k]);
                }

                for (k = 0; k < SymAtom.DIMENSION; k++)
                {
                    d0[k] -= (s0 * p[k]);
                    d1[k] -= (s1 * p[k]);
                    d2[k] -= (s2 * p[k]);
                }
            }
        }

        for (k = 0, s0 = s1 = s2 = 0; k < SymAtom.DIMENSION; k++)
        {
            s0 += d0[k];
            s1 += d1[k];
            s2 += d2[k];
        }

        d = null;

        if ((s0 >= s1) && (s0 >= s2))
        {
            d = d0;
        }

        if ((s1 >= s0) && (s1 >= s2))
        {
            d = d1;
        }

        if ((s2 >= s0) && (s2 >= s1))
        {
            d = d2;
        }

        if (d == null)
        {
            throw new SymmetryException(
                "Catastrophe in init_ultimate_plane(): " + s0 + " " + s1 +
                " and " + s2 + " have no ordering!");
        }

        r = 0;

        for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
        {
            r += (d[k] * d[k]);
        }

        r = Math.sqrt(r);

        if (r > 0)
        {
            for (k = 0; k < SymAtom.DIMENSION; k++)
            {
                plane.normal[k] = d[k] / r;
            }
        }
        else
        {
            for (k = 1; k < SymAtom.DIMENSION; k++)
            {
                plane.normal[k] = 0;
            }

            plane.normal[0] = 1;
        }

        for (k = 0, r = 0; k < SymAtom.DIMENSION; k++)
        {
            r += (centerOfSomething[k] * plane.normal[k]);
        }

        plane.distance = r;

        for (k = 0; k < atomsCount; k++)
        {
            plane.transform[k] = k;
        }

        if (refineSymmetryElement(plane, 0) < 0)
        {
            //throw new SymmetryException("refinement failed for the plane");
            destroySymmetryElement(plane);

            return null;
        }

        return plane;
    }

    private void optimizeTransformationParams(SymmetryElement elem)
        throws SymmetryException
    {
        double[] values = new double[MAXPARAM];
        double[] grad = new double[MAXPARAM];
        double[] force = new double[MAXPARAM];
        double[] step = new double[MAXPARAM];
        double f = 0.0;
        double fold;
        double fnew;
        double fnew2;
        double fdn;
        double fup;
        double snorm;
        double a;
        double b;
        double x;
        int vars = elem.nparam;
        int cycle = 0;
        int i;
        int[] finish = new int[1];
        int hits = 0;

        if (vars > MAXPARAM)
        {
            throw new SymmetryException(
                "Catastrophe in optimize_transformation_params()!");
        }

        f = 0;

        do
        {
            fold = f;
            f = evalOptimizationTargetFunction(elem, finish);

            // Evaluate methodName, gradient and diagonal force constants
            if (logger.isDebugEnabled())
            {
                logger.debug("methodName value = " + f);
            }

            if (finish[0] == 1)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("methodName value is small enough");
                }

                break;
            }

            if (cycle > 0)
            {
                if (Math.abs(f - fold) > getOptChangeThreshold())
                {
                    hits = 0;
                }
                else
                {
                    hits++;
                }

                if (hits >= getOptChangeHits())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("no progress is made, stop optimization");
                    }

                    break;
                }
            }

            getParams(elem, values);

            for (i = 0; i < vars; i++)
            {
                values[i] -= getGradientStep();
                setParams(elem, values);
                fdn = evalOptimizationTargetFunction(elem, null);
                values[i] += (2 * getGradientStep());
                setParams(elem, values);
                fup = evalOptimizationTargetFunction(elem, null);
                values[i] -= getGradientStep();
                grad[i] = (fup - fdn) / (2 * getGradientStep());
                force[i] = ((fup + fdn) - (2 * f)) /
                    (getGradientStep() * getGradientStep());

                if (logger.isDebugEnabled())
                {
                    logger.debug("i = " + i + ", grad = " + grad[i] +
                        ", force = " + force[i]);
                }
            }

            // Do a quasy-Newton step
            for (i = 0, snorm = 0; i < vars; i++)
            {
                if (force[i] < 0)
                {
                    force[i] = -force[i];
                }

                if (force[i] < 1e-3)
                {
                    force[i] = 1e-3;
                }

                if (force[i] > 1e3)
                {
                    force[i] = 1e3;
                }

                step[i] = -grad[i] / force[i];
                snorm += (step[i] * step[i]);
            }

            snorm = Math.sqrt(snorm);

            // Renormalize step
            if (snorm > getMaxOptStep())
            {
                for (i = 0; i < vars; i++)
                {
                    step[i] *= (getMaxOptStep() / snorm);
                }

                snorm = getMaxOptStep();
            }

            do
            {
                for (i = 0; i < vars; i++)
                {
                    values[i] += step[i];
                }

                setParams(elem, values);
                fnew = evalOptimizationTargetFunction(elem, null);

                if (fnew < f)
                {
                    break;
                }

                for (i = 0; i < vars; i++)
                {
                    values[i] -= step[i];
                    step[i] /= 2;
                }

                setParams(elem, values);
                snorm /= 2;
            }
            while (snorm > getMinOptStep());

            // try to do quadratic interpolation
            if ((snorm > getMinOptStep()) && (snorm < (getMaxOptStep() / 2)))
            {
                for (i = 0; i < vars; i++)
                {
                    values[i] += step[i];
                }

                setParams(elem, values);
                fnew2 = evalOptimizationTargetFunction(elem, null);

                if (logger.isDebugEnabled())
                {
                    logger.debug("interpolation base points: " + f + " " +
                        fnew + " " + fnew2);
                }

                for (i = 0; i < vars; i++)
                {
                    values[i] -= (2 * step[i]);
                }

                a = ((4 * f) - fnew2 - (3 * fnew)) / 2;
                b = ((f + fnew2) - (2 * fnew)) / 2;

                if (logger.isDebugEnabled())
                {
                    logger.debug("linear interpolation coefficients " + a +
                        " " + b);
                }

                if (b > 0)
                {
                    x = -a / (2 * b);

                    if ((x > 0.2) && (x < 1.8))
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("interpolated: " + x);
                        }

                        for (i = 0; i < vars; i++)
                        {
                            values[i] += (x * step[i]);
                        }
                    }
                    else
                    {
                        b = 0;
                    }
                }

                if (b <= 0)
                {
                    if (fnew2 < fnew)
                    {
                        for (i = 0; i < vars; i++)
                        {
                            values[i] += (2 * step[i]);
                        }
                    }
                    else
                    {
                        for (i = 0; i < vars; i++)
                        {
                            values[i] += step[i];
                        }
                    }
                }

                setParams(elem, values);
            }
        }
        while ((snorm > getMinOptStep()) && (++cycle < getMaxOptCycles()));

        f = evalOptimizationTargetFunction(elem, null);

        if (cycle >= getMaxOptCycles())
        {
            setBadOptimization(true);
        }

        if (logger.isDebugEnabled())
        {
            if (cycle >= getMaxOptCycles())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("maximum number of optimization cycles made");
                }
            }

            logger.debug("optimization completed after " + cycle +
                " cycles with f = " + f);
        }
    }

    private final double pow2(double x)
    {
        return x * x;
    }

    private int refineSymmetryElement(SymmetryElement elem, int build_table)
        throws SymmetryException
    {
        if (elem == null)
        {
            throw new SymmetryException(
                "Symmetry element to refine is not defined.");
        }

        if ((build_table != 0) && (establishPairs(elem) < 0))
        {
            statistic.removedInitialMating++;

            if (logger.isDebugEnabled())
            {
                logger.debug(
                    "no transformation correspondence table can be constructed");
            }

            return -1;
        }
        else
        {
            boolean proceed = true;

            for (int i = 0; i < planesCount; i++)
            {
                if (planes[i].sameTransform(atomsCount, elem))
                {
                    statistic.removedDuplicates++;

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("transformation is identical to plane " +
                            i);
                    }

                    proceed = false;

                    break;
                }
            }

            if (proceed)
            {
                proceed = true;

                for (int i = 0; i < inversionCentersCount; i++)
                {
                    if (inversionCenters[i].sameTransform(atomsCount, elem))
                    {
                        statistic.removedDuplicates++;

                        if (logger.isDebugEnabled())
                        {
                            logger.debug(
                                "transformation is identical to plane " + i);
                        }

                        proceed = false;

                        break;
                    }
                }

                if (proceed)
                {
                    proceed = true;

                    for (int i = 0; i < normalAxesCount; i++)
                    {
                        if (normalAxes[i].sameTransform(atomsCount, elem))
                        {
                            statistic.removedDuplicates++;

                            if (logger.isDebugEnabled())
                            {
                                logger.debug(
                                    "transformation is identical to plane " +
                                    i);
                            }

                            proceed = false;

                            break;
                        }
                    }

                    if (proceed)
                    {
                        proceed = true;

                        for (int i = 0; i < improperAxesCount; i++)
                        {
                            if (improperAxes[i].sameTransform(atomsCount, elem))
                            {
                                statistic.removedDuplicates++;

                                if (logger.isDebugEnabled())
                                {
                                    logger.debug(
                                        "transformation is identical to plane " +
                                        i);
                                }

                                proceed = false;

                                break;
                            }
                        }

                        if (proceed)
                        {
                            proceed = true;

                            if (checkTransformOrder(elem) < 0)
                            {
                                statistic.removedWrongTransOrder++;
                                proceed = false;
                            }

                            if (proceed)
                            {
                                optimizeTransformationParams(elem);

                                if (checkTransformQuality(elem) < 0)
                                {
                                    statistic.removedUnsuccOpt++;

                                    if (logger.isDebugEnabled())
                                    {
                                        logger.debug(
                                            "refined transformation does not pass the numeric threshold");
                                    }

                                    return -1;
                                }

                                statistic.accepted++;
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    /*
     * General symmetry handling
     */
    private void reportAndResetCounters()
    {
        System.out.println("  " + statistic.totalExaminedCandidates +
            " candidates examined\n" + "  " + statistic.earlyRemovedCandidates +
            " removed early\n" + "  " + statistic.removedInitialMating +
            " removed during initial mating stage\n" + "  " +
            statistic.removedDuplicates + " removed as duplicates\n" + "  " +
            statistic.removedWrongTransOrder +
            " removed because of the wrong transformation order\n" + "  " +
            statistic.removedUnsuccOpt +
            " removed after unsuccessful optimization\n" + "  " +
            statistic.accepted + " accepted");
        statistic.clear();
    }

    private void reportAxes()
    {
        if (normalAxesCount == 0)
        {
            System.out.println("There are no normal axes in the molecule");
        }
        else
        {
            if (normalAxesCount == 1)
            {
                System.out.println("There is a normal axis in the molecule");
            }
            else
            {
                System.out.println("There are " + normalAxesCount +
                    " normal axes in the molecule");
            }

            System.out.println(
                "     Residual  Order         Direction of the axis                         Supporting point\n");

            for (int normalAxe = 0; normalAxe < normalAxesCount; normalAxe++)
            {
                if (normalAxes[normalAxe] != null)
                {
                    System.out.print("" + normalAxe + " " +
                        normalAxes[normalAxe].maxdev + " ");

                    if (normalAxes[normalAxe].order == 0)
                    {
                        System.out.print("Inf ");
                    }
                    else
                    {
                        System.out.print(" " + normalAxes[normalAxe].order);
                    }

                    System.out.print(" (" + normalAxes[normalAxe].direction[0] +
                        "," + normalAxes[normalAxe].direction[1] + "," +
                        normalAxes[normalAxe].direction[2] + ") ");
                    System.out.println(" (" +
                        (normalAxes[0].distance * normalAxes[0].normal[0]) +
                        "," +
                        (normalAxes[0].distance * normalAxes[0].normal[1]) +
                        "," +
                        (normalAxes[0].distance * normalAxes[0].normal[2]) +
                        ")");
                }
            }
        }
    }

    private void reportImproperAxes()
    {
        int i;

        if (improperAxesCount == 0)
        {
            System.out.println("There are no improper axes in the molecule\n");
        }
        else
        {
            if (improperAxesCount == 1)
            {
                System.out.println("There is an improper axis in the molecule");
            }
            else
            {
                System.out.println("There are " + improperAxesCount +
                    " improper axes in the molecule");
            }

            System.out.println(
                "     Residual  Order         Direction of the axis                         Supporting point\n");

            for (i = 0; i < improperAxesCount; i++)
            {
                if (improperAxes[i] != null)
                {
                    System.out.print("" + i + " " + improperAxes[i].maxdev);

                    if (improperAxes[i].order == 0)
                    {
                        System.out.print(" Inf ");
                    }
                    else
                    {
                        System.out.print(" " + improperAxes[i].order);
                    }

                    System.out.print(" (" + improperAxes[i].direction[0] + "," +
                        improperAxes[i].direction[1] + "," +
                        improperAxes[i].direction[2] + ") ");
                    System.out.println(" (" +
                        (improperAxes[0].distance * improperAxes[0].normal[0]) +
                        "," +
                        (improperAxes[0].distance * improperAxes[0].normal[1]) +
                        "" +
                        (improperAxes[0].distance * improperAxes[0].normal[2]) +
                        ")");
                }
            }
        }
    }

    private void reportInversionCenter()
    {
        if (inversionCentersCount == 0)
        {
            System.out.println("There is no inversion center in the molecule");
        }
        else
        {
            System.out.println("There in an inversion center in the molecule");
            System.out.println("     Residual                      Position");
            System.out.print("   " + inversionCenters[0].maxdev + " ");
            System.out.println("(" +
                (inversionCenters[0].distance * inversionCenters[0].normal[0]) +
                "," +
                (inversionCenters[0].distance * inversionCenters[0].normal[1]) +
                "," +
                (inversionCenters[0].distance * inversionCenters[0].normal[2]) +
                ")");
        }
    }

    private void reportPlanes()
    {
        if (planesCount == 0)
        {
            System.out.println(
                "There are no planes of symmetry in the molecule");
        }
        else
        {
            if (planesCount == 1)
            {
                System.out.println(
                    "There is a plane of symmetry in the molecule");
            }
            else
            {
                System.out.println("There are " + planesCount +
                    " planes of symmetry in the molecule");
            }

            System.out.println(
                "     Residual          Direction of the normal           Distance");

            for (int plane = 0; plane < planesCount; plane++)
            {
                if (planes[plane] != null)
                {
                    System.out.print("" + plane + " " + planes[plane].maxdev +
                        " ");
                    System.out.print("(" + planes[plane].normal[0] + "," +
                        planes[plane].normal[1] + "," +
                        planes[plane].normal[2] + ") ");
                    System.out.println("" + planes[plane].distance + "");
                }
            }
        }
    }

    private void setParams(SymmetryElement elem, double[] values)
    {
        //System.out.println("set params::"+values.length+" "+(elem.nparam-1));
        double[] tmp = new double[1];
        System.arraycopy(values, elem.nparam - 1, tmp, 0, 1);
        elem.distance = tmp[0];
    }
}

///////////////////////////////////////////////////////////////////////////////
//  END OF FILE.
///////////////////////////////////////////////////////////////////////////////

