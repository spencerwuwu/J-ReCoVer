// https://searchcode.com/api/result/13955428/

package GM.diagram.edit.policies;

import java.util.Iterator;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.gef.commands.Command;
import org.eclipse.gmf.runtime.common.core.command.ICompositeCommand;
import org.eclipse.gmf.runtime.diagram.core.commands.DeleteCommand;
import org.eclipse.gmf.runtime.emf.commands.core.command.CompositeTransactionalCommand;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyElementCommand;
import org.eclipse.gmf.runtime.emf.type.core.commands.DestroyReferenceCommand;
import org.eclipse.gmf.runtime.emf.type.core.requests.CreateRelationshipRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyElementRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.DestroyReferenceRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.ReorientReferenceRelationshipRequest;
import org.eclipse.gmf.runtime.emf.type.core.requests.ReorientRelationshipRequest;
import org.eclipse.gmf.runtime.notation.Edge;
import org.eclipse.gmf.runtime.notation.Node;
import org.eclipse.gmf.runtime.notation.View;

import GM.diagram.edit.commands.AdGoalHelpCreateCommand;
import GM.diagram.edit.commands.AdGoalHelpReorientCommand;
import GM.diagram.edit.commands.ContributionLinkCreateCommand;
import GM.diagram.edit.commands.ContributionLinkReorientCommand;
import GM.diagram.edit.commands.GoalOperatCreateCommand;
import GM.diagram.edit.commands.GoalOperatReorientCommand;
import GM.diagram.edit.commands.GoalRefCreateCommand;
import GM.diagram.edit.commands.GoalRefReorientCommand;
import GM.diagram.edit.commands.RefinementLinkTargetCreateCommand;
import GM.diagram.edit.commands.RefinementLinkTargetReorientCommand;
import GM.diagram.edit.parts.AdGoalHelpEditPart;
import GM.diagram.edit.parts.ContributionLinkEditPart;
import GM.diagram.edit.parts.DefinitionEditPart;
import GM.diagram.edit.parts.GoalLtlCompartmentEditPart;
import GM.diagram.edit.parts.GoalOperatEditPart;
import GM.diagram.edit.parts.GoalRefEditPart;
import GM.diagram.edit.parts.RefinementLinkTargetEditPart;
import GM.diagram.part.GMVisualIDRegistry;
import GM.diagram.providers.GMElementTypes;

/**
 * @generated
 */
public class GoalItemSemanticEditPolicy extends GMBaseItemSemanticEditPolicy {

	/**
	 * @generated
	 */
	public GoalItemSemanticEditPolicy() {
		super(GMElementTypes.Goal_2001);
	}

