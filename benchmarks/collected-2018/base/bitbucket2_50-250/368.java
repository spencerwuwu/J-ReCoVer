// https://searchcode.com/api/result/102670080/

package com.liquidatom.derbyscore.ui;

import com.liquidatom.derbyscore.domain.Team;
import com.liquidatom.derbyscore.domain.TeamListener;
import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeListener;

/**
 * A listener implementation which will listen for changes to a teams state and update some common user-interface
 * elements in conjunction with the event.
 *
 * @author Russell Francis (russ@metro-six.com)
 */
@Immutable
@ThreadSafe
public class ControlWindowTeamListener implements TeamListener, Serializable {
    static private final long serialVersionUID = 1L;
    static private final int LOGO_WIDTH = 120;
    static private final int LOGO_HEIGHT = 120;
   
    final private JButton teamButton;
    final private JLabel teamLabel;
    final private JSpinner scoreSpinner;
    final private JSpinner jamSpinner;
    final private JSpinner timeoutSpinner;

    /**
     * Construct a new listener which will update the provided user-interface elements if they are provided.
     *
     * @param teamButton The button which representes the team.
     * @param teamLabel The label containing the team name.
     * @param scoreSpinner The JSpinner which holds the teams current score.
     * @param jamSpinner The JSpinner which holds the teams score for the current jam.
     * @param timeoutSpinner The JSpinner which holds the number of timeouts for the team.
     */
    public ControlWindowTeamListener(
            final JButton teamButton, 
            final JLabel teamLabel, 
            final JSpinner scoreSpinner, 
            final JSpinner jamSpinner, 
            final JSpinner timeoutSpinner)
    {
        super();
        this.teamButton = teamButton;
        this.teamLabel = teamLabel;
        this.scoreSpinner = scoreSpinner;
        this.jamSpinner = jamSpinner;
        this.timeoutSpinner = timeoutSpinner;
    }

    /**
     * Invoked when state on the team has been changed.
     *
     * @param team The team whose state has recently changed.
     */
    public void onChanged(final Team team) {
        if (team != null) {
            team.readLock().lock();
            try {
                UpdateRunnable updater = newUpdateRunnable(team);
                EventQueue.invokeLater(updater);
            }
            finally {
                team.readLock().unlock();
            }
        }
    }

    /**
     * Get the DefaultGraphicsConfiguration.
     *
     * @return The GraphicsConfiguration for the default screen.
     */
    protected GraphicsConfiguration getDefaultGraphicsConfiguration() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
    }

    /**
     * Construct a new UpdateRunnable instance which is used to update the state of the user-interface components inside
     * of the AWT event dispatching thread.
     *
     * @param team The team whose state has changed.
     * @return An UpdateRunnable instance which can be used to update the user-interface state based on the recent
     * changes to the teams state.
     */
    protected UpdateRunnable newUpdateRunnable(final Team team) {
        // In an effort to reduce lock contention, we acquire the readlock and grab all of the state from the
        // team instance up front.
        BufferedImage image;
        String name;
        int score;
        int jamPoints;
        int timeouts;

        team.readLock().lock();
        try {
            image = team.getImage();
            name = team.getName();
            score = team.getScore();
            jamPoints = team.getJamPoints();
            timeouts = team.getTimeouts();
        }
        finally {
            team.readLock().unlock();
        }

        return new UpdateRunnable(image, name, score, jamPoints, timeouts);
    }

    /**
     * Holds the relevent state for the Team which has changed and can be run to update user-interface elements within
     * the AWT event-dispatch thread.
     */
    @Immutable
    @ThreadSafe
    protected class UpdateRunnable implements Runnable {
        final BufferedImage image;
        final String name;
        final int score;
        final int jamPoints;
        final int timeouts;

        public UpdateRunnable(
                final BufferedImage image,
                final String name, 
                final int score, 
                final int jamPoints, 
                final int timeouts)
        {
            super();
            this.image = image;
            this.name = name;
            this.score = score;
            this.jamPoints = jamPoints;
            this.timeouts = timeouts;
        }

        protected Collection<ChangeListener> removeChangeListeners(JSpinner c, Class<? extends ChangeListener> listenerKlass) {
            List<ChangeListener> listenerList = new ArrayList<ChangeListener>();
            ChangeListener[] listeners = c.getChangeListeners();
            for (ChangeListener l : listeners) {
                if (listenerKlass.isAssignableFrom(l.getClass())) {
                    c.removeChangeListener(l);
                    listenerList.add(l);
                }
            }
            return listenerList;
        }

        public void run() {
            if (teamButton != null) {
                GraphicsConfiguration defaultConfiguration = getDefaultGraphicsConfiguration();
                BufferedImage small = defaultConfiguration.createCompatibleImage(LOGO_WIDTH, LOGO_HEIGHT, Transparency.TRANSLUCENT);
                small.getGraphics().drawImage(image, 0, 0, 120, 120, null);
                teamButton.setIcon(new ImageIcon(small));
            }

            if (teamLabel != null) {
                if (name != null && !name.equals(teamLabel.getText())) {
                    teamLabel.setText(name);
                }
            }

            if (scoreSpinner != null) {
                Integer newValue = Integer.valueOf(score);
                if (!scoreSpinner.getValue().equals(newValue)) {
                    Collection<ChangeListener> removeList = removeChangeListeners(scoreSpinner, ScoreChangeListener.class);
                    scoreSpinner.setValue(Integer.valueOf(score));
                    for (ChangeListener listener : removeList) {
                        scoreSpinner.addChangeListener(listener);
                    }
                }
            }

            if (jamSpinner != null) {
                Integer newValue = Integer.valueOf(jamPoints);
                if (!jamSpinner.getValue().equals(newValue)) {
                    Collection<ChangeListener> removeList = removeChangeListeners(jamSpinner, JamPointsChangeListener.class);
                    jamSpinner.setValue(newValue);
                    for (ChangeListener listener : removeList) {
                        jamSpinner.addChangeListener(listener);
                    }
                }
            }

            if (timeoutSpinner != null) {
                Integer newValue = Integer.valueOf(timeouts);
                if (!timeoutSpinner.getValue().equals(newValue)) {
                    Collection<ChangeListener> removeList = removeChangeListeners(timeoutSpinner, TimeoutChangeListener.class);
                    timeoutSpinner.setValue(newValue);
                    for (ChangeListener listener : removeList) {
                        timeoutSpinner.addChangeListener(listener);
                    }
                }
            }
        }
    }
}

