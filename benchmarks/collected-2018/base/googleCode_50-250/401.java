// https://searchcode.com/api/result/13353842/

/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.l2emuproject.gameserver.network.clientpackets;

import net.l2emuproject.gameserver.items.L2ItemInstance;
import net.l2emuproject.gameserver.network.SystemMessageId;
import net.l2emuproject.gameserver.network.serverpackets.ExVariationCancelResult;
import net.l2emuproject.gameserver.network.serverpackets.InventoryUpdate;
import net.l2emuproject.gameserver.network.serverpackets.SystemMessage;
import net.l2emuproject.gameserver.templates.item.L2Item;
import net.l2emuproject.gameserver.world.object.L2Player;

/**
 * Format(ch) d
 * @author  -Wooden-
 */
public final class RequestRefineCancel extends L2GameClientPacket
{
	private static final String	_C__D0_2E_REQUESTREFINECANCEL	= "[C] D0:2E RequestRefineCancel";
	private int					_targetItemObjId;

	@Override
	protected void readImpl()
	{
		_targetItemObjId = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		L2ItemInstance targetItem = activeChar.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			requestFailed(new ExVariationCancelResult(0));
			return;
		}

		// cannot remove augmentation from a not augmented item
		if (!targetItem.isAugmented())
		{
			sendPacket(new ExVariationCancelResult(0));
			requestFailed(SystemMessageId.AUGMENTATION_REMOVAL_CAN_ONLY_BE_DONE_ON_AN_AUGMENTED_ITEM);
			return;
		}

		// get the price
		int price = 0;
		switch (targetItem.getItem().getCrystalType())
		{
		case L2Item.CRYSTAL_C:
			if (targetItem.getCrystalCount() < 1720)
				price = 95000;
			else if (targetItem.getCrystalCount() < 2452)
				price = 150000;
			else
				price = 210000;
			break;
		case L2Item.CRYSTAL_B:
			if (targetItem.getCrystalCount() < 1746)
				price = 240000;
			else
				price = 270000;
			break;
		case L2Item.CRYSTAL_A:
			if (targetItem.getCrystalCount() < 2160)
				price = 330000;
			else if (targetItem.getCrystalCount() < 2824)
				price = 390000;
			else
				price = 420000;
			break;
		case L2Item.CRYSTAL_S:
			price = 480000;
			break;
		case L2Item.CRYSTAL_S80:
		case L2Item.CRYSTAL_S84:
			price = 920000;
			break;
			// any other item type is not augmentable
		default:
			requestFailed(new ExVariationCancelResult(0));
			return;
		}

		// try to reduce the players adena
		if (!activeChar.reduceAdena("RequestRefineCancel", price, null, true))
		{
			activeChar.sendPacket(new ExVariationCancelResult(0));
			activeChar.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
			return;
		}

		// unequip item
		if (targetItem.isEquipped())
			activeChar.disarmWeapons(false);

		// remove the augmentation
		targetItem.removeAugmentation();

		// send ExVariationCancelResult
		sendPacket(new ExVariationCancelResult(1));

		// send inventory update
		InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(targetItem);
		sendPacket(iu);

		// send system message
		SystemMessage sm = new SystemMessage(SystemMessageId.AUGMENTATION_HAS_BEEN_SUCCESSFULLY_REMOVED_FROM_YOUR_S1);
		sm.addString(targetItem.getItemName());
		sendPacket(sm);

		sendAF();
	}

	@Override
	public String getType()
	{
		return _C__D0_2E_REQUESTREFINECANCEL;
	}
}

