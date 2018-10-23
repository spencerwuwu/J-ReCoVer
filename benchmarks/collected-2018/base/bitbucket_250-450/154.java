// https://searchcode.com/api/result/97090582/

package noobbot;

import com.google.common.collect.Lists;
import noobbot.bestlane.StupidBrain;
import noobbot.domain.CarPosition;
import noobbot.domain.Lane;
import noobbot.domain.Piece;
import noobbot.domain.PiecePosition;
import noobbot.domain.internal.LapPiece;
import noobbot.domain.internal.TrackElement;
import noobbot.message.SwitchDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by vtajzich
 */
public class RaceTrack implements GameTickAware {

    static enum Type {

        GENERIC(2, 4), OVAL(60, 60), NOT_EXTRA_CURVES(4, 8);

        private double minSpeed;
        private double highSpeed;

        Type(double minSpeed, double highSpeed) {

            this.minSpeed = minSpeed;
            this.highSpeed = highSpeed;
        }

        public double getMinSpeed() {
            return minSpeed;
        }

        public double getHighSpeed() {
            return highSpeed;
        }
    }

    List<TrackElement> elements = new ArrayList<TrackElement>();

    int previousElementIndex = 0;
    int currentElementIndex = 0;
    PiecePosition currentPiecePosition;
    int lastPieceIndex = 0;
    StupidBrain brain;
    private List<LapPiece> pieciesToEndOfRace;
    private SwitchDirection switchDirection = SwitchDirection.None;
    private List<Piece> piecesInLap;
    private Type type;

    public RaceTrack() {
    }

    public RaceTrack(List<Piece> pieces, List<Lane> lanes, int laps) {
        piecesInLap = pieces;
        brain = new StupidBrain(lanes, laps);
        pieciesToEndOfRace = createWholeTrack(pieces, laps);

        AtomicInteger index = new AtomicInteger();

        elements = pieces.stream()
                .peek(p -> p.setIndex(index.getAndIncrement()))
                .map(piece -> new TrackElement(piece))
                .map(element -> Lists.newArrayList(element))
                .reduce((ArrayList<TrackElement> previous, ArrayList<TrackElement> current) -> {

                    TrackElement previousTrackElement = previous.get(previous.size() - 1);
                    TrackElement currentTrackElement = current.get(0);

                    if (previousTrackElement.getOriginalAngle() == currentTrackElement.getOriginalAngle()) {
                        previousTrackElement.plus(currentTrackElement);
                    } else {
                        previous.add(currentTrackElement);
                    }

                    return previous;

                }).get();
    }

    public List<TrackElement> getElements() {
        return elements;
    }

    public void add(TrackElement element) {
        elements.add(element);
    }

    public void onTick(CarPosition carPosition) {

        currentPiecePosition = carPosition.getPiecePosition();

        if (movedToNextPiece() && !pieciesToEndOfRace.isEmpty()) {
            pieciesToEndOfRace = pieciesToEndOfRace.subList(1, pieciesToEndOfRace.size());
            int currentLane = carPosition.getPiecePosition().getLane().getStartLaneIndex();
            int bestLane = brain.getBestLane(pieciesToEndOfRace, currentLane);
            chooseSwitchDirection(currentLane, bestLane);
        }

        updateToCurrentPieceIndex(currentPiecePosition);
        updateLastPieceIndex(currentPiecePosition);
    }

    private void updateLastPieceIndex(PiecePosition currentPiecePosition) {
        lastPieceIndex = currentPiecePosition.getPieceIndex();
    }

    public boolean movedToNextPiece() {
        return lastPieceIndex != currentPiecePosition.getPieceIndex();
    }

    private void chooseSwitchDirection(int currentLane, int bestLane) {
        if (bestLane == -1)
            switchDirection = SwitchDirection.None;
        else if (bestLane > currentLane)
            switchDirection = SwitchDirection.Right;
        else if (bestLane < currentLane)
            switchDirection = SwitchDirection.Left;
        else
            switchDirection = SwitchDirection.None;
    }

