// https://searchcode.com/api/result/119735457/


/*
 * Copyright 2008 2009 2010 Douglas Wikstrom
 *
 * This file is part of Verificatum.
 *
 * Verificatum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Verificatum is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Verificatum.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package verificatum.protocol.secretsharing;

import java.io.*;
import java.util.*;

import verificatum.arithm.*;
import verificatum.crypto.*;
import verificatum.eio.*;
import verificatum.ui.*;
import verificatum.protocol.*;

/**
 * Implements a generalized version of Pedersen's Verifiable Secret
 * Sharing (VSS) scheme for arbitrary homomorphisms. This allows
 * receivers to verify their shares and complain if a share is
 * incorrect. The dealer then has the chance of publishing the shares
 * of all complaining parties. If these shares are correct, the
 * sharing is accepted and otherwise it is rejected.
 *
 * <p>
 *
 * This class allows using any {@link HomPRingPGroup} as the
 * underlying homomorphism. Feldman VSS boils down to the
 * exponentiation homomorphism and standard Pedersen VSS boils down to
 * the homomorphism that outputs the exponentiated product of two
 * exponents. The protocol allows reconstruction as long as the
 * underlying homomorphism is collision-resistant. Note that this is
 * the case for the mentioned examples. The hiding property of the
 * protocol depends on how it is used; for Feldman it does not hide
 * the secret perfectly, for standard Pedersen it does, etc.
 *
 * @author Douglas Wikstrom
 */
public class Pedersen extends Protocol implements PGroupAssociated {

    /**
     * Underlying functionality.
     */
    protected PedersenBasic pedersenBasic;

    /**
     * Is set to <code>true</code> when the secret shared in this
     * instance can be recovered.
     */
    protected boolean recoverable;

    /**
     * Is set to <code>true</code> when this instance is a "trivial"
     * instance.
     */
    protected boolean trivial;

    /**
     * Public keys used for communication.
     */
    protected CryptoPKey[] pkeys;

    /**
     * Secret key of this instance.
     */
    protected CryptoSKey skey;

    /**
     * Index of the dealer in this instance.
     */
    protected int l;

    /**
     * Decides if this instance should store and recover itself
     * automatically to/from file.
     */
    protected boolean storeToFile;

    /**
     * States in which an instance can be.
     */
    protected enum State {
        /**
         * Initial state of an instantiation.
         */
        INITIAL,

        /**
         * State of the dealer after completed distribution of secret.
         */
        SECRET_DISTRIBUTED,

        /**
         * State of receiver (the dealer may receive), before
         * correctly receiving a share.
         */
        ATTEMPTING_RECEIVE,

        /**
         * State of receiver after receiving a correct share.
         */
        SHARE_RECEIVED,

        /**
         * State after the secret has been recovered.
         */
        SECRET_RECOVERED
    };

    /**
     * Current state of this instance.
     */
    protected State state;

    /**
     * Decides the statistical distance from the uniform distribution.
     */
    protected int statDist;

    /**
     * Creates an instance of the protocol.
     *
     * @param sid Session identifier of this instance.
     * @param protocol Protocol which invokes this one.
     * @param l Index of the dealer.
     * @param hom Underlying homomorphism.
     * @param pkeys Plain public keys of all parties.
     * @param skey Plain secret key.
     * @param statDist Decides the statistical distance from the
     * uniform distribution.
     */
    public Pedersen(String sid,
                    Protocol protocol,
                    int l,
                    HomPRingPGroup hom,
                    CryptoPKey[] pkeys,
                    CryptoSKey skey,
                    int statDist) {
        super(sid, protocol);
        this.state = State.INITIAL;
        pedersenBasic =
            new PedersenBasic(k, j, l, hom, randomSource, statDist);
        this.l = l;
        this.pkeys = pkeys;
        this.skey = skey;
        this.statDist = statDist;
        trivial = false;
        storeToFile = false;
    }

    /**
     * Creates an instance of the protocol.
     *
     * @param sid Session identifier of this instance.
     * @param protocol Protocol which invokes this one.
     * @param l Index of the dealer.
     * @param hom Underlying homomorphism.
     * @param pkeys Plain public keys of all parties.
     * @param skey Plain secret key.
     * @param statDist Decides the statistical distance from the
     * uniform distribution.
     * @param storeToFile Determines if this instance stores/reads
     * itself from/to file.
     */
    public Pedersen(String sid,
                    Protocol protocol,
                    int l,
                    HomPRingPGroup hom,
                    CryptoPKey[] pkeys,
                    CryptoSKey skey,
                    int statDist,
                    boolean storeToFile) {
        this(sid, protocol, l, hom, pkeys, skey, statDist);
        this.storeToFile = storeToFile;
    }

