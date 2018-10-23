// https://searchcode.com/api/result/57726190/

/*  
 *  Client-Bank: System For Electronic Funds Transfer.
 *  (C) 2011 Serious Corporation <https://bitbucket.org/crome/client-bank>.
 *  This program is free software: you can redistribute and/or modify
 *  it under the terms of the GNU General Public License version 3.
 */

package com.sc.clientbank.client;

import com.sc.clientbank.client.auth.TokenDialog;
import com.sc.clientbank.client.auth.User;
import com.sc.clientbank.client.data.DaoException;
import com.sc.clientbank.client.model.Payment;
import com.sc.clientbank.client.model.PaymentState;
import com.sc.clientbank.client.model.Payments;
import com.sc.clientbank.client.model.Statement;
import com.sc.clientbank.client.model.StatementState;
import com.sc.clientbank.client.model.Statements;
import com.sc.clientbank.communication.AccountStatement;
import com.sc.clientbank.communication.AccountStatementRequest;
import com.sc.clientbank.communication.ClientMessage;
import com.sc.clientbank.communication.Connector;
import com.sc.clientbank.communication.MessageType;
import com.sc.clientbank.communication.PaymentOrderStatus;
import com.sc.clientbank.communication.SignedPaymentOrder;
import com.sc.clientbank.security.DigitalSignature;
import com.sc.clientbank.util.Base64Helper;
import com.sc.clientbank.util.HtmlExport;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Logic layer. Manages application logical state.
 * TODO: do all file and network IO in another threads
 * TODO: remove duplicate code
 * TODO: call payments.update when save change really occured
 * TODO: register items with constant ref on init (selection, trackeditems, current document)
 */
public final class MainControl {

    private static final Logger LOG =
            Logger.getLogger(MainControl.class.getName());
    private ViewAccessor view;
    private ServerAccessor server;
    private Converter res = new Converter(ResourceManager.getBundle(getClass()));

    private Payments payments;
    private Statements statements;
    private User user;

    public MainControl(User user) {
        Connector.initClientTrustStore(SSL_KEYS_FILEPATH, TRUST_STORE_PSWD);
        htmlChooser = new JFileChooser(".");

        htmlChooser.addChoosableFileFilter(
                new FileNameExtensionFilter(res.getString("export.html.desc"), "html"));

        payments = user.getProfile().getPayments();
        statements = user.getProfile().getStatements();
        this.user = user;
    }

    public void setViewAccessor(ViewAccessor viewAccessor) {
        view = viewAccessor;
        keyDialog = new TokenDialog(view.getFrame());
        view.setPayment(payments.create());
    }

    public void setServerAccessor(ServerAccessor server) {
        this.server = server;
    }

    private TokenDialog keyDialog;
    private JFileChooser htmlChooser;

    /*
     * ACTIONS.
     * Warning: actions are called on EDT. Any heavy operations must be done
     * in another threads.
     */

    private void openPayment(Payment p) {
        view.setContent(Content.PAYMENT);
        view.setPayment(p);
        view.setStatus(getPaymentStatus(p));
    }

    @ActionMethod
    public void newPayment() {
        Payment p = payments.create();
        openPayment(p);
    }

    @ActionMethod
    public void openPayment(ActionEvent e) {
        long id = Long.parseLong(e.getActionCommand());
        Payment p = payments.get(id);
        openPayment(p);
    }

    @ActionMethod
    public void copyPayment() {
        Payment p = view.getPayment();
        Payment copy = payments.createCopy(p);
        // TODO: save prompt before copying
        openPayment(copy);
    }

    @ActionMethod
    public void changeContent(ActionEvent e) {
        String command = e.getActionCommand();
        Content content = Content.parse(command);
        view.setContent(content);
        view.setStatus("");
    }

    @ActionMethod
    public void discardPayment() {
        view.setPayment(null);
        view.setContent(Content.PAYMENT_LIST);
        view.setStatus(res.getString("status.payment.Discard.ok"));
    }

    @ActionMethod
    public void deletePayment() {
        Payment p = view.getPayment();

        /* if state is NEW, document is not saved yet and we just discard
         * the changes */
        if (p.getState() == PaymentState.NEW) {
            discardPayment();
            return;
        }

        int count = payments.delete(p);

        if (count != 1) {
            view.setStatus(res.getString("status.delete.error"));
            return;
        }

        view.setPayment(null);
        view.setContent(Content.PAYMENT_LIST);
        view.setStatus(res.getString("status.delete.ok"));
    }

