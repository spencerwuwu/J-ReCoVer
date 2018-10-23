// https://searchcode.com/api/result/71372517/

package context.apps.demos.helloroom;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import context.arch.discoverer.Discoverer;
import context.arch.enactor.Enactor;
import context.arch.intelligibility.Explanation;
import context.arch.intelligibility.presenters.QueryPanel;
import context.arch.intelligibility.presenters.StringPresenter;
import context.arch.intelligibility.query.Query;
import context.arch.intelligibility.query.QueryListener;
import context.arch.intelligibility.reducers.ConjunctionReducer;
import context.arch.intelligibility.reducers.FilteredCReducer;

/**
 * Intelligible version of the Hello Room tutorial application.
 * It provides a GUI to allow the user to ask several types of questions, and
 * displays generated explanations in a text area below.
 * @author Brian Y. Lim
 *
 */
public class HelloRoomIntelligible extends HelloRoom {
	
	/** Intelligibility UI */
	protected JPanel iui;
	
	public HelloRoomIntelligible() {
		super();
		iui =  new IntelligibleUI(enactor);
	}
	
	/**
	 * Panel for displaying intelligibility query UI and explanations.
	 * @author Brian Y. Lim
	 *
	 */
	public class IntelligibleUI extends JPanel {

		private static final long serialVersionUID = -1419171329700935534L;
		
		private QueryPanel queryPanel;
		private ConjunctionReducer creducer;
		private StringPresenter presenter;
		
		private JTextArea explanationArea;

		public IntelligibleUI(final Enactor enactor) {
			super();
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createTitledBorder("Explanations"));
			
			// reducer for showing only brightness and presence in explanations
			creducer = new FilteredCReducer("brightness", "presence", "light");
			
			// presenter for rendering explanations
			presenter = new StringPresenter(enactor);
			
			// UI for obtaining queries from the user
			queryPanel = new QueryPanel(enactor, creducer, true);
			add(queryPanel, BorderLayout.NORTH);
			
			// UI for showing explanation
			explanationArea = new JTextArea();
			add(explanationArea, BorderLayout.CENTER);

			// query listener for responding to queries
			queryPanel.addQueryListener(new QueryListener() {
				@Override
				public void queryInvoked(Query query) {
					// generate explanation
					Explanation explanation = enactor.getExplainer().getExplanation(query);
					System.out.println("explanation = " + explanation);
					
					// reduce
					explanation = creducer.apply(explanation);
					
					// render
					String explanationText = presenter.render(explanation);
					
					explanationArea.setText(explanationText);
				}
			});
		}
		
	}
	
	public static void main(String[] args) {
		Discoverer.start();
		
		HelloRoomIntelligible app = new HelloRoomIntelligible();
		
		/*
		 * start GUI
		 */
		JFrame frame = new JFrame("Hello Room Intelligible");
		frame.add(app.ui, BorderLayout.NORTH);
		frame.add(app.iui, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(320, 360));
		frame.setLocationRelativeTo(null); // center of screen
		frame.setVisible(true);
	}

}