    /**
     * Creates an instance of the protocol in a state that allows
     * recovery of the secret.
     *
     * @param sid Session identifier of this instance.
     * @param protocol Protocol which invokes this one.
     * @param l Index of the dealer.
     * @param pkeys Plain public keys of all parties.
     * @param skey Plain secret key.
     * @param pedersenBasic An instance of pedersenBasic.
     * @param statDist Decides the statistical distance from the
     * uniform distribution.
     * @param storeToFile Determines if this instance stores/reads
     * itself from/to file.
     * @param log Logging context.
     */
    public Pedersen(String sid,
                    Protocol protocol,
                    int l,
                    CryptoPKey[] pkeys,
                    CryptoSKey skey,
                    PedersenBasic pedersenBasic,
                    int statDist,
                    boolean storeToFile,
                    Log log) {
        super(sid, protocol);
        if (j == l) {
            state = State.SECRET_DISTRIBUTED;
        } else {
            state = State.SHARE_RECEIVED;
        }
        this.pedersenBasic = pedersenBasic;
        this.l = l;
        this.pkeys = pkeys;
        this.skey = skey;
        trivial = false;
        this.storeToFile = storeToFile;
        this.statDist = statDist;

        if (storeToFile && !stateOnFile()) {
            stateToFile(log);
        }
    }

    /**
     * Returns the instances corresponding to this one over the
     * factors of the underlying {@link Pedersen} instances.
     *
     * @param log Logging context.
     * @return Instances corresponding to this one over the factors of
     * the underlying {@link Pedersen} instances.
     */
    public Pedersen[] getFactors(Log log) {
        if (state != State.SHARE_RECEIVED
            && state != State.SECRET_DISTRIBUTED) {
            String s = "Factoring is only possible when recovering is!";
            throw new ProtocolError(s);
        }

        PedersenBasic[] pedersenBasics = pedersenBasic.getFactors();

        Pedersen[] pedersens = new Pedersen[pedersenBasics.length];
        for (int i = 0; i < pedersens.length; i++) {
            pedersens[i] =
                new Pedersen("" + i, this, l, pkeys, skey,
                             pedersenBasics[i], statDist, storeToFile, log);
        }
        return pedersens;
    }

    /**
     * Writes this instance to file.
     *
     * @param log Logging context.
     */
    public void stateToFile(Log log) {
        ByteTreeBasic bt =
            new ByteTreeContainer(ByteTree.booleanToByteTree(trivial),
                                  pedersenBasic.stateToByteTree());
        log.info("Write state to file.");
        bt.unsafeWriteTo(getFile("State"));
    }

    /**
     * Returns <code>true</code> or <code>false</code> depending on if
     * this instance can find its state on file or not.
     *
     * @return <code>true</code> or <code>false</code> depending on if
     * this instance can find its state on file or not.
     */
    public boolean stateOnFile() {
        File file = getFile("State");
        return file.exists();
    }