    @ActionMethod
    public void deletePayments() {
        Collection<Payment> selected = view.getPaymentSelection();
        // this size changes after deletion, so back it up
        int selectedCount = selected.size();
        if (selectedCount == 0) {
            return;
        }

        int deletedCount = payments.delete(selected);
        int errors = selectedCount - deletedCount;

        view.setStatus(bulkResultStatus("status.delete.many",
                deletedCount, 0, errors));
    }

    /**
     * Returns true if payment was signed.
     */
    private void sign(Payment p, String privateKey) {
        p.syncPaymentOrderB64();
        byte[] sig = DigitalSignature.signData(
                p.getPaymentOrderB64(), privateKey);
        p.setSignature(sig);
        p.setState(PaymentState.SIGNED);
    }

    /**
     * Returns private key selected by user or null if signing was aborted,
     * i.e. user pressed Cancel.
     */
    private String getKey() {
        try {
            keyDialog.setLogin(user.getCredentials().getLogin());
            keyDialog.setVisible(true);
            String key = keyDialog.getKey();
            boolean keyChanged = user.updateSignature(key);
            if (keyChanged) {
                LOG.log(Level.INFO, "Private key has been changed. Saving new login signature.");
            }
            return key;
        } catch (ClientException ex) {
            view.setStatus(ex.getMessage());
            return null;
        }
    }

    /**
     * Gets key and signs one payment. Returns true if succeeded.
     */
    private boolean signOne(Payment p) {
        String key = getKey();
        if (key == null) {
            return false;
        } else {
            sign(p, key);
            return true;
        }
    }

    @ActionMethod
    public void signPayment() {
        Payment p = view.getPayment();

        if (!view.isPaymentFormValid()) {
            view.setStatus("The form contains errors"); // TODO: localize
            return;
        }

        PaymentState oldState = p.getState();

        if (signOne(p)) {
            save(p, oldState);
        }
    }

    // TODO: fix double save on positive scenario
    // TODO: use signPayment code when save<>saveVerbose problem solved
    @ActionMethod
    public void sendPayment() {
        Payment p = view.getPayment();
        PaymentState oldState = p.getState();
        if (!oldState.precedes(PaymentState.SENT)) {
            throw new IllegalStateException("sending SENT payment");
        }

        if (!view.isPaymentFormValid()) {
            view.setStatus("The form contains errors"); // TODO: localize
            return;
        }

        if (oldState != PaymentState.SIGNED) {
            if (!signOne(p)) {
                return;
            }
            if (!save(p, oldState)) {
                return;
            }
            oldState = p.getState();
        }

        try {
            SignedPaymentOrder spo = p.createSignedPaymentOrder();
            String request64 = Base64Helper.toBase64String(spo);

            ClientMessage response = server.sendMessage(
                    MessageType.AddPaymentOrderRequest, request64);

            if (response.getErrorCode() != 0 || response.getMessageParameters() == null) {
                p.setServerErrorCode(response.getErrorCode());
                p.setServerError(response.getErrorText());
                save(p, oldState);
                view.setStatus(String.format(res.getString("error.response.error"), response.getErrorText()));
                return;
            }

            long serverId = (Integer) response.getMessageParameters();

            p.setServerId(serverId);
            p.setState(PaymentState.SENT);
            save(p, oldState);
        } catch (ServerException ex) {
            view.setStatus(ex.getMessage());
        }
    }

    @ActionMethod
    public void signPayments() {
        String privateKey = getKey();
        if (privateKey == null) {
            return;
        }

        Collection<Payment> selected = view.getPaymentSelection();
        if (selected.isEmpty()) {
            return;
        }
        Set<Payment> signed = new HashSet<Payment>();
        int skipped = 0;
        for (Payment p : selected) {
            // TODO: validate here, skip invalid

            if (!p.getState().precedes(PaymentState.SENT)) {
                skipped++;
                continue;  // skip sent
            }

            sign(p, privateKey);
            signed.add(p);
        }

        int savedCount = payments.updateStatuses(signed);

        view.setStatus(bulkResultStatus("status.payments.sign", savedCount, skipped, 0));
    }

