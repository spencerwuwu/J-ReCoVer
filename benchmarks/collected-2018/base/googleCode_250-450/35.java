// https://searchcode.com/api/result/13968934/

package de.swta.freezay.admin.reservation;

import de.swta.freezay.components.ActionPanel;
import de.swta.freezay.components.CountDownLabel;
import de.swta.freezay.components.ExtendedActionPanel;
import de.swta.freezay.components.OptionContentPanel;
import de.swta.freezay.database.JPAController;
import de.swta.freezay.database.controller.exceptions.IllegalOrphanException;
import de.swta.freezay.database.controller.exceptions.NonexistentEntityException;
import de.swta.freezay.database.dbEntities.ItemPackage;
import de.swta.freezay.database.dbEntities.Package;
import de.swta.freezay.database.dbEntities.Reservation;
import de.swta.freezay.database.wicketAdapter.ReservationDataProvider;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.NumberFormatter;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 *
 * @author Jan
 */
public final class ReservationView extends Panel {

    /** ModalWindow to show edit an reservation. */
    private final ModalWindow modalWindow = new ModalWindow("modal");
    /** DataView to display all Reservations. */
    private DefaultDataTable<Reservation> reservationView;
    /** Formatter to format the numbers. */
    private static final NumberFormatter NUMBER_FORMATTER =
            new NumberFormatter(new DecimalFormat("####.##"));

    /**
     * Creates a new instance of ReservationView.
     */
    public ReservationView() {
        this("ReservationView");
    }

    /**
     * Creates a new instance of ReservationView.
     * @param id Id of the panel.
     */
    public ReservationView(String id) {
        super(id);
        init();
    }

    /**
     * Initializes all components.
     */
    private void init() {
        add(modalWindow);

        List<IColumn> columns = new ArrayList<IColumn>();

        columns.add(new PropertyColumn(new Model("Reservierungs-Nr."),
                                    "reservationNr", "reservationNr"));

        columns.add(new AbstractColumn(new Model("Paketname"), "package1.name") {

            public void populateItem(Item cellItem, String componentId, IModel model) {
                final Reservation r = (Reservation) model.getObject();

                /* Panel to show the content of a package. */
                cellItem.add(new ActionPanel(componentId,
                            r.getPackage1().getName(), new AjaxLink("select") {


                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        modalWindow.setContent(new PackageContentView(
                                    modalWindow.getContentId(),
                                    r.getPackage1().getId()) {

                            @Override
                            public void onCloseClicked(AjaxRequestTarget target) {
                                modalWindow.close(target);
                            }
                        });

                        modalWindow.setTitle("Paketinhalt");
                        modalWindow.setInitialHeight(250);
                        modalWindow.setInitialWidth(350);
                        modalWindow.show(target);
                    }
                }));
            }
        });

        columns.add(new PropertyColumn(new Model("Paketanzahl"), "packagecount"));

        columns.add(new AbstractColumn(new Model("Gesamtpreis")) {

            public void populateItem(Item cellItem, String componentId, IModel model) {
                final Reservation r = (Reservation) model.getObject();
                try {
                    cellItem.add(new Label(componentId, ""
                            + NUMBER_FORMATTER.valueToString(
                            r.getPackage1().getPrice() 
                            * (float) r.getPackagecount().intValue()) + ""));

                } catch (ParseException ex) {
                    Logger.getLogger(ReservationView.class.getName()).log(
                            Level.SEVERE, null, ex);
                }
            }
        });

        columns.add(new AbstractColumn(new Model("Gultigkeit"), "validity") {

            public void populateItem(Item cellItem, String componentId, IModel model) {
                final Reservation r = (Reservation) model.getObject();

                cellItem.add(new CountDownLabel(componentId, r.getValidity()));
            }
        });

