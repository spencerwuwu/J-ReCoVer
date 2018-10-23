// https://searchcode.com/api/result/126504935/

/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

/**
 * Simplex is a data class that builds up a simplex for use by the GJK and EPA algorithms. Much of this class
 * was ported and restructured from the Simplex used in Bullet's GJK algorithms.
 *
 * @author Michael Ludwig
 */
public class Simplex {
    public static final int MAX_RANK = 4;

    private final MinkowskiShape shape;

    private final Vector3[] inputs;
    private final Vector3[] vertices;
    private final double[] weights;

    private int rank;

    private boolean isIntersection;

    public Simplex(MinkowskiShape shape) {
        inputs = new Vector3[MAX_RANK];
        vertices = new Vector3[MAX_RANK];
        weights = new double[MAX_RANK];

        for (int i = 0; i < MAX_RANK; i++) {
            inputs[i] = new Vector3();
            vertices[i] = new Vector3();
            weights[i] = 0.0;
        }

        rank = 0;
        this.shape = shape;

        isIntersection = false;
    }

    public MinkowskiShape getShape() {
        return shape;
    }

    public boolean isIntersection() {
        return isIntersection;
    }

    public void setIntersection(boolean intersect) {
        isIntersection = intersect;
    }

    public void reset() {
        rank = 0;
    }

    public int getRank() {
        return rank;
    }

    public Vector3 getVertex(int i) {
        return vertices[i];
    }

    public Vector3 getInput(int i) {
        return inputs[i];
    }

    public double getWeight(int i) {
        return weights[i];
    }