    /**
     * Checks if this instance resides on file. If so it reads its
     * state from file and returns <code>true</code> and otherwise it
     * returns <code>false</code>.
     *
     * @param log Logging context.
     * @return <code>true</code> or <code>false</code> depending on if
     * the state exists and could be read from file or not.
     */
    protected boolean stateFromFile(Log log) {
        File file = getFile("State");
        if (file.exists()) {

            ByteTreeReader btr = null;
            log.info("Read state from file.");
            try {
                btr = (new ByteTreeF(file)).getByteTreeReader();
                trivial = btr.getNextChild().readBoolean();
                pedersenBasic.
                    unsafeStateFromByteTree(btr.getNextChild());
                if (j == l) {
                    state = State.SECRET_DISTRIBUTED;
                } else {
                    state = State.SHARE_RECEIVED;
                }
            } catch (EIOException eioe) {
                throw new ProtocolError("Unable to open state file!", eioe);
            } finally {
                if (btr != null) {
                    btr.close();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Shares the secret given as input. This is the standard way to
     * deal a secret in a perfectly hiding way.
     *
     * @param log Logging context.
     * @param secret Secret to be shared.
     */
    public void dealSecret(Log log, PRingElement secret) {

        log.info("Deal a secret using Pedersen VSS.");
        Log tempLog = log.newChildLog();

        if (!pedersenBasic.getHom().getDomain().equals(secret.getPRing())) {
            throw new ProtocolError("Secret not in domain of homomorphism!");
        }

        if (state != State.INITIAL) {
            throw new ProtocolError("Attempting to reuse instance!");
        }

        if (j != l) {
            throw new ProtocolError("This instance is not the dealer!");
        }

        // Try to read state from file
        if (storeToFile && stateFromFile(tempLog)) {
            return;
        }

        // Compute sharing.
        tempLog.info("Generate checking elements.");
        pedersenBasic.generateSharing(secret);

        // Compute and encrypt the share of each party.
        ByteTreeBasic[] byteTrees = new ByteTreeBasic[k + 1];

        // Add the checking elements.
        byteTrees[0] = pedersenBasic.getPolynomialInExponent().toByteTree();

        tempLog.info("Compute encrypted shares for all parties.");
        Log tempLog2 = tempLog.newChildLog();
        for (int i = 1; i <= k; i++) {

            // Compute share.
            PRingElement share = pedersenBasic.computeShare(i);

            // Encode it as a byte[]
            byte[] shareBytes = share.toByteTree().toByteArray();

            // Encrypt the byte[]. Use the unique full name as a
            // label. This ensures that the ciphertexts of one
            // instance of a protocol can *never* be used in another
            // instance of any protocol. Formally, this ensures that
            // we can reduce the security to the cryptosystem.
            byte[] ciphertext = pkeys[i].encrypt(getFullName().getBytes(),
                                                 shareBytes,
                                                 randomSource,
                                                 statDist);
            byteTrees[i] = new ByteTree(ciphertext);
        }

        // Write the ByteTree to the bulletin board.
        tempLog.info("Publish checking elements and encrypted shares.");
        bullBoard.publish("Sharing", new ByteTreeContainer(byteTrees), tempLog);

        // Exchange verdicts (we are obviously honest).
        boolean[] verdicts = exchangeVerdicts(tempLog, true);

        // If somebody complained.
        if (!verdicts[0]) {

            tempLog.info("Somebody complained about their share.");

            // Publish the shares of the complaining parties.
            ByteTreeBasic[] shares = new ByteTreeBasic[k + 1];
            Arrays.fill(shares, new ByteTree());

            for (int i = 1; i <= k; i++) {
                if (!verdicts[i]) {
                    shares[i] = pedersenBasic.computeShare(i).toByteTree();
                }
            }
            ByteTreeBasic bt = new ByteTreeContainer(shares);

            tempLog.info("Publish shares of complaining parties.");
            bullBoard.publish("OpenShares", bt, tempLog);
        }

        // Store ourselves to file.
        if (storeToFile) {
            stateToFile(tempLog);
        }

        tempLog.info("Sharing completed.");

        state = State.SECRET_DISTRIBUTED;
    }

    /**
     * Receives a secret share. Returns <code>true</code> or
     * <code>false</code> depending on if the sharing was accepting or not.
     *
     * @param log Logging context.
     * @return Joint verdict of the sharing.
     */
    public boolean receiveShare(Log log) {

        if (l == 0) {
            log.info("Verify jointly generated Pedersen VSS.");
        } else {
            log.info("Verify Pedersen VSS of " + ui.getDescrString(l) + ".");
        }
        Log tempLog = log.newChildLog();

        if (state != State.INITIAL && state != State.SECRET_DISTRIBUTED) {
            throw new ProtocolError("Attempting to reuse instance!");
        }

        // Try to read state from file
        if (storeToFile && stateFromFile(tempLog)) {
            return true;
        }

        state = State.ATTEMPTING_RECEIVE;

        // Assume that sharing is correct to start with
        boolean verdict = true;

        // Wait for polynomial in the exponent and encrypted shares
        tempLog.info("Read checking elements and ciphertexts from "
                     + ui.getDescrString(l) + ".");
        ByteTreeReader reader = bullBoard.waitFor(l, "Sharing", tempLog);

        String s = "Unable to read or parse data!";
        try {

            // Parse the data
            if (reader.getRemaining() != k + 1) {
                tempLog.info("Wrong number of components!");
                verdict = false;
            } else {

                PolynomialInExponent pie =
                    new PolynomialInExponent(pedersenBasic.hom,
                                             reader.getNextChild());
                pedersenBasic.setPolynomialInExponent(pie);

                reader.skipChildren(j - 1);

                byte[] ciphertext = reader.getNextChild().read();
                byte[] plaintext = skey.decrypt(getFullName().getBytes(),
                                                ciphertext);

                reader.close();

                // Check if it decrypted to bot
                if (plaintext == null) {

                    tempLog.info("Invalid ciphertext!");
                    verdict = false;

                } else {

                    // Decode and save the share
                    ByteTreeReader btr =
                        (new ByteTree(plaintext, null)).getByteTreeReader();
                    PRingElement share =
                        pedersenBasic.getHom().getDomain().toElement(btr);
                    pedersenBasic.setShare(share);

                    // Verify the share
                    verdict = pedersenBasic.verifyShare();
                }
            }
        } catch (EIOException eioe) {
            tempLog.info("Could not parse data!");
            verdict = false;
        } catch (ProtocolFormatException pfe) {
            tempLog.info(s);
            verdict = false;
        } catch (ArithmFormatException afe) {
            tempLog.info(s);
            verdict = false;
        } finally {
            reader.close();
        }
        if (verdict) {
            tempLog.info("Our share is correct.");
        } else {
            tempLog.info("Our share is incorrect.");
        }

        // Exchange verdicts.
        boolean[] verdicts = exchangeVerdicts(tempLog, verdict);

        // If somebody complained.
        if (!verdicts[0]) {

            tempLog.info("Read open shares.");
            reader = bullBoard.waitFor(l, "OpenShares", tempLog);

            // Verify all opened shares.
            verdict = true;
            for (int i = 1; verdict && i <= k; i++) {

                try {

                    if (verdicts[i]) {

                        reader.skipChild();

                    } else {

                        PRingElement tmpShare =
                            pedersenBasic.getHom().getDomain().
                            toElement(reader.getNextChild());

                        if (!pedersenBasic.verifyShare(i, tmpShare)) {
                            tempLog.info("Opened share of " +
                                         ui.getDescrString(i) +
                                         " is not correct.");
                            verdict = false;
                        }
                    }
                } catch (ArithmFormatException afe) {
                    tempLog.info("Could not parse open share of " +
                                 ui.getDescrString(i) + ".");
                    verdict = false;
                } catch (EIOException eioe) {
                    tempLog.info("Could not read open share of " +
                                 ui.getDescrString(i) + ".");
                    verdict = false;
                }
            }
            reader.close();
            if (!verdict) {
                tempLog.info("Reject sharing of " +
                             ui.getDescrString(l) + ".");
                return false;
            }
        }
        if (!verdicts[0]) {
            tempLog.info("Dealer successfully refuted all complaints.");
        }

        tempLog.info("Sharing of " + ui.getDescrString(l) + " is accepted.");
        state = State.SHARE_RECEIVED;

        if (storeToFile) {
            stateToFile(tempLog);
        }

        return true;
    }

    /**
     * Publish our verdict and read the verdicts of all other parties.
     *
     * @param log Logging context.
     * @param verdict Verdict of this party.
     * @return Array of all verdicts, where the boolean at index zero
     * is the conjunction of the verdicts of all parties.
     */
    public boolean[] exchangeVerdicts(Log log, boolean verdict) {

        log.info("Exchange verdicts.");
        Log tempLog = log.newChildLog();

        // Make room for everybody's verdicts.
        boolean[] verdicts = new boolean[k + 1];
        Arrays.fill(verdicts, true);

        // Store our verdict.
        verdicts[j] = verdict;

        // Exchange verdicts.
        for (int i = 1; i <= k; i++) {

            if (i == j) {

                // Publish our verdict.
                tempLog.info("Publish verdict (" + verdict + ").");
                bullBoard.publish("Verdict",
                                  ByteTree.booleanToByteTree(verdict),
                                  tempLog);
            } else {

                // Read verdict of other.
                tempLog.info("Read verdict of " + ui.getDescrString(i) + ".");
                ByteTreeReader reader =
                    bullBoard.waitFor(i, "Verdict", tempLog);

                // Verdicts that can not be parsed are set to false.
                verdict = false;
                try {
                    verdict = reader.readBoolean();
                    tempLog.info("Parse verdict (" + verdict + ").");
                } catch (EIOException eioe) {
                    tempLog.info("Unable to parse verdict, assuming false.");
                }
                reader.close();

                verdicts[i] = verdict;

                // Update conjunction of all verdicts.
                if (!verdict) {
                    verdicts[0] = false;
                }
            }
        }
        return verdicts;
    }

    /**
     * Sets this instance to be in the trivial state, i.e., it appears
     * as if the dealer correctly dealt a one. This is useful on
     * higher abstraction levels to eliminate the actions of corrupted
     * parties without introducing special handling.
     *
     * @param log Logging context.
     */
    public void setTrivial(Log log) {
        if (j == l) {
            state = State.SECRET_DISTRIBUTED;
        } else {
            state = State.SHARE_RECEIVED;
        }
        pedersenBasic =
            new PedersenBasic(k, j, l, pedersenBasic.getHom(), statDist);
        trivial = true;

        if (storeToFile) {
            stateToFile(log);
        }
    }

    /**
     * Recovers the secret of the dealer.
     *
     * @param log Logging context.
     * @return Secret of dealer.
     */
    public PRingElement recover(Log log) {

        if (l == 0) {
            log.info("Recover Pedersen VSS generated jointly.");
        } else {
            log.info("Recover Pedersen VSS dealt by " +
                     ui.getDescrString(l) + ".");
        }
        Log tempLog = log.newChildLog();

        if (state != State.SHARE_RECEIVED
            && state != State.SECRET_DISTRIBUTED) {
            String s = "No valid share has been received (or shared), "
                + "or the secret has already been recovered!";
            throw new ProtocolError(s);
        }

        if (trivial) {
            // If the pedersenBasic is trivial, then our share is the
            // secret and there is no need to communicate.
            state = State.SECRET_RECOVERED;
            tempLog.info("Sharing is trivial, returns trivial secret.");
            return pedersenBasic.getShare();
        }


        // Make room for all shares
        int[] indexes = new int[k];
        PRingElement[] shares = new PRingElement[k];
        for (int i = 0; i < shares.length; i++) {
            shares[i] = null;
        }

        // Initialize with our own share
        indexes[0] = j;
        shares[0] = pedersenBasic.getShare();

        // Get the shares of others
        int noShares = 1;

        tempLog.info("Exchange secret shares with all parties.");
        Log tempLog2 = tempLog.newChildLog();
        for (int i = 1; i <= k; i++) {

            if (i != l) {

                if (i == j) {

                    // Publish our share
                    tempLog2.info("Publish our share.");
                    bullBoard.publish("Recover" + l,
                                      pedersenBasic.getShare().toByteTree(),
                                      tempLog2);

                // Wait for the shares of everybody except ourselves
                // and Ml whose secret key we are recovering.
                } else {

                    // Wait for the share of Mi
                    tempLog2.info("Read share from " + ui.getDescrString(i) +
                                  ".");
                    ByteTreeReader reader =
                        bullBoard.waitFor(i, "Recover" + l, tempLog2);

                    // Parse the share
                    indexes[noShares] = i;
                    try {
                        shares[noShares] =
                            pedersenBasic.getHom().getDomain().
                            toElement(reader);

                        // Only keep valid shares
                        if (pedersenBasic.verifyShare(i, shares[noShares])) {
                            tempLog2.info("Share published by " +
                                          ui.getDescrString(i) +
                                          " is correct.");
                            noShares++;
                        } else {
                            tempLog2.info("Share published by "
                                          + ui.getDescrString(i) +
                                          " is incorrect");
                        }
                    } catch (ArithmFormatException afe) {
                        tempLog2.info("Unable to parse share.");
                    } finally {
                        if (reader != null) {
                            reader.close();
                        }
                    }
                }
            }
        }

        // At this point we should have sufficiently many shares
        // to recover the secret key of Ml. If we do not, then
        // something is seriously wrong, and we have to abort.
        if (noShares < pedersenBasic.getDegree() + 1) {
            throw new ProtocolError("Insufficient number of honest "
                                    + ui.getDescrString() + "!");
        }

        PRingElement recoveredShare =
            pedersenBasic.recover(indexes, shares,
                                  pedersenBasic.getDegree() + 1);
        tempLog.info("Interpolate and return secret.");

        state = State.SECRET_RECOVERED;

        return recoveredShare;
    }

    /**
     * Returns the first checking element. This is used in distributed
     * key generation for discrete logarithm based primitives.
     *
     * @param log Logging context.
     * @return Constant coefficient in the exponent.
     */
    public PGroupElement getConstCoeffElement(Log log) {
        if (state == State.SECRET_DISTRIBUTED
            || state == State.SHARE_RECEIVED
            || state == State.SECRET_RECOVERED) {
            return pedersenBasic.getPolynomialInExponent().getElement(0);
        } else {
            throw new ProtocolError("No valid share has been received!");
        }
    }

    // Documented in arithm.PGroupAssociated.java

    public PGroup getPGroup() {
        return pedersenBasic.getPGroup();
    }
}