        columns.add(new AbstractColumn(new Model("Editieren")) {

            public void populateItem(org.apache.wicket.markup.repeater.Item cellItem, String componentId, IModel model) {
                final Reservation r = (Reservation) model.getObject();

                /* Panel to edit a reservation. */
                cellItem.add(new ExtendedActionPanel(componentId, new AjaxLink("select1") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        target.appendJavascript("Wicket.Window.unloadConfirmation = false;");

                        modalWindow.setContent(new OptionContentPanel(modalWindow.getContentId()) {

                            public void onSelect(AjaxRequestTarget target, String selection) {
                                /* Confirm a reservation - delete it from DB. */
                                try {
                                    JPAController.ReservationJpaController.destroy(
                                                            r.getReservationNr());
                                } catch (NonexistentEntityException ex) {
                                    Logger.getLogger(ReservationView.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                /* reduce package count. */
                                Package p = r.getPackage1();
                                int newCount = p.getCount() - (r.getPackagecount().intValue());
                                p.setCount(newCount);
                                try {
                                    /* Edit package with new count. */
                                    JPAController.PackageJpaController.edit(p);

                                } catch (IllegalOrphanException ex) {
                                    Logger.getLogger(
                                            ReservationView.class.getName()).log(
                                            Level.SEVERE, null, ex);
                                } catch (NonexistentEntityException ex) {
                                    Logger.getLogger(
                                            ReservationView.class.getName()).log(
                                            Level.SEVERE, null, ex);
                                } catch (Exception ex) {
                                    Logger.getLogger(
                                            ReservationView.class.getName()).log(
                                            Level.SEVERE, null, ex);
                                }

                                /* reduce count of the item in package. */
                                List<ItemPackage> itemPackages =
                                        r.getPackage1().getItemPackageCollection();

                                for (ItemPackage ip : itemPackages) {
                                    de.swta.freezay.database.dbEntities.Item actItem = ip.getItem();

                                    /* new Itemcount = oldItemCount -
                                     * (PackageCOunt * itemcount in package. */
                                    int newItemCount = actItem.getCount() -
                                            (r.getPackagecount().intValue() * ip.getItemCount());
                                    actItem.setCount(newItemCount);
                                    try {
                                        JPAController.ItemJpaController.edit(actItem);
                                    } catch (IllegalOrphanException ex) {
                                        Logger.getLogger(
                                                ReservationView.class.getName()).log(
                                                Level.SEVERE, null, ex);
                                    } catch (NonexistentEntityException ex) {
                                        Logger.getLogger(
                                                ReservationView.class.getName()).log(
                                                Level.SEVERE, null, ex);
                                    } catch (Exception ex) {
                                        Logger.getLogger(
                                                ReservationView.class.getName()).log(
                                                Level.SEVERE, null, ex);
                                    }
                                }
                                modalWindow.close(target);
                                target.addComponent(ReservationView.this.reservationView);
                            }

                            public void onCancel(AjaxRequestTarget target) {
                                modalWindow.close(target);
                            }
                        });
                        try {
                            /* Confirm message. */
                            ((OptionContentPanel) modalWindow.get(
                                    modalWindow.getContentId())).setMessageText(
                                    "\nWurde der Gesamtpreis von " + NUMBER_FORMATTER.valueToString(
                                    r.getPackage1().getPrice() * r.getPackagecount().intValue()) + "  bezahlt?\n\n" + "Ist das BEZAHLTE Paket  '" + r.getReservationNr() + "' dem Kunden uberreicht worden?");
                        } catch (ParseException ex) {
                            Logger.getLogger(ReservationView.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        modalWindow.setTitle("Reservierung abschliessen");
                        modalWindow.setInitialHeight(200);
                        modalWindow.setInitialWidth(300);
                        modalWindow.show(target);
                    }
                }, "Abschliessen", new AjaxLink("select2") {

                    /* Cancel a reservation. */
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        target.appendJavascript("Wicket.Window.unloadConfirmation = false;");

                        modalWindow.setContent(new OptionContentPanel(modalWindow.getContentId()) {

                            public void onSelect(AjaxRequestTarget target, String selection) {
                                try {
                                    /* delete reservation from DB. */
                                    JPAController.ReservationJpaController.destroy(r.getReservationNr());
                                } catch (NonexistentEntityException ex) {
                                    Logger.getLogger(ReservationView.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                modalWindow.close(target);
                                target.addComponent(ReservationView.this.reservationView);
                            }

                            public void onCancel(AjaxRequestTarget target) {
                                modalWindow.close(target);
                            }
                        });

                        /* message text. */
                        ((OptionContentPanel) modalWindow.get(
                                modalWindow.getContentId())).setMessageText(
                                "Wollen sie die Reservierungsnummer  '"
                                + r.getReservationNr() + "' stornieren?");
                        modalWindow.setTitle("Reservierung stornieren");
                        modalWindow.setInitialHeight(200);
                        modalWindow.setInitialWidth(300);
                        modalWindow.show(target);
                    }
                }, "Stornieren"));
            }
        });

        /* reservation view. */
        reservationView = new DefaultDataTable("table", columns,
                                    new ReservationDataProvider(), 10);
        reservationView.setOutputMarkupId(true);
        add(reservationView);
    }
}

