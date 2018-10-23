// https://searchcode.com/api/result/97090619/

package noobbot;

import noobbot.domain.CarPosition;
import noobbot.domain.Turbo;
import noobbot.domain.internal.TrackElement;
import noobbot.message.AbstractMessage;
import noobbot.message.TurboMessage;

import java.util.List;

/**
 * Created by vtajzich
 */
public class TurboUnit {

    private Turbo turbo;

    RaceTrack raceTrack;

    TrackElement trackElementForTurbo;

    public TurboUnit(RaceTrack raceTrack) {
        this.raceTrack = raceTrack;
    }

    public void onTick(Turbo turbo) {
        this.turbo = turbo;
    }

    public List<AbstractMessage> onCarPosition(List<AbstractMessage> messages, CarPosition carPosition) {

        if (turbo == null/* || raceTrack.getType() == RaceTrack.Type.GENERIC*/) {
            return messages;
        }

        if (trackElementForTurbo == null) {
            trackElementForTurbo = raceTrack.getElements().stream()
                    .filter(e -> !e.isCurve())
                    .reduce((r, l) -> {
                        if (r.getLength() > l.getLength()) {
                            return r;
                        } else {
                            return l;
                        }
                    }).get();
        }

        if (raceTrack.getCurrentElement().equals(trackElementForTurbo)) {
            messages.add(new TurboMessage("GO! GO! GO!"));
            turbo = null;
        }

        return messages;
    }
}

