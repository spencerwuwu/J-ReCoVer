// https://searchcode.com/api/result/103246120/

package gigadot.chom.thermo.partitionfunc;

import gigadot.chom.chem.info.alias.ThermoAnalyzable;
import gigatools.lite.constant.PhysicalConstants;

/**
 * unit in SI
 * @author wp214
 */
public class TranslationalPartitionFunction extends PartitionFunction {
    private double T = 0.0;
    private double mass = 0.0;
    private double P = PhysicalConstants.P_1atm;

    @Override
    public void setThermoAnalyzable(ThermoAnalyzable mDoc) {
        super.setThermoAnalyzable(mDoc);
        this.mass = mDoc.getWeigth() * PhysicalConstants.amu;
    }

    public void setPressure(double P) {
        this.P = P;
    }

    @Override
    public PartitionValues getPartitionValues(double T) {
        this.T = T;
        PartitionValues Q = new PartitionValues();
        double k_B = PhysicalConstants.k_B;
        double h   = PhysicalConstants.h;
        double V = k_B * T / P;
        // CALCULATE q
        // Divide in this order to reduce numerical error from divisions.
        // q_trans = (2 pi m k_B T / h^2)^(3/2)
        double q_in = 2.0 * Math.PI * (mass/h) * (k_B/h) ;
        Q.q = Math.pow(q_in * T, 1.5) * V;

        // CALCULATE dqBydT
        // dq_trans/dT = (3/2) (2 pi m k_B / h^2)^(3/2) (T^1/2)
        Q.dqBydT = 1.5 * Q.q / T;

        // CALCULATE d2qBydT2
        // d^2 q_trans/d^2 T = (3/4) (2 pi m k_B / h^2)^(3/2) (T^-1/2)
        Q.d2qBydT2 = 0.5 * Q.dqBydT / T;

        return Q;
    }

}