    // TODO: remove dupe code
    // TODO: consider sequential status update
    @ActionMethod
    public void sendPayments() {
        // TODO: use signPayments code (first sign all, save, then go sending)
        // TODO: check if key really needed (selection has unsigned docs)
        String privateKey = getKey();
        if (privateKey == null) {
            return;
        }

        Collection<Payment> selected = view.getPaymentSelection();
        if (selected.isEmpty()) {
            return;
        }

        Set<Payment> signedUnsent = new HashSet<Payment>();
        Set<Payment> sent = new HashSet<Payment>();
        int skipped = 0;
        boolean aborted = false;
        for (Payment p : selected) {
            // TODO: validate here, skip invalid

            if (!p.getState().precedes(PaymentState.SENT)) {
                skipped++;
                continue;  // skip sent and later states
            }

            if (p.getState() != PaymentState.SIGNED) {
                sign(p, privateKey);
            }

            try {
                SignedPaymentOrder spo = p.createSignedPaymentOrder();
                String request64 = Base64Helper.toBase64String(spo);

                ClientMessage response = server.sendMessage(
                        MessageType.AddPaymentOrderRequest, request64);

                if (response.getErrorCode() != 0) {
                    signedUnsent.add(p);
                    continue;
                }

                long serverId = (Integer) response.getMessageParameters();
                p.setServerId(serverId);
                p.setState(PaymentState.SENT);
                sent.add(p);
            } catch (NoConnectionException ex) {
                signedUnsent.add(p);
                aborted = true;
                break;
            } catch (ServerException ex) {
                signedUnsent.add(p);
            }
        }

        payments.updateStatuses(sent);
        payments.updateStatuses(signedUnsent);

        StringBuilder sb = new StringBuilder();
        if (aborted) {
            sb.append(res.getString("status.abord.conn")).append(". ");
        }
        sb.append(bulkResultStatus("status.payments.send",
                sent.size(), skipped, signedUnsent.size()));

        view.setStatus(sb.toString());
    }

    @ActionMethod
    public void updatePayments() {
        Set<Payment> tracked = payments.getTrackedPayments();
        Set<Payment> updated = new HashSet<Payment>();

        int errors = 0;
        boolean aborted = false;
        for (Payment p : tracked) {
            try {
                ClientMessage response = server.sendMessage(
                        MessageType.PaymentOrderStatusRequest, (int) p.getServerId());

                if (response.getErrorCode() != 0) {
                    p.setServerErrorCode(response.getErrorCode());
                    p.setServerError(response.getErrorText());
                    errors++;
                    break;
                }

                PaymentOrderStatus status = (PaymentOrderStatus)
                        response.getMessageParameters();
                PaymentState state = PaymentState.parseServerStatus(
                        status.getMessageStatusCode());
                p.setState(state);

                int errorCode = status.getMessageErrorCode();
                p.setServerErrorCode(errorCode);
                String error = status.getMessageError();
                p.setServerError(error);
                p.setSaveDate(new Date());
                updated.add(p);
            } catch (NoConnectionException ex) {
                aborted = true;
                break;
            } catch (ServerException ex) {
                errors++;
            }
        }

        payments.updateStatuses(updated);

        StringBuilder sb = new StringBuilder();
        if (aborted) {
            sb.append(res.getString("status.abord.conn")).append(". ");
        }
        sb.append(bulkResultStatus("status.payments.update",
                updated.size(), 0, errors));
        view.setStatus(sb.toString());
    }

    @ActionMethod
    public void savePayment() {
        Payment payment = view.getPayment();
        PaymentState oldState = payment.getState();
        
        PaymentState newState = view.isPaymentFormValid() ?
            PaymentState.VALID : PaymentState.DRAFT;

        payment.setState(newState);
        payment.syncPaymentOrderB64();
        payment.setSignature(null);
        save(payment, oldState);
    }