	/**
	 * @generated
	 */
	protected Command getDestroyElementCommand(DestroyElementRequest req) {
		View view = (View) getHost().getModel();
		CompositeTransactionalCommand cmd = new CompositeTransactionalCommand(
				getEditingDomain(), null);
		cmd.setTransactionNestingEnabled(false);
		for (Iterator<?> it = view.getTargetEdges().iterator(); it.hasNext();) {
			Edge incomingLink = (Edge) it.next();
			if (GMVisualIDRegistry.getVisualID(incomingLink) == AdGoalHelpEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						incomingLink.getSource().getElement(), null,
						incomingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
			if (GMVisualIDRegistry.getVisualID(incomingLink) == RefinementLinkTargetEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						incomingLink.getSource().getElement(), null,
						incomingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
			if (GMVisualIDRegistry.getVisualID(incomingLink) == ContributionLinkEditPart.VISUAL_ID) {
				DestroyElementRequest r = new DestroyElementRequest(
						incomingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), incomingLink));
				continue;
			}
		}
		for (Iterator<?> it = view.getSourceEdges().iterator(); it.hasNext();) {
			Edge outgoingLink = (Edge) it.next();
			if (GMVisualIDRegistry.getVisualID(outgoingLink) == GoalRefEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						outgoingLink.getSource().getElement(), null,
						outgoingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), outgoingLink));
				continue;
			}
			if (GMVisualIDRegistry.getVisualID(outgoingLink) == GoalOperatEditPart.VISUAL_ID) {
				DestroyReferenceRequest r = new DestroyReferenceRequest(
						outgoingLink.getSource().getElement(), null,
						outgoingLink.getTarget().getElement(), false);
				cmd.add(new DestroyReferenceCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), outgoingLink));
				continue;
			}
			if (GMVisualIDRegistry.getVisualID(outgoingLink) == ContributionLinkEditPart.VISUAL_ID) {
				DestroyElementRequest r = new DestroyElementRequest(
						outgoingLink.getElement(), false);
				cmd.add(new DestroyElementCommand(r));
				cmd.add(new DeleteCommand(getEditingDomain(), outgoingLink));
				continue;
			}
		}
		EAnnotation annotation = view.getEAnnotation("Shortcut"); //$NON-NLS-1$
		if (annotation == null) {
			// there are indirectly referenced children, need extra commands: false
			addDestroyChildNodesCommand(cmd);
			addDestroyShortcutsCommand(cmd, view);
			// delete host element
			cmd.add(new DestroyElementCommand(req));
		} else {
			cmd.add(new DeleteCommand(getEditingDomain(), view));
		}
		return getGEFWrapper(cmd.reduce());
	}

	/**
	 * @generated
	 */
	private void addDestroyChildNodesCommand(ICompositeCommand cmd) {
		View view = (View) getHost().getModel();
		for (Iterator<?> nit = view.getChildren().iterator(); nit.hasNext();) {
			Node node = (Node) nit.next();
			switch (GMVisualIDRegistry.getVisualID(node)) {
			case GoalLtlCompartmentEditPart.VISUAL_ID:
				for (Iterator<?> cit = node.getChildren().iterator(); cit
						.hasNext();) {
					Node cnode = (Node) cit.next();
					switch (GMVisualIDRegistry.getVisualID(cnode)) {
					case DefinitionEditPart.VISUAL_ID:
						cmd.add(new DestroyElementCommand(
								new DestroyElementRequest(getEditingDomain(),
										cnode.getElement(), false))); // directlyOwned: true
						// don't need explicit deletion of cnode as parent's view deletion would clean child views as well 
						// cmd.add(new org.eclipse.gmf.runtime.diagram.core.commands.DeleteCommand(getEditingDomain(), cnode));
						break;
					}
				}
				break;
			}
		}
	}

	/**
	 * @generated
	 */
	protected Command getCreateRelationshipCommand(CreateRelationshipRequest req) {
		Command command = req.getTarget() == null ? getStartCreateRelationshipCommand(req)
				: getCompleteCreateRelationshipCommand(req);
		return command != null ? command : super
				.getCreateRelationshipCommand(req);
	}

	/**
	 * @generated
	 */
	protected Command getStartCreateRelationshipCommand(
			CreateRelationshipRequest req) {
		if (GMElementTypes.AdGoalHelp_4001 == req.getElementType()) {
			return null;
		}
		if (GMElementTypes.GoalRef_4003 == req.getElementType()) {
			return getGEFWrapper(new GoalRefCreateCommand(req, req.getSource(),
					req.getTarget()));
		}
		if (GMElementTypes.RefinementLinkTarget_4004 == req.getElementType()) {
			return null;
		}
		if (GMElementTypes.GoalOperat_4005 == req.getElementType()) {
			return getGEFWrapper(new GoalOperatCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		if (GMElementTypes.ContributionLink_4007 == req.getElementType()) {
			return getGEFWrapper(new ContributionLinkCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		return null;
	}

	/**
	 * @generated
	 */
	protected Command getCompleteCreateRelationshipCommand(
			CreateRelationshipRequest req) {
		if (GMElementTypes.AdGoalHelp_4001 == req.getElementType()) {
			return getGEFWrapper(new AdGoalHelpCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		if (GMElementTypes.GoalRef_4003 == req.getElementType()) {
			return null;
		}
		if (GMElementTypes.RefinementLinkTarget_4004 == req.getElementType()) {
			return getGEFWrapper(new RefinementLinkTargetCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		if (GMElementTypes.GoalOperat_4005 == req.getElementType()) {
			return null;
		}
		if (GMElementTypes.ContributionLink_4007 == req.getElementType()) {
			return getGEFWrapper(new ContributionLinkCreateCommand(req,
					req.getSource(), req.getTarget()));
		}
		return null;
	}

	/**
	 * Returns command to reorient EClass based link. New link target or source
	 * should be the domain model element associated with this node.
	 * 
	 * @generated
	 */
	protected Command getReorientRelationshipCommand(
			ReorientRelationshipRequest req) {
		switch (getVisualID(req)) {
		case ContributionLinkEditPart.VISUAL_ID:
			return getGEFWrapper(new ContributionLinkReorientCommand(req));
		}
		return super.getReorientRelationshipCommand(req);
	}

	/**
	 * Returns command to reorient EReference based link. New link target or source
	 * should be the domain model element associated with this node.
	 * 
	 * @generated
	 */
	protected Command getReorientReferenceRelationshipCommand(
			ReorientReferenceRelationshipRequest req) {
		switch (getVisualID(req)) {
		case AdGoalHelpEditPart.VISUAL_ID:
			return getGEFWrapper(new AdGoalHelpReorientCommand(req));
		case GoalRefEditPart.VISUAL_ID:
			return getGEFWrapper(new GoalRefReorientCommand(req));
		case RefinementLinkTargetEditPart.VISUAL_ID:
			return getGEFWrapper(new RefinementLinkTargetReorientCommand(req));
		case GoalOperatEditPart.VISUAL_ID:
			return getGEFWrapper(new GoalOperatReorientCommand(req));
		}
		return super.getReorientReferenceRelationshipCommand(req);
	}

}