    private void updateToCurrentPieceIndex(PiecePosition piecePosition) {

        previousElementIndex = currentElementIndex;

        TrackElement current = elements.stream().filter(e -> e.getPieceFor(piecePosition) != null).findFirst().get();
        currentElementIndex = elements.indexOf(current);
    }

    public boolean movedToNextElement() {
        return previousElementIndex != currentElementIndex;
    }

    public boolean isCurve() {
        return getCurrentElement().isCurve();
    }

    public ThrottleTeKey getNextThrottleTeKey() {

        TrackElement nextElement = getNextElement();

        return new ThrottleTeKey(nextElement, nextElement.getFirstPiece());
    }

    public double getLengthTillNextCurve() {

        TrackElement current = getCurrentElement();

        PiecePosition currentPiecePos = getCurrentPiecePosition();

        double currentLeft = current.getLength(currentPiecePos.getPieceIndex(), currentPiecePos.getInPieceDistance(), true);

        List<TrackElement> nextElements = elements.stream()
                .filter(e -> elements.indexOf(e) > elements.indexOf(current))
                .collect(Collectors.toList());

        TrackElement bigCurveElement = nextElements.stream().filter(e -> e.getAngleAbs() > 40).findFirst().orElseGet(() -> getNextElement());

        return nextElements.stream()
                .filter(e -> nextElements.indexOf(e) < nextElements.indexOf(bigCurveElement))
                .mapToDouble(e -> e.getLength())
                .reduce(currentLeft, (a, b) -> a + b);
    }

    public TrackElement getNextElement() {

        int nextIndex = currentElementIndex + 1;

        if (nextIndex > elements.size() - 1) {
            nextIndex = 0;
        }

        return elements.get(nextIndex);
    }

    public ThrottleTeKey getThrottleTeKey() {

        TrackElement element = getCurrentElement();

        return new ThrottleTeKey(element, element.getPieceFor(getCurrentPiecePosition()));
    }

    public TrackElement getCurrentElement() {
        return elements.get(currentElementIndex);
    }

    protected PiecePosition getCurrentPiecePosition() {

        if (currentPiecePosition == null) {
            currentPiecePosition = new PiecePosition(0, 0);
        }

        return currentPiecePosition;
    }

    public SwitchDirection shouldSwitch() {
        return switchDirection;
    }

    /**
     * Return list of LasPieces for whole track - new array contain pieces for one track multiplied by number of laps.
     *
     * @param pieces
     * @param laps
     * @return
     */
    public static List<LapPiece> createWholeTrack(List<Piece> pieces, int laps) {
        List<LapPiece> track = new ArrayList<>();
        for (int lap = 0; lap < laps; lap++) {
            for (Piece piece : pieces) {
                LapPiece lapPiece = new LapPiece(piece);
                lapPiece.setLap(lap);
                track.add(lapPiece);
            }
        }
        return track;
    }


    @Override
    public String toString() {
        return "RaceTrack{" +
                "elements=" + elements +
                ", currentPiecePosition=" + currentPiecePosition +
                '}';
    }

    public boolean isSwitch() {
        return piecesInLap.get(currentPiecePosition.getPieceIndex()).isSwitchPiece();
    }

    public Type getType() {

        if (type == null) {
            type = classifyTrack();
        }

        return type;
    }

    protected Type classifyTrack() {

        if (!elements.stream().filter(e -> e.getOriginalAngleAbs() > 23).findFirst().isPresent()) {
            return Type.OVAL;
        } else if (!elements.stream().filter(e -> e.getOriginalAngle() >= 45 && e.getRadius() < 100).findFirst().isPresent()) {
            return Type.NOT_EXTRA_CURVES;
        }

        return Type.GENERIC;
    }
}

