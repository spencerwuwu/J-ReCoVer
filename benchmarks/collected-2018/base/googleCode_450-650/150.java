// https://searchcode.com/api/result/13354045/

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

import java.util.ArrayList;

import net.l2emuproject.Config;
import net.l2emuproject.gameserver.datatables.ItemTable;
import net.l2emuproject.gameserver.entity.itemcontainer.PcInventory;
import net.l2emuproject.gameserver.items.L2ItemInstance;
import net.l2emuproject.gameserver.network.SystemMessageId;
import net.l2emuproject.gameserver.network.serverpackets.ItemList;
import net.l2emuproject.gameserver.network.serverpackets.StatusUpdate;
import net.l2emuproject.gameserver.network.serverpackets.SystemMessage;
import net.l2emuproject.gameserver.network.serverpackets.UserInfo;
import net.l2emuproject.gameserver.services.attribute.Attributes;
import net.l2emuproject.gameserver.services.augmentation.L2Augmentation;
import net.l2emuproject.gameserver.services.transactions.L2Multisell;
import net.l2emuproject.gameserver.services.transactions.L2Multisell.MultiSellEntry;
import net.l2emuproject.gameserver.services.transactions.L2Multisell.MultiSellIngredient;
import net.l2emuproject.gameserver.services.transactions.L2Multisell.MultiSellListContainer;
import net.l2emuproject.gameserver.system.util.FloodProtector.Protected;
import net.l2emuproject.gameserver.templates.item.L2Armor;
import net.l2emuproject.gameserver.templates.item.L2Item;
import net.l2emuproject.gameserver.templates.item.L2Weapon;
import net.l2emuproject.gameserver.world.object.L2Npc;
import net.l2emuproject.gameserver.world.object.L2Player;

public final class MultiSellChoose extends L2GameClientPacket
{
	private static final String _C__MULTISELLCHOOSE = "[C] B0 MultiSellChoose c[ddqhddhhhhhhhh]";
	
	private int _listId;
	private int _entryId;
	private long _amount;
	private int _enchantment;
	private long _transactionTax; // local handling of taxation
	
	@Override
	protected void readImpl()
	{
		_listId = readD();
		_entryId = readD();
		_amount = readQ();
		readH();
		readD();
		readD();
		readH(); // elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		readH();// elemental attributes
		_enchantment = _entryId % 100000;
		_entryId = _entryId / 100000;
		_transactionTax = 0; // initialize tax amount to 0...
	}
	
	@Override
	protected void runImpl()
	{
		if (_amount < 1 || _amount > 5000)
			return;
		
		L2Player player = getActiveChar();
		if (player == null)
			return;
		
		// Flood protect Multisell
		if (!getClient().getFloodProtector().tryPerformAction(Protected.MULTISELL))
			return;
		
		MultiSellListContainer list = L2Multisell.getInstance().getList(_listId);
		if (list != null)
		{
			L2Npc target = player.getTarget(L2Npc.class);
			if (!player.isGM()
				&& (target == null || !list.checkNpcId(target.getNpcId()) || !target.canInteract(player)))
				return;
			
			for (MultiSellEntry entry : list.getEntries())
			{
				if (entry.getEntryId() == _entryId)
				{
					doExchange(player, entry, list.getApplyTaxes(), list.getMaintainEnchantment(), _enchantment);
					break;
				}
			}
		}
		
		//will always be sent
		sendAF();
	}
	