    @ActionMethod
    public void updateStatements() {
        Set<Statement> tracked = new HashSet<Statement>(statements.getTrackedStatements());

        ClientMessage newResponse;
        try {
            newResponse = server.sendMessage(
                    MessageType.NewAccountStatementsSeqnosRequest, null);

            if (newResponse.getErrorCode() != 0) {
                view.setStatus(String.format(res.getString("error.response.error"), newResponse.getErrorText()));
                return;
            }
            
            if (newResponse.getMessageParameters() == null) {
                view.setStatus(res.getString("error.response.null"));
                return;
            }
        } catch (ServerException ex) {
            view.setStatus(ex.getMessage());
            return;
        }

        @SuppressWarnings("unchecked")
        List<Integer> newSeqnos = (ArrayList<Integer>) newResponse.getMessageParameters();

        for (Integer seqno : newSeqnos) {
            Statement s = new Statement(seqno);
            tracked.add(s);
        }

        boolean aborted = false;
        int errors = 0;
        int obtained = 0;
        for (Statement statement : tracked) {
            try {
                Integer seqno = (int) statement.getServerId();
                ClientMessage response = server.sendMessage(
                        MessageType.GetAccountStatementRequest, seqno);

                if (response.getErrorCode() != 0 || response.getMessageParameters() == null) {
                    statement.setState(StatementState.ERROR);
                    errors++;
                    continue;
                }

                AccountStatement accSta = (AccountStatement) response.getMessageParameters();

                if (newSeqnos.contains((int) statement.getServerId())) {
                    statement.setAccNo(accSta.getAccountIdentifier());
                    statement.setSinceDate(accSta.getStartDate());
                    statement.setTillDate(accSta.getEndDate());
                }

                if (accSta.getErrorCode() != 0) {
                    statement.setState(StatementState.ERROR);
                    errors++;
                    continue;
                }

                statement.setBody(accSta);
                statement.setState(StatementState.OBTAINED);
                obtained++;
            } catch (NoConnectionException ex) {
                statement.setState(StatementState.ERROR);
                errors++;
                aborted = true;
            } catch (ServerException ex) {
                statement.setState(StatementState.ERROR);
                errors++;
            }
        }

        statements.save(tracked);

        StringBuilder sb = new StringBuilder();
        if (aborted) {
            sb.append(res.getString("status.abord.conn")).append(". ");
        }
        sb.append(bulkResultStatus("status.statements.receive",
                obtained, 0, errors));
        view.setStatus(sb.toString());
    }

    @ActionMethod
    public void deleteStatements() {
        Collection<Statement> selected = view.getStatementSelection();
        // this size changes after deletion, so back it up
        int selectedCount = selected.size();
        if (selectedCount == 0) {
            return;
        }

        Statement currentStatement = view.getStatement();
        boolean isCurrentSelected = selected.contains(currentStatement);

        int deletedCount = statements.delete(selected);
        int errors = selectedCount - deletedCount;

        boolean isCurrentDeleted = !selected.contains(currentStatement);

        if (isCurrentSelected && isCurrentDeleted) {
            view.setStatement(null);
        }

        view.setStatus(bulkResultStatus("status.delete.many",
                deletedCount, 0, errors));
    }

    @ActionMethod
    public void requestStatement() {
        try {
            AccountStatementRequest request = view.getStatementRequest();

            if (request == null) {
                view.setStatus("Request contains errors");
                return;
            }

            String request64 = Base64Helper.toBase64String(request);
            ClientMessage response = server.sendMessage(
                    MessageType.AccountStatementRequest, request64);

            if (response.getErrorCode() != 0) {
                view.setStatus(String.format(res.getString("error.response.error"), response.getErrorText()));
                return;
            }

            if (response.getMessageParameters() == null) {
                view.setStatus(res.getString("error.response.null"));
                return;
            }

            long serverId = (Integer) response.getMessageParameters();
            Statement s = new Statement(serverId);
            s.setAccNo(request.getAccount());
            s.setSinceDate(request.getStartDate());
            s.setTillDate(request.getEndDate());
            s.setState(StatementState.QUEUED);

            statements.save(s);
            view.setStatus(res.getString("status.statement.request.ok"));
        } catch (ServerException ex) {
            view.setStatus(ex.getMessage());
        } catch (DaoException ex) {
            LOG.log(Level.SEVERE, null, ex);
            view.setStatus(res.getString("status.statement.save.error"));
        }
    }

    private DateFormat fileDateFormatter = new SimpleDateFormat("yyyy.MM.dd");

    private String suggestFilename(Payment p) {
        StringBuilder sb = new StringBuilder();
        sb.append(res.getString("export.payment.prefix"));
        sb.append(p.getFieldData().getString("number")).append('_');
        sb.append(fileDateFormatter.format(p.getFieldData().getDate("date")));
        return sb.toString();
    }

    @ActionMethod
    public void exportPayment() {
        Payment p = view.getPayment();
        String html = HtmlExport.toHtml(p.createPaymentOrder());
        saveHtml(html, suggestFilename(p), "export.payment.title");
    }

