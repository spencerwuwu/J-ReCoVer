// https://searchcode.com/api/result/103280521/

package com.alc.own.tweet.presentation.menu;

import com.alc.own.tweet.ApplicationContext;
import com.alc.own.tweet.Dialogs;
import com.alc.own.tweet.Scenes;
import com.alc.own.tweet.spi.Result;
import com.alc.own.tweet.util.RuleWrapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import twitter4j.TwitterException;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.alc.own.tweet.util.MessageConstants.*;

/**
 * Menu scene presenter
 */
public class MenuPresenter implements Runnable {

    @Inject
    private ApplicationContext context;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Scenes scenes;

    @FXML
    private Label login;
    @FXML
    private Button logout;
    @FXML
    private Button exit;
    @FXML
    private ResourceBundle resources;

    private Optional<InetSocketAddress> lastProxy;

    @Override
    public void run() {
        setProxy();
        applyState(getState());
    }

    /**
     * Start button action handler
     */
    public void onStart() {
        switch (getState()) {
            case NOT_AUTHENTICATED:
                obtainRequestToken();
                break;
            case AUTHENTICATED:
                verifyAndProceed();
                break;
            default:
                break;
        }
        applyState(getState());
    }

    /**
     * Configure button action handler
     */
    public void onConfigure() {
        scenes.gotoConfiguration();
    }

    /**
     * About / Help button action handler
     */
    public void onHelp() {
        scenes.gotoHelp();
    }

    /**
     * Logout button action handler
     */
    public void onLogout() {
        context.reset();
        applyState(getState());
    }

    /**
     * Exit button action handler
     */
    public void onExit() {
        Platform.exit();
    }

    private void obtainRequestToken() {
        try {
            context.acquireRequestToken();
            scenes.gotoPin();
        } catch (TwitterException te) {
            handleException(te, resources.getString(ERROR_REQUEST_TOKEN));
        }
    }

    private void verifyAndProceed() {
        Result<RuleWrapper> validationResult = context.getRules().parallelStream()
                .filter(RuleWrapper::isEnabled)
                .map(rule ->
                        rule.getValidationResult().isSuccess() ? rule.getValidationResult() :
                                Result.<RuleWrapper>failure(String.format(resources.getString(SPI_RULE_INVALID),
                                        rule.getRule(), rule.getValidationResult())))
                .reduce(Result::merge)
                .orElse(Result.failure(resources.getString(COMMON_VALIDATION_NO_RULES)));
        if (validationResult.isSuccess()) {
            scenes.gotoProgress();
        } else {
            dialogs.showWarning(resources.getString(DIALOG_WARNING_TITLE), validationResult.toString());
        }
    }

    private void handleException(TwitterException exception, String message) {
        if (exception.getStatusCode() < 0) {
            dialogs.showWarning(resources.getString(DIALOG_WARNING_TITLE),
                    resources.getString(DIALOG_WARNING_NO_PROXY));
        } else {
            dialogs.showThrowable(message + '\n' + exception.getMessage(), exception);
        }
    }

    private State getState() {
        return context.getTwitter().getAuthorization().isEnabled() ? State.AUTHENTICATED : State.NOT_AUTHENTICATED;
    }

    private void applyState(State state) {
        try {
            if (State.AUTHENTICATED.equals(state)) {
                login.setText(context.getTwitter().getScreenName());
            }
            login.setVisible(State.AUTHENTICATED.equals(state));
            logout.setVisible(State.AUTHENTICATED.equals(state));
            logout.setCancelButton(State.AUTHENTICATED.equals(state));
            exit.setCancelButton(State.NOT_AUTHENTICATED.equals(state));
        } catch (TwitterException te) {
            handleException(te, resources.getString(ERROR_SCREEN_NAME));
        }
    }

    private void setProxy() {
        if (context.getProxy().equals(lastProxy)) return;

        if (context.getProxy().isPresent()) {
            InetSocketAddress proxy = context.getProxy().get();
            System.setProperty("https.proxyHost", proxy.getHostString());
            System.setProperty("https.proxyPort", String.valueOf(proxy.getPort()));
        } else {
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
        }
        lastProxy = context.getProxy();
    }

    private enum State {NOT_AUTHENTICATED, AUTHENTICATED}

}
