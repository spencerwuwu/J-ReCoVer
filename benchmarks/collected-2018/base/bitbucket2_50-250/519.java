// https://searchcode.com/api/result/121331931/

package com.wharzed.pc;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.wharzed.input.IInput;
import com.wharzed.output.IOutput;
import com.wharzed.output.display.RenderableType;
import com.wharzed.pc.hud.IHudExtension;
import com.wharzed.pc.output.display.GameFrame;
import com.wharzed.pc.output.display.GamePanel;
import com.wharzed.pc.output.display.IGamePanel;
import com.wharzed.pc.renderer.BoundsRenderer;
import com.wharzed.pc.renderer.IRenderer;
import com.wharzed.pc.renderer.SVGRenderer;

/**
 * @author Chris Walter
 * 
 * This is the main configuration class of the application. Implementations are bound to interfaces here. Additionally 
 * any user supplied configuration is bound here as well, typically through annotations. If it is necessary to bind 
 * configurations via files, that should be done here as well.
 * 
 * No global changes should be made anywhere else in the application. This is necessary to avoid conflicts and reduce 
 * the complexity of troubleshooting.
 */
public class PCModule extends AbstractModule {

	/** {@inheritDoc} */
	@Override
	protected final void configure() {
		userDefined();
		
		//Add graphics
		bind(IGamePanel.class).to(GamePanel.class);
		bind(IOutput.class).to(GameFrame.class);
		
		//Bind renderers
		MapBinder<RenderableType, IRenderer> rendererBinder = MapBinder.newMapBinder(
				binder(), RenderableType.class, IRenderer.class);
		rendererBinder.addBinding(RenderableType.BOUNDS).to(BoundsRenderer.class).in(Singleton.class);
		rendererBinder.addBinding(RenderableType.SVG).to(SVGRenderer.class).in(Singleton.class);

		bind(Color.class).annotatedWith(Names.named("Color.NotFound")).toInstance(Color.PINK);

		/**
		 * I am assuming that a linked list implementation should be adequate for tracking input  and output handles. 
		 * This is because they should rarely change (i.e. only when the user visits a configuration or settings menu)
		 *  and should only be iterated through.
		 */
		
		//Add inputs
		bind(new TypeLiteral<Collection<IInput>>() { }).toInstance(new LinkedList<IInput>());
		
		//Add outputs
		bind(new TypeLiteral<Collection<IOutput>>() { }).toInstance(new LinkedList<IOutput>());
		
		bind(new TypeLiteral<Collection<IHudExtension>>() { }).toInstance(new LinkedList<IHudExtension>());
	}
	
	/**
	 * Configuration options which should be specified by the end user. In a future iteration we should move these 
	 * options to a configuration file to be read in by the PCModule.
	 * 
	 * Keep in mind that these options should have sane defaults to prevent issues in the default configurations 
	 * and allow for a 'safe' mode.
	 */
	private void userDefined() {
		//We are only turning checkstyle off here to avoid problems with the magic numbers used for configuration.
		//This should not be a common occurrence and eventually removed when configuration is loaded via file.
		//CHECKSTYLE:OFF
		//Display options
		bindConstant().annotatedWith(Names.named("display.width")).to(800);
		bindConstant().annotatedWith(Names.named("display.height")).to(600);
		bindConstant().annotatedWith(Names.named("display.showFPS")).to(true);
		
		//Game options
		bindConstant().annotatedWith(Names.named("game.fps")).to(60);
		bindConstant().annotatedWith(Names.named("system.threads")).to(Runtime.getRuntime().availableProcessors());
		//CHECKSTYLE:ON
	}
}