    private String suggestFilename(Statement s) {
        StringBuilder sb = new StringBuilder();
        sb.append(res.getString("export.statement.prefix"));
        sb.append(s.getBody().getAccountIdentifier()).append('_');
        sb.append(fileDateFormatter.format(s.getBody().getStartDate())).append('-');
        sb.append(fileDateFormatter.format(s.getBody().getEndDate()));
        return sb.toString();
    }

    @ActionMethod
    public void exportStatement() {
        Statement s = view.getStatement();
        String html = HtmlExport.toHtml(s.getBody());
        saveHtml(html, suggestFilename(s), "export.statement.title");
    }

    private void saveHtml(String html, String proposedName, String titleKey) {
        try {
            String filename = proposedName + ".html";
            File f = new File(filename).getCanonicalFile();
            htmlChooser.setSelectedFile(f);
            htmlChooser.setDialogTitle(res.getString(titleKey));
            int returnVal = htmlChooser.showSaveDialog(view.getFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File fsel = htmlChooser.getSelectedFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(fsel, false));
                out.write(html);
                out.close();
            }
        } catch (IOException ex) {
            view.setStatus(res.getString("export.error"));
        }
    }

    @ActionMethod
    public void deleteStatement() {
        Statement s = view.getStatement();

        try {
            statements.delete(s);
            view.setStatement(null);
            view.setStatus(res.getString("status.delete.ok"));
        } catch (DaoException ex) {
            LOG.log(Level.SEVERE, null, ex);
            view.setStatus(res.getString("status.delete.error"));
        }
    }

    /**
     * Save payment to storage. Calls add() if payment is NEW or update()
     * otherwise. End point of any save operation.
     * TODO: solve save<>saveVerbose mess
     * TODO: ask DAO if exist, remove initState param
     * TODO: maybe move to Payments
     * TODO: reduce API: leave only save(), remove add() and update()
     */
    private boolean save(Payment p, PaymentState initState) {
        Date date = new Date();
        p.setSaveDate(date);
        int count = (initState == PaymentState.NEW) ?
            payments.add(p) : payments.update(p);

        if (count != 1) {
            view.setStatus(res.getString("status.payment.Save.error"));
            return false;
        }

        if (p.getState() == PaymentState.VALID ||
            p.getState() == PaymentState.SIGNED) {
            view.updateDirectory();
        }

        view.setStatus(getPaymentStatus(p));
        return true;
    }

    private String bulkResultStatus(String resKey, int good, int skipped, int errors) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(res.getString(resKey), good));

        if (skipped > 0 || errors > 0) {
            sb.append(" (");
            if (skipped > 0) {
                sb.append(String.format(res.getString("status.skipped"), skipped));
                if (errors > 0) {
                    sb.append(", ");
                }
            }
            if (errors > 0) {
                sb.append(String.format(res.getString("status.errors"), errors));
            }
            sb.append(")");
        }

        return sb.toString();
    }

    /**
     * Construct positive (all good) status string for Payment p.
     */
    private String getPaymentStatus(Payment p) {
        String resKey;
        switch (p.getState()) {
            case NEW:
                resKey = "status.payment.New";
                return res.getString(resKey);
            case SIGNED:
                resKey = "status.payment.Sign.ok";
                break;
            case SENT:
                resKey = "status.payment.Send.ok";
                break;
            default:
                resKey = "status.payment.Save.ok";
                break;
        }

        Date saveDate = p.getSaveDate();

        String status = String.format(
                res.getString(resKey), getDateString(saveDate));

        return status;
    }

    /**
     * Returns string with current date and time, formatted using default
     * locale.
     */
    private String getDateString(Date date) {
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String day = new SimpleDateFormat("EEE").format(date);
        return String.format(res.getString("status.payment.Save.dateFormat"),
                timeFormat.format(date), day, dateFormat.format(date));
    }

    /*
     * TEST CODE BELOW
     * REMOVE WHEN OBSOLETE
     */

    // TODO: store in settings
    private final String SSL_KEYS_FILEPATH = "data/clientTrustStore";

    // TODO: move trust store password to a better place
    // 1. client must never see this pwd
    // 2. must be embedded in code
    // 3. it should obfuscated in real life
    private final String TRUST_STORE_PSWD = "87654321";
}

