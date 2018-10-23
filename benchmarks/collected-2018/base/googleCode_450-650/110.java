// https://searchcode.com/api/result/12691724/

/*******************************************************************************
 * Copyright (c) 2008, 2012 The Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.zend.usagedata.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.zend.usagedata.gathering.UsageDataEvent;
import org.zend.usagedata.recording.IUploadParameters;
import org.zend.usagedata.recording.UsageDataFileReader;
import org.zend.usagedata.ui.internal.Messages;
import org.zend.usagedata.ui.internal.UIUsageDataActivator;

public class UploadPreview {

	private final IUploadParameters parameters;

	TableViewer viewer;
	Job contentJob;
	List<UsageDataEventWrapper> events = Collections
			.synchronizedList(new ArrayList<UsageDataEventWrapper>());

	private static final DateFormat dateFormat = DateFormat
			.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

	private UsageDataTableViewerColumn whatColumn;
	private UsageDataTableViewerColumn kindColumn;
	private UsageDataTableViewerColumn descriptionColumn;
	private UsageDataTableViewerColumn bundleIdColumn;
	private UsageDataTableViewerColumn bundleVersionColumn;
	private UsageDataTableViewerColumn timestampColumn;

	private Color colorBlack;

	private Cursor busyCursor;

	Button removeFilterButton;

	private Composite container;

	public UploadPreview(IUploadParameters parameters) {
		this.parameters = parameters;
	}

	public Control createControl(final Composite parent) {
		allocateResources(parent);

		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());

		createEventsTable(container);
		/*
		 * Bit of a crazy idea. Add a paint listener that will start the job of
		 * actually populating the page when the composite is exposed. If the
		 * instance is created in a wizard, it won't start populating until the
		 * user actually switches to the page.
		 */
		final PaintListener paintListener = new PaintListener() {
			boolean called = false;

			// Don't need to synchronize since this will only ever be called in
			// the UI thread.
			public void paintControl(PaintEvent e) {
				if (called)
					return;
				called = true;
				startContentJob();
			}
		};
		container.addPaintListener(paintListener);

		return container;
	}

	/*
	 * This method allocates the resources used by the receiver. It installs a
	 * dispose listener on parent so that when the parent is disposed, the
	 * allocated resources will be deallocated.
	 */
	private void allocateResources(Composite parent) {
		colorBlack = parent.getDisplay().getSystemColor(SWT.COLOR_BLACK);
		busyCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
	}

	private void createEventsTable(Composite parent) {
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER
				| SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(false);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		layoutData.widthHint = 500;
		layoutData.heightHint = 200;
		viewer.getTable().setLayoutData(layoutData);

		createWhatColumn();
		createKindColumn();
		createDescriptionColumn();
		createBundleIdColumn();
		createBundleVersionColumn();
		createTimestampColumn();

		timestampColumn.setSortColumn();

		viewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {
			}

			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			@SuppressWarnings("unchecked")
			public Object[] getElements(Object input) {
				if (input instanceof List) {
					return (Object[]) ((List<UsageDataEventWrapper>) input)
							.toArray(new Object[((List<UsageDataEventWrapper>) input)
									.size()]);
				}
				return new Object[0];
			}
		});

		// Initially, we have nothing.
		viewer.setInput(events);
	}

	private void createWhatColumn() {
		whatColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		whatColumn.setText(Messages.UploadPreview_What);
		whatColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return event.getWhat();
			}
		});
	}

	private void createKindColumn() {
		kindColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		kindColumn.setText(Messages.UploadPreview_Kind);
		kindColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return event.getKind();
			}
		});
	}

	private void createDescriptionColumn() {
		descriptionColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		descriptionColumn.setText(Messages.UploadPreview_Description);
		descriptionColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return event.getDescription();
			}
		});
	}

	private void createBundleIdColumn() {
		bundleIdColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		bundleIdColumn.setText(Messages.UploadPreview_BundleId);
		bundleIdColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return event.getBundleId();
			}
		});
	}

	private void createBundleVersionColumn() {
		bundleVersionColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		bundleVersionColumn.setText(Messages.UploadPreview_Version);
		bundleVersionColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return event.getBundleVersion();
			}
		});
	}

	private void createTimestampColumn() {
		timestampColumn = new UsageDataTableViewerColumn(SWT.LEFT);
		timestampColumn.setText(Messages.UploadPreview_When);
		timestampColumn.setLabelProvider(new UsageDataColumnProvider() {
			@Override
			public String getText(UsageDataEventWrapper event) {
				return dateFormat.format(new Date(event.getWhen()));
			}
		});
		timestampColumn.setSorter(new Comparator<UsageDataEventWrapper>() {
			public int compare(UsageDataEventWrapper event1,
					UsageDataEventWrapper event2) {
				if (event1.getWhen() == event2.getWhen())
					return 0;
				return event1.getWhen() > event2.getWhen() ? 1 : -1;
			}
		});
	}

	/**
	 * This method starts the job that populates the list of events.
	 */
	synchronized void startContentJob() {
		if (contentJob != null)
			return;
		contentJob = new Job("Generate Usage Data Upload Preview") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				setTableCursor(busyCursor);
				processFiles(monitor);
				setTableCursor(null);
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;
				return Status.OK_STATUS;
			}

			private void setTableCursor(final Cursor cursor) {
				if (isDisposed())
					return;
				getDisplay().syncExec(new Runnable() {
					public void run() {
						if (isDisposed())
							return;
						viewer.getTable().setCursor(cursor);
					}
				});
			}
		};
		contentJob.setPriority(Job.LONG);
		contentJob.schedule();
	}

	void processFiles(IProgressMonitor monitor) {
		File[] files = parameters.getFiles();
		monitor.beginTask("Process Files", files.length); //$NON-NLS-1$
		for (File file : files) {
			if (isDisposed())
				break;
			if (monitor.isCanceled())
				break;
			processFile(file, monitor);
			monitor.worked(1);
		}
		monitor.done();
	}

	/**
	 * This method extracts the events found in a {@link File} and adds them to
	 * the list of events displayed by the receiver. Events are batched into
	 * groups to reduce the number of times the viewer will have to update.
	 * 
	 * @param file
	 *            the {@link File} to process.
	 * @param monitor
	 *            the monitor.
	 */
	void processFile(File file, IProgressMonitor monitor) {
		final List<UsageDataEventWrapper> events = new ArrayList<UsageDataEventWrapper>();
		UsageDataFileReader reader = null;
		try {
			reader = new UsageDataFileReader(file);
			reader.iterate(monitor, new UsageDataFileReader.Iterator() {
				public void header(String header) {
					// Ignore the header.
				}

				public void event(String line, UsageDataEvent event) {
					events.add(new UsageDataEventWrapper(event));
					if (events.size() > 25) {
						addEvents(events);
						events.clear();
					}
				}
			});
			addEvents(events);
		} catch (Exception e) {
			UIUsageDataActivator.log(e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
			}
		}
	}

	boolean isDisposed() {
		if (viewer == null)
			return true;
		if (viewer.getTable() == null)
			return true;
		return viewer.getTable().isDisposed();
	}

	/*
	 * This method adds the list of events to the master list maintained by the
	 * instance. It also updates the table.
	 */
	void addEvents(List<UsageDataEventWrapper> newEvents) {
		if (isDisposed())
			return;
		events.addAll(newEvents);

		final Object[] array = (Object[]) newEvents
				.toArray(new Object[newEvents.size()]);
		getDisplay().syncExec(new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				viewer.add(array);
				resizeColumns(array);
			}
		});
	}

	private Display getDisplay() {
		return viewer.getTable().getDisplay();
	}

	/*
	 * Oddly enough, this method resizes the columns. In order to figure out how
	 * wide to make the columns, we need to use a GC (specifically, the {@link
	 * GC#textExtent(String)} method). To avoid creating too many of them, we
	 * create one in this method and pass it into the helper method {@link
	 * #resizeColumn(GC, UsageDataTableViewerColumn)} which does most of the
	 * heavy lifting.
	 * 
	 * This method must be run in the UI Thread.
	 */
	void resizeColumns(final Object[] events) {
		if (isDisposed())
			return;

		GC gc = new GC(getDisplay());
		gc.setFont(viewer.getTable().getFont());
		resizeColumn(gc, whatColumn, events);
		resizeColumn(gc, kindColumn, events);
		resizeColumn(gc, bundleIdColumn, events);
		resizeColumn(gc, bundleVersionColumn, events);
		resizeColumn(gc, descriptionColumn, events);
		resizeColumn(gc, timestampColumn, events);
		gc.dispose();
	}

	void resizeColumn(GC gc, UsageDataTableViewerColumn column, Object[] events) {
		column.resize(gc, events);
	}

	/**
	 * The {@link UsageDataTableViewerColumn} provides a level of abstraction
	 * for building table columns specifically for the table displaying
	 * instances of {@link UsageDataEventWrapper}. Instances automatically know
	 * how to sort themselves (ascending only) with help from the label
	 * provider. This behaviour can be overridden by providing an alternative
	 * {@link Comparator}.
	 */
	class UsageDataTableViewerColumn {
		private TableViewerColumn column;
		private UsageDataColumnProvider usageDataColumnProvider;
		/**
		 * The default comparator knows how to compare objects based on the
		 * string value returned for each instance by the
		 * {@link UsageDataColumnProvider#getText(UsageDataEventWrapper)}
		 * method.
		 */
		private Comparator<UsageDataEventWrapper> comparator = new Comparator<UsageDataEventWrapper>() {
			public int compare(UsageDataEventWrapper event1,
					UsageDataEventWrapper event2) {
				if (usageDataColumnProvider == null)
					return 0;
				String text1 = usageDataColumnProvider.getText(event1);
				String text2 = usageDataColumnProvider.getText(event2);

				if (text1 == null && text2 == null)
					return 0;
				if (text1 == null)
					return -1;
				if (text2 == null)
					return 1;

				return text1.compareTo(text2);
			}
		};

		private ViewerSorter sorter = new ViewerSorter() {
			@Override
			public int compare(Viewer viewer, Object object1, Object object2) {
				return comparator.compare((UsageDataEventWrapper) object1,
						(UsageDataEventWrapper) object2);
			}
		};

		private SelectionListener selectionListener = new SelectionAdapter() {
			/**
			 * When the column is selected (clicked on by the the user, sort the
			 * table based on the value presented in that column.
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSortColumn();
			}
		};

		public UsageDataTableViewerColumn(int style) {
			column = new TableViewerColumn(viewer, style);
			initialize();
		}

		public void setSortColumn() {
			getTable().setSortColumn(getColumn());
			getTable().setSortDirection(SWT.DOWN);
			viewer.setSorter(sorter);
		}

		private void initialize() {
			getColumn().addSelectionListener(selectionListener);
			getColumn().setWidth(25);
		}

		TableColumn getColumn() {
			return column.getColumn();
		}

		Table getTable() {
			return viewer.getTable();
		}

		public void setSorter(Comparator<UsageDataEventWrapper> comparator) {
			// TODO May need to handle the case when the active comparator is
			// changed.
			this.comparator = comparator;
		}

		public void resize(GC gc, Object[] objects) {
			int width = usageDataColumnProvider.getMaximumWidth(gc, objects) + 20;
			width = Math.max(getColumn().getWidth(), width);
			getColumn().setWidth(width);
		}

		public void setLabelProvider(
				UsageDataColumnProvider usageDataColumnProvider) {
			this.usageDataColumnProvider = usageDataColumnProvider;
			column.setLabelProvider(usageDataColumnProvider);
		}

		public void setWidth(int width) {
			getColumn().setWidth(width);
		}

		public void setText(String text) {
			getColumn().setText(text);
		}
	}

	/**
	 * The {@link UsageDataColumnProvider} is a column label provider that
	 * includes some convenience methods.
	 */
	abstract class UsageDataColumnProvider extends ColumnLabelProvider {
		/**
		 * This convenience method is used to determine an appropriate width for
		 * the column based on the collection of event objects. The returned
		 * value is the maximum width (in pixels) of the text the receiver
		 * associates with each of the events. The events are provided as
		 * Object[] because converting them to {@link UsageDataEventWrapper}[]
		 * would be an unnecessary expense.
		 * 
		 * @param gc
		 *            a {@link GC} loaded with the font used to display the
		 *            events.
		 * @param events
		 *            an array of {@link UsageDataEventWrapper} instances.
		 * @return the width of the widest event
		 */
		public int getMaximumWidth(GC gc, Object[] events) {
			int width = 0;
			for (Object event : events) {
				Point extent = gc.textExtent(getText(event));
				int x = extent.x;
				Image image = getImage(event);
				if (image != null)
					x += image.getBounds().width;
				if (x > width)
					width = x;
			}
			return width;
		}

		/**
		 * This method provides a foreground colour for the cell. The cell will
		 * be black if the filter includes the includes the provided
		 * {@link UsageDataEvent}, or gray if the filter excludes it.
		 */
		@Override
		public Color getForeground(Object element) {
			return colorBlack;
		}

		@Override
		public String getText(Object element) {
			return getText((UsageDataEventWrapper) element);
		}

		@Override
		public Image getImage(Object element) {
			return getImage((UsageDataEventWrapper) element);
		}

		public String getText(UsageDataEventWrapper element) {
			return ""; //$NON-NLS-1$
		}

		public Image getImage(UsageDataEventWrapper element) {
			return null;
		}
	}
}