	private void doExchange(L2Player player, MultiSellEntry templateEntry, boolean applyTaxes,
		boolean maintainEnchantment, int enchantment)
	{
		PcInventory inv = player.getInventory();
		
		// given the template entry and information about maintaining enchantment and applying taxes
		// re-create the instance of the entry that will be used for this exchange
		// i.e. change the enchantment level of select ingredient/products and adena amount appropriately.
		final L2Npc merchant = player.getTarget(L2Npc.class);
		if (merchant == null)
			return;
		
		MultiSellEntry entry = prepareEntry(merchant, templateEntry, applyTaxes, maintainEnchantment, enchantment);
		
		int slots = 0;
		int weight = 0;
		for (MultiSellIngredient e : entry.getProducts())
		{
			final L2Item template = ItemTable.getInstance().getTemplate(e.getItemId());
			if (template == null)
				continue;
			
			if (!template.isStackable())
				slots += e.getItemCount() * _amount;
			else if (player.getInventory().getItemByItemId(e.getItemId()) == null)
				slots++;
			
			weight += e.getItemCount() * _amount * template.getWeight();
		}
		
		if (!inv.validateWeight(weight))
		{
			sendPacket(SystemMessageId.WEIGHT_LIMIT_EXCEEDED);
			return;
		}
		
		if (!inv.validateCapacity(slots))
		{
			sendPacket(SystemMessageId.SLOTS_FULL);
			return;
		}
		
		// Generate a list of distinct ingredients and counts in order to check if the correct item-counts
		// are possessed by the player
		ArrayList<MultiSellIngredient> _ingredientsList = new ArrayList<MultiSellIngredient>();
		boolean newIng = true;
		for (MultiSellIngredient e : entry.getIngredients())
		{
			newIng = true;
			
			// at this point, the template has already been modified so that enchantments are properly included
			// whenever they need to be applied.  Uniqueness of items is thus judged by item id AND enchantment level
			for (MultiSellIngredient ex : _ingredientsList)
			{
				// if the item was already added in the list, merely increment the count
				// this happens if 1 list entry has the same ingredient twice (example 2 swords = 1 dual)
				if ((ex.getItemId() == e.getItemId()) && (ex.getEnchantmentLevel() == e.getEnchantmentLevel()))
				{
					if (ex.getItemCount() + e.getItemCount() >= Integer.MAX_VALUE)
					{
						sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					ex.setItemCount(ex.getItemCount() + e.getItemCount());
					newIng = false;
				}
			}
			if (newIng)
			{
				// if it's a new ingredient, just store its info directly (item id, count, enchantment)
				_ingredientsList.add(new MultiSellIngredient(e));
			}
		}
		// now check if the player has sufficient items in the inventory to cover the ingredients' expences
		for (MultiSellIngredient e : _ingredientsList)
		{
			if (e.getItemCount() * _amount >= Integer.MAX_VALUE)
			{
				sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_QUANTITY_THAT_CAN_BE_INPUTTED);
				_ingredientsList.clear();
				_ingredientsList = null;
				return;
			}
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					if (player.getClan() == null)
					{
						sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER);
						return;
					}
					else if (!player.isClanLeader())
					{
						sendPacket(SystemMessageId.ONLY_THE_CLAN_LEADER_IS_ENABLED);
						return;
					}
					else if (player.getClan().getReputationScore() < e.getItemCount() * _amount)
					{
						sendPacket(SystemMessageId.CLAN_REPUTATION_SCORE_IS_TOO_LOW);
						return;
					}
					break;
				}
				case -300: // Player Fame
				{
					if (player.getFame() < e.getItemCount() * _amount)
					{
						sendPacket(SystemMessageId.NOT_ENOUGH_FAME_POINTS);
						return;
					}
					break;
				}
				default:
				{
					// if this is not a list that maintains enchantment, check the count of all items that have the given id.
					// otherwise, check only the count of items with exactly the needed enchantment level
					if (inv.getInventoryItemCount(e.getItemId(), maintainEnchantment ? e.getEnchantmentLevel() : -1, false) <
							((Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient()) ? (e.getItemCount() * _amount) : e.getItemCount()))
					{
						sendPacket(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS);
						_ingredientsList.clear();
						_ingredientsList = null;
						return;
					}
					
					//TODO: review
					if (ItemTable.getInstance().getTemplate(e.getItemId()).isStackable())
						_enchantment = 0;
					
					break;
				}
			}
		}
		
		_ingredientsList.clear();
		_ingredientsList = null;
		ArrayList<L2Augmentation> augmentation = new ArrayList<L2Augmentation>();
		Attributes[] elemental = null;
		/** All ok, remove items and add final product */
		
		for (MultiSellIngredient e : entry.getIngredients())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score
				{
					int repLeft = (int)(player.getClan().getReputationScore() - (e.getItemCount() * _amount));
					player.getClan().setReputationScore(repLeft, true);
					sendPacket(new SystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber((int)(e
						.getItemCount() * _amount)));
					break;
				}
				case -300: // Player Fame
				{
					int fameLeft = (int)(player.getFame() - (e.getItemCount() * _amount));
					player.setFame(fameLeft);
					sendPacket(new UserInfo(player));
					break;
				}
				default:
				{
					L2ItemInstance itemToTake = inv.getItemByItemId(e.getItemId()); // initialize and initial guess for the item to take.
					if (itemToTake == null)
					{ //this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
						_log.fatal("Character: " + player.getName() + " is trying to cheat in multisell, merchant id:"
							+ merchant.getNpcId());
						requestFailed(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS);
						return;
					}
					
					if (itemToTake.isEquipped())
					{ //this is a cheat, transaction will be aborted and if any items already taken will not be returned back to inventory!
						_log.fatal("Character: " + player.getName() + " is trying to cheat in multisell, exchanging equipped item, merchatnt id:" + merchant.getNpcId());
						requestFailed(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS);
						return;
					}
					
					if (itemToTake.isWear())
					{//Player trying to buy something from the Multisell store with an item that's just being used from the Wear option from merchants.
						_log.fatal("Character: " + player.getName() + " is trying to cheat in multisell, merchant id:"
							+ merchant.getNpcId());
						requestFailed(SystemMessageId.NOT_ENOUGH_REQUIRED_ITEMS);
						return;
					}
					
					if (Config.ALT_BLACKSMITH_USE_RECIPES || !e.getMaintainIngredient())
					{
						// if it's a stackable item, just reduce the amount from the first (only) instance that is found in the inventory
						if (itemToTake.isStackable())
						{
							if (!player.destroyItem("Multisell", itemToTake.getObjectId(),
								(e.getItemCount() * _amount), player.getTarget(), true))
								return;
						}
						else
						{
							// for non-stackable items, one of two scenaria are possible:
							// a) list maintains enchantment: get the instances that exactly match the requested enchantment level
							// b) list does not maintain enchantment: get the instances with the LOWEST enchantment level
							
							// a) if enchantment is maintained, then get a list of items that exactly match this enchantment
							if (maintainEnchantment)
							{
								// loop through this list and remove (one by one) each item until the required amount is taken.
								L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId(), e
									.getEnchantmentLevel());
								for (int i = 0; i < (e.getItemCount() * _amount); i++)
								{
									if (inventoryContents[i].isAugmented())
										augmentation.add(inventoryContents[i].getAugmentation());
									if (inventoryContents[i].getElementals() != null)
										elemental = inventoryContents[i].getElementals();
									if (!player.destroyItem("Multisell", inventoryContents[i].getObjectId(), 1, player
										.getTarget(), true))
										return;
								}
							}
							else
							// b) enchantment is not maintained.  Get the instances with the LOWEST enchantment level
							{
								/* NOTE: There are 2 ways to achieve the above goal.
								 * 1) Get all items that have the correct itemId, loop through them until the lowest enchantment
								 * 		level is found.  Repeat all this for the next item until proper count of items is reached.
								 * 2) Get all items that have the correct itemId, sort them once based on enchantment level,
								 * 		and get the range of items that is necessary.
								 * Method 1 is faster for a small number of items to be exchanged.
								 * Method 2 is faster for large amounts.
								 *
								 * EXPLANATION:
								 *   Worst case scenario for algorithm 1 will make it run in a number of cycles given by:
								 * m*(2n-m+1)/2 where m is the number of items to be exchanged and n is the total
								 * number of inventory items that have a matching id.
								 *   With algorithm 2 (sort), sorting takes n*log(n) time and the choice is done in a single cycle
								 * for case b (just grab the m first items) or in linear time for case a (find the beginning of items
								 * with correct enchantment, index x, and take all items from x to x+m).
								 * Basically, whenever m > log(n) we have: m*(2n-m+1)/2 = (2nm-m*m+m)/2 >
								 * (2nlogn-logn*logn+logn)/2 = nlog(n) - log(n*n) + log(n) = nlog(n) + log(n/n*n) =
								 * nlog(n) + log(1/n) = nlog(n) - log(n) = (n-1)log(n)
								 * So for m < log(n) then m*(2n-m+1)/2 > (n-1)log(n) and m*(2n-m+1)/2 > nlog(n)
								 *
								 * IDEALLY:
								 * In order to best optimize the performance, choose which algorithm to run, based on whether 2^m > n
								 * if ( (2<<(e.getItemCount() * _amount)) < inventoryContents.length )
								 *   // do Algorithm 1, no sorting
								 * else
								 *   // do Algorithm 2, sorting
								 *
								 * CURRENT IMPLEMENTATION:
								 * In general, it is going to be very rare for a person to do a massive exchange of non-stackable items
								 * For this reason, we assume that algorithm 1 will always suffice and we keep things simple.
								 * If, in the future, it becomes necessary that we optimize, the above discussion should make it clear
								 * what optimization exactly is necessary (based on the comments under "IDEALLY").
								 */

								// choice 1.  Small number of items exchanged.  No sorting.
								for (int i = 1; i <= (e.getItemCount() * _amount); i++)
								{
									L2ItemInstance[] inventoryContents = inv.getAllItemsByItemId(e.getItemId());
									
									itemToTake = inventoryContents[0];
									// get item with the LOWEST enchantment level  from the inventory...
									// +0 is lowest by default...
									if (itemToTake.getEnchantLevel() > 0)
									{
										for (L2ItemInstance item : inventoryContents)
										{
											if (item.getEnchantLevel() < itemToTake.getEnchantLevel())
											{
												itemToTake = item;
												// nothing will have enchantment less than 0. If a zero-enchanted
												// item is found, just take it
												if (itemToTake.getEnchantLevel() == 0)
													break;
											}
										}
									}
									if (!player.destroyItem("Multisell", itemToTake.getObjectId(), 1, player
										.getTarget(), true))
										return;
								}
							}
						}
					}
					break;
				}
			}
		}
		
		// Generate the appropriate items
		for (MultiSellIngredient e : entry.getProducts())
		{
			switch (e.getItemId())
			{
				case -200: // Clan Reputation Score - now not supported
				{
					//player.getClan().setReputationScore((int)(player.getClan().getReputationScore() + e.getItemCount() * _amount), true);
					break;
				}
				case -300: // Player Fame
				{
					player.setFame((int)(player.getFame() + e.getItemCount() * _amount));
					sendPacket(new UserInfo(player));
					break;
				}
				default:
				{
					if (ItemTable.getInstance().getTemplate(e.getItemId()).isStackable())
					{
						inv.addItem("Multisell", e.getItemId(), (e.getItemCount() * _amount), player, player
							.getTarget());
					}
					else
					{
						L2ItemInstance product = null;
						for (int i = 0; i < (e.getItemCount() * _amount); i++)
						{
							product = inv.addItem("Multisell", e.getItemId(), 1, player, player.getTarget());
							if (maintainEnchantment)
							{
								if (i < augmentation.size())
									product.setAugmentation(new L2Augmentation(augmentation.get(i).getAugmentationId(),
										augmentation.get(i).getSkill()));
								if (elemental != null)
									for (Attributes attr : elemental)
										product.setElementAttr(attr.getElement(), attr.getValue());
								product.setEnchantLevel(e.getEnchantmentLevel());
								product.updateDatabase();
							}
						}
					}
					// msg part
					SystemMessage sm;
					if (e.getItemCount() * _amount > 1)
					{
						sm = new SystemMessage(SystemMessageId.EARNED_S2_S1_S);
						sm.addItemName(e.getItemId());
						sm.addItemNumber(e.getItemCount() * _amount);
						sendPacket(sm);
					}
					else
					{
						if (maintainEnchantment && e.getEnchantmentLevel() > 0)
						{
							sm = new SystemMessage(SystemMessageId.ACQUIRED_S1_S2);
							sm.addNumber(e.getEnchantmentLevel());
							sm.addItemName(e.getItemId());
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.EARNED_S1);
							sm.addItemName(e.getItemId());
						}
						sendPacket(sm);
					}
				}
			}
		}
		sendPacket(new ItemList(player, false));
		
		StatusUpdate su = new StatusUpdate(player.getObjectId());
		su.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		sendPacket(su);
		
		// finally, give the tax to the castle...
		if (merchant.getIsInTown() && merchant.getCastle().getOwnerId() > 0)
			merchant.getCastle().addToTreasury(_transactionTax * _amount);
	}
	
	// Regarding taxation, the following appears to be the case:
	// a) The count of aa remains unchanged (taxes do not affect aa directly).
	// b) 5/6 of the amount of aa is taxed by the normal tax rate.
	// c) the resulting taxes are added as normal adena value.
	// d) normal adena are taxed fully.
	// e) Items other than adena and ancient adena are not taxed even when the list is taxable.
	// example: If the template has an item worth 120aa, and the tax is 10%,
	// then from 120aa, take 5/6 so that is 100aa, apply the 10% tax in adena (10a)
	// so the final price will be 120aa and 10a!
	private MultiSellEntry prepareEntry(L2Npc merchant, MultiSellEntry templateEntry, boolean applyTaxes,
		boolean maintainEnchantment, int enchantLevel)
	{
		MultiSellEntry newEntry = new MultiSellEntry(templateEntry.getEntryId());
		
		long totalAdenaCount = 0;
		boolean hasIngredient = false;
		
		for (MultiSellIngredient ing : templateEntry.getIngredients())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (newIngredient.getItemId() == PcInventory.ADENA_ID && newIngredient.isTaxIngredient())
			{
				double taxRate = 0.0;
				if (applyTaxes)
				{
					if (merchant != null && merchant.getIsInTown())
						taxRate = merchant.getCastle().getTaxRate();
				}
				
				_transactionTax = Math.round(newIngredient.getItemCount() * taxRate);
				totalAdenaCount += _transactionTax;
				continue; // do not yet add this adena amount to the list as non-taxIngredient adena might be entered later (order not guaranteed)
			}
			else if (ing.getItemId() == PcInventory.ADENA_ID) // && !ing.isTaxIngredient()
			{
				totalAdenaCount += newIngredient.getItemCount();
				continue; // do not yet add this adena amount to the list as taxIngredient adena might be entered later (order not guaranteed)
			}
			// if it is an armor/weapon, modify the enchantment level appropriately, if necessary
			// not used for clan reputation and fame
			else if (maintainEnchantment && newIngredient.getItemId() > 0)
			{
				L2Item tempItem = ItemTable.getInstance().getTemplate(newIngredient.getItemId());
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
				{
					newIngredient.setEnchantmentLevel(enchantLevel);
					hasIngredient = true;
				}
			}
			
			// finally, add this ingredient to the entry
			newEntry.addIngredient(newIngredient);
		}
		// Next add the adena amount, if any
		if (totalAdenaCount > 0)
			newEntry.addIngredient(new MultiSellIngredient(PcInventory.ADENA_ID, totalAdenaCount));
		
		// Now modify the enchantment level of products, if necessary
		for (MultiSellIngredient ing : templateEntry.getProducts())
		{
			// load the ingredient from the template
			MultiSellIngredient newIngredient = new MultiSellIngredient(ing);
			
			if (maintainEnchantment && hasIngredient)
			{
				// if it is an armor/weapon, modify the enchantment level appropriately
				// (note, if maintain enchantment is "false" this modification will result to a +0)
				L2Item tempItem = ItemTable.getInstance().getTemplate(newIngredient.getItemId());
				if ((tempItem instanceof L2Armor) || (tempItem instanceof L2Weapon))
					newIngredient.setEnchantmentLevel(enchantLevel);
			}
			newEntry.addProduct(newIngredient);
		}
		return newEntry;
	}
	
	@Override
	public String getType()
	{
		return _C__MULTISELLCHOOSE;
	}
}