    public void setWeight(int i, double weight) {
        weights[i] = weight;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Vector3 addVertex(@Const Vector3 dir) {
        weights[rank] = 0.0;
        inputs[rank].normalize(dir);
        shape.getSupport(inputs[rank], vertices[rank]);
        return vertices[rank++];
    }

    public Vector3 addNegatedVertex(@Const Vector3 dir) {
        weights[rank] = 0.0;
        inputs[rank].scale(dir, -1.0).normalize();
        shape.getSupport(inputs[rank], vertices[rank]);
        return vertices[rank++];
    }

    public void discardLastVertex() {
        rank--;
    }

    public boolean encloseOrigin() {
        if (encloseOriginImpl()) {
            // orient the simplex
            if (Util.tripleProduct(new Vector3().sub(vertices[0], vertices[3]),
                                   new Vector3().sub(vertices[1], vertices[3]),
                                   new Vector3().sub(vertices[2], vertices[3])) < 0.0) {
                Vector3 temp = new Vector3();

                temp.set(vertices[0]);
                vertices[0].set(vertices[1]);
                vertices[1].set(temp);

                temp.set(inputs[0]);
                inputs[0].set(inputs[1]);
                inputs[1].set(temp);

                double weight = weights[0];
                weights[0] = weights[1];
                weights[1] = weight;
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean encloseOriginImpl() {
        switch (rank) {
        case 1: {
            Vector3 axis = new Vector3();
            for (int i = 0; i < 3; i++) {
                axis.set(0, 0, 0).set(i, 1.0);
                addVertex(axis);
                if (encloseOriginImpl()) {
                    return true;
                }
                discardLastVertex();
                addNegatedVertex(axis);
                if (encloseOriginImpl()) {
                    return true;
                }
                discardLastVertex();
            }
            break;
        }
        case 2: {
            Vector3 d = new Vector3().sub(vertices[1], vertices[0]);
            Vector3 axis = new Vector3();
            for (int i = 0; i < 3; i++) {
                axis.set(0, 0, 0).set(i, 1.0);
                axis.cross(d, axis);
                if (axis.lengthSquared() > 0) {
                    addVertex(axis);
                    if (encloseOriginImpl()) {
                        return true;
                    }
                    discardLastVertex();
                    addNegatedVertex(axis);
                    if (encloseOriginImpl()) {
                        return true;
                    }
                    discardLastVertex();
                }
            }
            break;
        }
        case 3: {
            Vector3 n = Util.normal(vertices[0], vertices[1], vertices[2], null);
            if (n.lengthSquared() > 0) {
                addVertex(n);
                if (encloseOriginImpl()) {
                    return true;
                }
                discardLastVertex();
                addNegatedVertex(n);
                if (encloseOriginImpl()) {
                    return true;
                }
                discardLastVertex();
            }
            break;
        }
        case 4: {
            if (Math.abs(Util.tripleProduct(new Vector3().sub(vertices[0], vertices[3]),
                                            new Vector3().sub(vertices[1], vertices[3]),
                                            new Vector3().sub(vertices[2], vertices[3]))) > 0.0) {
                return true;
            }
            break;
        }
        }

        // failed
        return false;
    }

    public boolean reduce() {
        int mask = projectOrigin();
        if (mask > 0) {
            // compact simplex arrays
            for (int i = 0; i < rank; i++) {
                if ((mask & (1 << i)) != 0) {
                    // find lowest empty vertex
                    for (int j = 0; j < i; j++) {
                        if ((mask & (1 << j)) == 0) {
                            // found it, now shift i to j
                            inputs[j].set(inputs[i]);
                            vertices[j].set(vertices[i]);
                            weights[j] = weights[i];

                            // mark j as used and i as unused
                            mask |= (1 << j);
                            mask &= ~(1 << i);
                            break;
                        }
                    }
                }
            }

            // compute new rank
            rank = 0;
            for (int i = MAX_RANK; i >= 0; i--) {
                if ((mask & (1 << i)) != 0) {
                    // all bits lower than this one will also be set
                    rank = i + 1;
                    break;
                }
            }

            // reduced successfully
            return true;
        } else {
            return false;
        }
    }

    private int projectOrigin() {
        Projection proj = null;
        switch (rank) {
        case 2:
            proj = projectOrigin2(vertices[0], vertices[1]);
            break;
        case 3:
            proj = projectOrigin3(vertices[0], vertices[1], vertices[2]);
            break;
        case 4:
            proj = projectOrigin4(vertices[0], vertices[1], vertices[2], vertices[3]);
            break;
        }

        if (proj != null) {
            for (int i = 0; i < rank; i++) {
                weights[i] = proj.weights[i];
            }
            return proj.mask;
        } else {
            return -1;
        }
    }

    private Projection projectOrigin2(@Const Vector3 a, @Const Vector3 b) {
        Vector3 d = new Vector3().sub(b, a);
        double l = d.lengthSquared();

        if (l > 0.0) {
            double t = -a.dot(d) / l;
            if (t >= 1.0) {
                return new Projection(b.lengthSquared(), new double[] { 0.0, 1.0 }, 2);
            } else if (t <= 0.0) {
                return new Projection(a.lengthSquared(), new double[] { 1.0, 0.0 }, 1);
            } else {
                return new Projection(d.scale(t).add(a).lengthSquared(), new double[] { 1 - t, t }, 3);
            }
        } else {
            return null;
        }
    }

    private Projection projectOrigin3(@Const Vector3 a, @Const Vector3 b, @Const Vector3 c) {
        Vector3[] vs = new Vector3[] { a, b, c };
        Vector3[] ds = new Vector3[3];

        ds[0] = new Vector3().sub(a, b);
        ds[1] = new Vector3().sub(b, c);
        ds[2] = new Vector3().sub(c, a);

        Vector3 n = new Vector3().cross(ds[0], ds[1]);
        double l = n.lengthSquared();

        if (l > 0.0) {
            double minDist = -1.0;
            double[] weights = new double[3];
            int mask = 0;

            Vector3 p = new Vector3();
            for (int i = 0; i < 3; i++) {
                if (vs[i].dot(p.cross(ds[i], n)) > 0.0) {
                    int j = (i + 1) % 3;
                    Projection subProj = projectOrigin2(vs[i], vs[j]);

                    if (subProj != null && (minDist < 0.0 || subProj.distance < minDist)) {
                        minDist = subProj.distance;
                        mask = ((subProj.mask & 1) != 0 ? (1 << i) : 0) |
                               ((subProj.mask & 2) != 0 ? (1 << j) : 0);
                        weights[i] = subProj.weights[0];
                        weights[j] = subProj.weights[1];
                        weights[(j + 1) % 3] = 0.0;
                    }
                }
            }

            if (minDist < 0.0) {
                double d = a.dot(n);
                double s = Math.sqrt(l);

                n.scale(d / l);
                minDist = n.lengthSquared();
                mask = 7;
                weights[0] = new Vector3().cross(ds[1], new Vector3().sub(b, n)).length() / s;
                weights[1] = new Vector3().cross(ds[2], new Vector3().sub(c, n)).length() / s;
                weights[2] = 1 - weights[0] - weights[1];
            }

            return new Projection(minDist, weights, mask);
        } else {
            return null;
        }
    }

    private Projection projectOrigin4(@Const Vector3 a, @Const Vector3 b, @Const Vector3 c,
                                      @Const Vector3 d) {
        Vector3[] vs = new Vector3[] { a, b, c, d };
        Vector3[] ds = new Vector3[3];
        ds[0] = new Vector3().sub(a, d);
        ds[1] = new Vector3().sub(b, d);
        ds[2] = new Vector3().sub(c, d);

        double vl = Util.tripleProduct(ds[0], ds[1], ds[2]);
        boolean ng = (vl * a.dot(new Vector3().sub(b, c).cross(new Vector3().sub(a, b)))) <= 0.0;

        if (ng && Math.abs(vl) > 0.0) {
            double minDist = -1.0;
            double[] weights = new double[4];
            int mask = 0;

            for (int i = 0; i < 3; i++) {
                int j = (i + 1) % 3;
                double s = vl * d.dot(new Vector3().cross(ds[i], ds[j]));
                if (s > 0.0) {
                    Projection subProj = projectOrigin3(vs[i], vs[j], d);
                    if (subProj != null && (minDist < 0.0 || subProj.distance < minDist)) {
                        minDist = subProj.distance;
                        mask = ((subProj.mask & 1) != 0 ? (1 << i) : 0) |
                               ((subProj.mask & 2) != 0 ? (1 << j) : 0) |
                               ((subProj.mask & 4) != 0 ? 8 : 0);
                        weights[i] = subProj.weights[0];
                        weights[j] = subProj.weights[1];
                        weights[(j + 1) % 3] = 0.0;
                        weights[3] = subProj.weights[2];
                    }
                }
            }

            if (minDist < 0.0) {
                minDist = 0.0;
                mask = 15;
                weights[0] = Util.tripleProduct(c, b, d) / vl;
                weights[1] = Util.tripleProduct(a, c, d) / vl;
                weights[2] = Util.tripleProduct(b, a, d) / vl;
                weights[3] = 1 - weights[0] - weights[1] - weights[2];
            }

            return new Projection(minDist, weights, mask);
        } else {
            return null;
        }
    }

    // FIXME work out a way to reduce the allocation of Projection objects
    private static class Projection {
        final double[] weights;
        final double distance;
        final int mask;

        public Projection(double distance, double[] weights, int mask) {
            this.weights = weights;
            this.distance = distance;
            this.mask = mask;
        }
    }
}

