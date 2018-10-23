// https://searchcode.com/api/result/60104109/

package ru.ifmo.gpo.core;

import ru.ifmo.gpo.core.instructions.InstructionSequence;
import ru.ifmo.gpo.core.instructions.SequenceGenerationFailedException;
import ru.ifmo.gpo.core.instructions.generic.IGenericInstruction;
import ru.ifmo.gpo.core.vmstate.FailedToSatisfyRestrictionsException;
import ru.ifmo.gpo.core.vmstate.IMachineState;
import ru.ifmo.gpo.core.vmstate.IStateDelta;

import java.util.Random;

/**
 * User: e_smirnov
 * Date: 18.08.2011
 * Time: 13:32:35
 */
public abstract class BaseInstructionSequenceGenerator<StateClass extends IMachineState, DeltaClass extends IStateDelta> implements IInstructionSequenceGenerator {

    protected static Random generator = new Random(System.currentTimeMillis());

    protected ITargetArchitecture targetArchitecture;

    public BaseInstructionSequenceGenerator(ITargetArchitecture target) {
        this.targetArchitecture = target;
    }

    protected int getMaxLengthMultiplier() {
        return Settings.getMaxSequenceLengthMultiplier();
    }

    @Override
    public ru.ifmo.gpo.core.instructions.InstructionSequence generateInstructionSequence(IMachineState source, IStateDelta desiredDelta, int length) throws ru.ifmo.gpo.core.instructions.SequenceGenerationFailedException {
        IMachineState state = targetArchitecture.cloneState(source);
        IStateDelta stateDelta = desiredDelta;
        InstructionSequence result = new InstructionSequence();
        do {
            double probability = computeRestrictionSatisfactionSelectionProbability(stateDelta, length, result);
            if (result.size() > length * getMaxLengthMultiplier()) {
                // failed to find suitable sequence
                throw new SequenceGenerationFailedException(SequenceGenerationFailedException.Cause.TOO_MANY_INSTRUCTIONS, result);
            }
            int numOfTries = 0;
            IGenericInstruction instr;
            do {
                preprocessState(state);
                try {
                    if (generator.nextDouble() < probability || probability < 0) {
                        instr = selectInstructionSatisfyRestrictions(result, (StateClass) state, (DeltaClass) stateDelta);
                    } else {
                        instr = selectAnyInstruction(result, (StateClass) state);
                    }

                } catch (FailedToSatisfyRestrictionsException ex) {
                    instr = null;
                }

            } while (numOfTries++ < Settings.getRetryCount() && instr == null);
            if (instr == null) {
                //after 5 tries we failed to select any instruction
                throw new SequenceGenerationFailedException(SequenceGenerationFailedException.Cause.NO_INSTRUCTION_POSSIBLE, result);
            }
            state = targetArchitecture.evaluateInstruction(state, instr);
            stateDelta = targetArchitecture.getStateDelta(state, desiredDelta.getTarget());
            result.add(instr);

            // separate variables for ease of debugging
            final double breakProbability = computeBreakProbability(result.size(), length, stateDelta.getDelta());
            final double random = Math.random();
            if (random < breakProbability) {
                break;
            }
        } while (true);
        return result;
    }

    @Override
    public ru.ifmo.gpo.core.instructions.InstructionSequence generateInstructionSequence(IMachineState source, IMachineState desiredState, int length) throws ru.ifmo.gpo.core.instructions.SequenceGenerationFailedException {
        return generateInstructionSequence(source, targetArchitecture.getStateDelta(source, desiredState), length);
    }

    /**
     * Do some custom stuff with state (as for some target platforms like java state used here requires some additional tuning)
     *
     * @param state State that will be used for new instruction selection
     */
    protected void preprocessState(IMachineState state) {
        // nothing in base class
    }

    /**
     * Given generated sequence, returns chance of finishing generation
     *
     * @param resultSize    Length of generated sequence
     * @param desiredLength Length of reference sequence
     * @param stateDelta    Delta between last state of generated sequence and desired state (for architectures like x86 where delta=0 is not necessary)
     * @return Chance to break loop and finish generation. Return negative number if some conditions are invalid and generation can not be stopped
     */
    protected abstract double computeBreakProbability(int resultSize, int desiredLength, int stateDelta);

    /**
     * Selects next instruction and its arguments in such way, that after that instruction's application to state, difference with desired state will be less than delta
     *
     * @param alreadyGenerated Already generated part of instruction sequence, can be used to check conditions about instruction ordering. Should not be modified.
     * @param state            Initial state for instruction execution
     * @param delta            Delta between state and some desired target state
     * @return Instruction that reduces delta
     * @throws FailedToSatisfyRestrictionsException
     *          If no instruction exists that reduces delta and operates on state
     */
    protected abstract IGenericInstruction selectInstructionSatisfyRestrictions(InstructionSequence alreadyGenerated, StateClass state, DeltaClass delta) throws FailedToSatisfyRestrictionsException;

    /**
     * Selects next instruction. Selected instruction must be able to operate on given VM state
     *
     * @param alreadyGenerated Already generated part of instruction sequence, can be used to check conditions about instruction ordering. Should not be modified.
     * @param state            State prior to instruction execution
     * @return New instruction
     * @throws FailedToSatisfyRestrictionsException
     *          if no instruction from supported set can operate on state (generally,
     *          quite a rare case, as usually at least NOP instruction can operate on any state)
     */
    protected abstract IGenericInstruction selectAnyInstruction(InstructionSequence alreadyGenerated, StateClass state) throws FailedToSatisfyRestrictionsException;

    /**
     * Computes chance that next instruction to be selected will need to reduce delta (selectInstructionSatisfyRestrictions() will be called)
     *
     * @param stateDelta Delta between current and desired state
     * @param length     Desired average length of code sequence
     * @param result     Currently generated sequence
     * @return Value between 0.0 and 1.0 where 1.0 meens 100% chance of calling selectInstructionSatisfyRestrictions()
     */
    protected double computeRestrictionSatisfactionSelectionProbability(IStateDelta stateDelta, int length, InstructionSequence result) {
        return ((double) stateDelta.getDelta()) / ((length * getMaxLengthMultiplier()) - result.size());
    }
}

